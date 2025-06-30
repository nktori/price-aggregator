package model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class TickerPrice(
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    val timestamp: Instant
)
