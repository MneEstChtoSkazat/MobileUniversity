package com.example.wallet.data

/** Пункт обмена валюты на карте (демонстрационные данные, координаты приблизительные). */
data class ExchangePoint(
    val name: String,
    val address: String,
    val description: String,
    val lat: Double,
    val lon: Double
)

/** Статические данные: пункты обмена на карте и список валют для настроек. */
object SampleData {
    val exchangePoints = listOf(
        ExchangePoint(
            name = "Обмен валют «Центр»",
            address = "пл. Революции, Красноярск",
            description = "USD, EUR, CNY. Ежедневно 9:00–20:00",
            lat = 56.0102, lon = 92.8524
        ),
        ExchangePoint(
            name = "Обмен валют «Предмостная»",
            address = "Предмостная площадь, Красноярск",
            description = "USD, EUR, CNY, KZT. Пн–Сб 10:00–19:00",
            lat = 56.0195, lon = 92.8745
        ),
        ExchangePoint(
            name = "Обмен валют «Взлётка»",
            address = "Взлётка, Красноярск",
            description = "USD, EUR. Ежедневно 10:00–21:00",
            lat = 56.0328, lon = 92.9086
        )
    )

    val mainCurrencies = listOf("RUB", "USD", "EUR")
}
