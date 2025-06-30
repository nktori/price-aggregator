package bitstamp

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class Message(
    val event: String,
    val channel: String,
    val data: Data?
)

@Serializable
data class Data(
    val id: Long? = null,
    val timestamp: Long? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal? = null
)
