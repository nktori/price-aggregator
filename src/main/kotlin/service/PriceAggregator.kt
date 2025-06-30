package service

import model.TickerData
import java.math.BigDecimal

interface PriceAggregator {
    suspend fun getAvailableTickers(): List<TickerData>
    fun updateTickerPrice(marketSymbol: String, newPrice: BigDecimal, timestamp: Long)
}