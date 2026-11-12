package com.example.wallet.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.wallet.data.SampleData
import com.example.wallet.ui.AppColors
import com.example.wallet.ui.components.BackButton
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

private val KRASNOYARSK = GeoPoint(56.0153, 92.8932)


private const val USER_AGENT = "MyWalletStudyApp/1.0 (bzduhoff@gmail.com)"

private const val USE_FALLBACK_TILES = true

private val fallbackTiles = XYTileSource(
    "OpenTopoMap", 0, 17, 256, ".png",
    arrayOf(
        "https://a.tile.opentopomap.org/",
        "https://b.tile.opentopomap.org/",
        "https://c.tile.opentopomap.org/"
    ),
    "© OpenStreetMap contributors, SRTM | © OpenTopoMap (CC-BY-SA)"
)

/**
 * Экран 02 «Карта»: настоящая карта OpenStreetMap (osmdroid) с метками пунктов обмена.
 * Нажатие на метку показывает название и описание; внизу те же пункты списком карточек.
 */
@Composable
fun MapScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val points = SampleData.exchangePoints

    // Метки храним, чтобы карточки внизу могли открывать их описание
    val markers = remember { mutableListOf<Marker>() }

    val mapView = remember {
        initOsmdroid(context)
        MapView(context).apply {
            setTileSource(if (USE_FALLBACK_TILES) fallbackTiles else TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.5)
            controller.setCenter(KRASNOYARSK)

            overlays.add(CopyrightOverlay(context)) // «© OpenStreetMap contributors»

            points.forEach { p ->
                val marker = Marker(this).apply {
                    position = GeoPoint(p.lat, p.lon)
                    title = p.name
                    snippet = "${p.address}\n${p.description}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                overlays.add(marker)
                markers.add(marker)
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        BackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.MapLabelBackground)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(text = "Пункты обмена", fontSize = 13.sp)
        }

        LazyRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(points) { index, point ->
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.95f))
                        .clickable {
                            InfoWindow.closeAllInfoWindowsOn(mapView)
                            markers[index].showInfoWindow()
                            mapView.controller.animateTo(GeoPoint(point.lat, point.lon))
                        }
                        .padding(12.dp)
                ) {
                    Text(text = point.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = point.address, fontSize = 12.sp, color = AppColors.Hint)
                    Text(text = point.description, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Настройка osmdroid: кэш в папках приложения (разрешение на память не нужно) и User-Agent для тайлов OSM. */
private fun initOsmdroid(context: Context) {
    val config = Configuration.getInstance()
    config.load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    config.userAgentValue = USER_AGENT
    config.osmdroidBasePath = File(context.filesDir, "osmdroid").apply { mkdirs() }
    config.osmdroidTileCache = File(context.cacheDir, "osmdroid_tiles").apply { mkdirs() }
}
