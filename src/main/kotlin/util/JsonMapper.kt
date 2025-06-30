package util

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.math.BigDecimal

object JsonMapper {

    val defaultMapper = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        serializersModule = SerializersModule {
            contextual(BigDecimal::class, BigDecimalSerializer)
        }
    }

}
