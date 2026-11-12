package com.example.wallet.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallet.data.Asset
import com.example.wallet.data.BuyResult
import com.example.wallet.data.RateItem
import com.example.wallet.data.RatesUiState
import com.example.wallet.data.WalletUiState
import com.example.wallet.ui.AppColors
import com.example.wallet.ui.components.BackButton
import com.example.wallet.ui.formatQty
import com.example.wallet.ui.formatRub
import com.example.wallet.ui.formatRate

private val SellRed = Color(0xFFC62828)

private fun String.toNumber(): Double? = replace(',', '.').trim().toDoubleOrNull()

private fun Double.plain(): String = formatQty(this).replace(" ", "")

/**
 * Экран 03 «Редактор».
 * assetId == -1 — покупка новой валюты: валюта выбирается из выпадающего списка (обычные валюты и
 * криптовалюты), курс берётся из API, иконка случайная, баланс уменьшается.
 * Иначе — редактирование существующей записи: «Сохранить» (Update) и «Продать» (Delete + возврат рублей на баланс).
 */
@Composable
fun EditorScreen(
    assetId: Long,
    state: WalletUiState,
    rates: RatesUiState,
    onReloadRates: () -> Unit,
    onBack: () -> Unit,
    onSave: (Asset) -> Unit,
    onSell: (Long, (Double?) -> Unit) -> Unit,
    onBuy: (String, Double, Double, (BuyResult) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val isNew = assetId == -1L
    val existing = state.items.find { it.id == assetId }

    var currency by remember(assetId) { mutableStateOf(existing?.name ?: "") }
    var symbol by remember(assetId) { mutableStateOf(existing?.symbol ?: "") }
    var amount by remember(assetId) { mutableStateOf(existing?.amount?.plain() ?: "") }
    var rate by remember(assetId) { mutableStateOf(existing?.rateToRub?.plain() ?: "") }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    var showSellDialog by remember { mutableStateOf(false) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    // При покупке валюта и курс берутся из API (курс обновляется вместе со списком)
    val selected: RateItem? = rates.items.find { it.key == selectedKey }

    val parsedAmount = amount.toNumber()
    val parsedRate = if (isNew) selected?.priceRub else rate.toNumber()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        BackButton(onClick = onBack)

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            FieldLabel("Валюта")
            if (isNew) {
                CurrencyDropdown(
                    selected = selected,
                    items = rates.items,
                    loading = rates.loading,
                    onSelect = { selectedKey = it.key }
                )
                if (rates.loading) {
                    Text("Загрузка курсов…", fontSize = 12.sp, color = AppColors.Hint, modifier = Modifier.padding(top = 4.dp))
                }
                if (rates.error != null) {
                    Text(
                        text = "${rates.error}. Проверьте интернет.",
                        fontSize = 12.sp,
                        color = SellRed,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                FormTextField(value = currency, onValueChange = { currency = it }, placeholder = "Название")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isNew) {
                FieldLabel("Символ (необязательно)")
                FormTextField(
                    value = symbol,
                    onValueChange = { symbol = it.take(2) },
                    placeholder = "Например, ¥"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            FieldLabel("Сумма (количество единиц)")
            FormTextField(
                value = amount,
                onValueChange = { amount = it },
                placeholder = "Количество",
                keyboardType = KeyboardType.Decimal
            )

            Spacer(modifier = Modifier.height(16.dp))

            FieldLabel("Курс, ₽ за 1 единицу")
            if (isNew) {
                // Курс по API: только для чтения
                FormTextField(
                    value = selected?.let { formatRate(it.priceRub) } ?: "",
                    onValueChange = {},
                    placeholder = "Выберите валюту",
                    readOnly = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onReloadRates, enabled = !rates.loading) {
                        Text("Обновить курсы", fontSize = 13.sp, color = AppColors.ButtonDark)
                    }
                    if (selected != null) {
                        Text("Источник: ${selected.source}", fontSize = 12.sp, color = AppColors.Hint)
                    }
                }
            } else {
                FormTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    placeholder = "Курс",
                    keyboardType = KeyboardType.Decimal
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            FieldLabel("Стоимость в валюте пользователя")
            RateBox(
                rate = parsedRate,
                amount = parsedAmount,
                balance = if (isNew) state.balance else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isNew) {
                // ---------- Купить: Create + списание баланса ----------
                DarkButton(text = "Купить") {
                    when {
                        selected == null -> toast("Выберите валюту")
                        parsedAmount == null || parsedAmount <= 0 -> toast("Введите корректную сумму")
                        parsedRate == null || parsedRate <= 0 -> toast("Курс недоступен, обновите курсы")
                        else -> onBuy(selected.name, parsedAmount, parsedRate) { result ->
                            when (result) {
                                is BuyResult.Success -> {
                                    toast("Куплено за ${formatRub(result.cost)}. Баланс: ${formatRub(result.newBalance)}")
                                    onBack()
                                }
                                is BuyResult.NotEnoughMoney ->
                                    toast("Недостаточно средств. Баланс: ${formatRub(result.balance)}")
                                BuyResult.Invalid -> toast("Не удалось выполнить покупку")
                            }
                        }
                    }
                }
            } else {
                // ---------- Сохранить: Update ----------
                DarkButton(text = "Сохранить") {
                    when {
                        currency.isBlank() -> toast("Введите название валюты")
                        parsedAmount == null || parsedAmount < 0 -> toast("Введите корректную сумму")
                        parsedRate == null || parsedRate <= 0 -> toast("Введите корректный курс")
                        else -> onSave(
                            Asset(
                                id = assetId,
                                symbol = symbol.ifBlank { currency.trim().first().uppercase() },
                                name = currency.trim(),
                                amount = parsedAmount,
                                rateToRub = parsedRate
                            )
                        )
                    }
                }

                if (existing != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // ---------- Продать: Delete + рубли на баланс ----------
                    OutlinedButton(
                        onClick = { showSellDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(text = "Продать", fontSize = 14.sp, color = SellRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSellDialog && existing != null) {
        AlertDialog(
            onDismissRequest = { showSellDialog = false },
            title = { Text("Продать «${existing.name}»?") },
            text = { Text("Вы получите ${formatRub(existing.valueRub)} на баланс.") },
            confirmButton = {
                TextButton(onClick = {
                    showSellDialog = false
                    onSell(existing.id) { proceeds ->
                        if (proceeds != null) toast("Продано за ${formatRub(proceeds)}")
                        onBack()
                    }
                }) { Text("Продать", color = SellRed) }
            },
            dismissButton = {
                TextButton(onClick = { showSellDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun DarkButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.ButtonDark,
            contentColor = Color.White
        )
    ) {
        Text(text = text, fontSize = 14.sp)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = 6.dp),
        fontSize = 13.sp
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholder, color = AppColors.Hint, fontSize = 14.sp) },
        singleLine = true,
        readOnly = readOnly,
        shape = RoundedCornerShape(6.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = AppColors.FieldBorder,
            focusedBorderColor = AppColors.ButtonDark
        )
    )
}

/** Блок со стоимостью (и балансом при покупке). */
@Composable
private fun RateBox(rate: Double?, amount: Double?, balance: Double?) {
    val total = if (rate != null && amount != null) formatRub(rate * amount) else "—"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColors.FieldBorder, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Курс: 1 ед. = ${if (rate != null) formatRate(rate) else "—"}",
            fontSize = 12.sp,
            color = AppColors.Hint
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "Стоимость в RUB: $total", fontSize = 12.sp, color = AppColors.Hint)
        if (balance != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Ваш баланс: ${formatRub(balance)}", fontSize = 12.sp, color = AppColors.Hint)
        }
    }
}

/** Выпадающий список валют и криптовалют (в том же стиле, что и список в настройках). */
@Composable
private fun CurrencyDropdown(
    selected: RateItem?,
    items: List<RateItem>,
    loading: Boolean,
    onSelect: (RateItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val enabled = items.isNotEmpty()

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, AppColors.FieldBorder, RoundedCornerShape(6.dp))
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected?.label ?: if (loading && !enabled) "Загрузка…" else "Выберите валюту",
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = if (selected == null) AppColors.Hint else AppColors.Ink
            )
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Выбрать валюту")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            val fiat = items.filter { !it.isCrypto }
            val crypto = items.filter { it.isCrypto }

            if (fiat.isNotEmpty()) {
                MenuHeader("Валюты (ЦБ РФ)")
                fiat.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.label, fontSize = 14.sp) },
                        onClick = { onSelect(item); expanded = false }
                    )
                }
            }
            if (crypto.isNotEmpty()) {
                MenuHeader("Криптовалюты (CoinGecko)")
                crypto.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.label, fontSize = 14.sp) },
                        onClick = { onSelect(item); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        fontSize = 12.sp,
        color = AppColors.Hint,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )
}
