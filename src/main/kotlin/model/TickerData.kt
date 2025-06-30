package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TickerData(
    val name: String,
    @SerialName("market_symbol") val marketSymbol: String,
    @SerialName("base_currency") val baseCurrency: String,
    @SerialName("base_decimals") val baseDecimal: Int,
    @SerialName("counter_currency") val counterCurrency: String,
    @SerialName("counter_decimals") val counterDecimal: Int,
)
