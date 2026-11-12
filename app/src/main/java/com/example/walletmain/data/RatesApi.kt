package com.example.wallet.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Валюта или криптовалюта с актуальной ценой за 1 единицу в рублях. */
data class RateItem(
    val code: String,
    val name: String,
    val priceRub: Double,
    val isCrypto: Boolean
) {
    /** Уникальный ключ (коды фиата и крипты теоретически могут совпасть). */
    val key: String get() = "${if (isCrypto) "c" else "f"}:$code"

    /** Подпись в выпадающем списке. */
    val label: String get() = "$name ($code)"

    val source: String get() = if (isCrypto) "CoinGecko" else "ЦБ РФ"
}

/** Результат загрузки: список валют и тексты ошибок тех источников, которые не ответили. */
data class RatesResult(
    val items: List<RateItem>,
    val errors: List<String>
)

/**
 * Открытые API без ключа:
 *  - обычные валюты — курсы Банка России (https://www.cbr-xml-daily.ru);
 *  - криптовалюты — CoinGecko (https://www.coingecko.com/api), цена сразу в рублях.
 */
object RatesApi {
    private const val CBR_URL = "https://www.cbr-xml-daily.ru/daily_json.js"
    private const val COINGECKO_URL =
        "https://api.coingecko.com/api/v3/coins/markets?vs_currency=rub&order=market_cap_desc&per_page=30&page=1"

    /** Если у CoinGecko начнёт требоваться ключ, вставь сюда бесплатный Demo API key. */
    private const val COINGECKO_API_KEY = ""

    private val popularFiat = listOf("USD", "EUR", "CNY", "GBP", "JPY", "CHF", "KZT", "TRY")

    suspend fun loadAll(): RatesResult = coroutineScope {
        val fiat = async { runCatching { loadFiat() } }
        val crypto = async { runCatching { loadCrypto() } }
        val fiatResult = fiat.await()
        val cryptoResult = crypto.await()

        val errors = mutableListOf<String>()
        if (fiatResult.isFailure) errors += "курсы ЦБ РФ недоступны"
        if (cryptoResult.isFailure) errors += "курсы криптовалют недоступны"

        RatesResult(
            items = (fiatResult.getOrNull() ?: emptyList()) + (cryptoResult.getOrNull() ?: emptyList()),
            errors = errors
        )
    }

    private suspend fun loadFiat(): List<RateItem> = withContext(Dispatchers.IO) {
        val valute = JSONObject(httpGet(CBR_URL, withKey = false)).getJSONObject("Valute")
        val result = mutableListOf<RateItem>()
        val keys = valute.keys()
        while (keys.hasNext()) {
            val o = valute.getJSONObject(keys.next())
            val nominal = o.getDouble("Nominal")
            if (nominal <= 0.0) continue
            // Value — цена Nominal единиц валюты, поэтому делим
            result += RateItem(
                code = o.getString("CharCode"),
                name = o.getString("Name"),
                priceRub = o.getDouble("Value") / nominal,
                isCrypto = false
            )
        }
        result.sortedWith(
            compareBy<RateItem> { popularFiat.indexOf(it.code).let { i -> if (i < 0) Int.MAX_VALUE else i } }
                .thenBy { it.name }
        )
    }

    private suspend fun loadCrypto(): List<RateItem> = withContext(Dispatchers.IO) {
        val array = JSONArray(httpGet(COINGECKO_URL, withKey = true))
        val result = mutableListOf<RateItem>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            if (o.isNull("current_price")) continue
            result += RateItem(
                code = o.getString("symbol").uppercase(),
                name = o.getString("name"),
                priceRub = o.getDouble("current_price"),
                isCrypto = true
            )
        }
        result
    }

    private fun httpGet(url: String, withKey: Boolean): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "WalletApp/1.0")
            if (withKey && COINGECKO_API_KEY.isNotEmpty()) {
                conn.setRequestProperty("x-cg-demo-api-key", COINGECKO_API_KEY)
            }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
