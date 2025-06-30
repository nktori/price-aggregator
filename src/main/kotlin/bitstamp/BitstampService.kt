package bitstamp

import model.TickerData
import service.PriceAggregator
import service.TickerPriceService
import util.getLogger
import java.math.BigDecimal

class BitstampService(
    private val apiClient: BitstampApiClient,
    private val tickerConfig: Set<String>,
    private val tickerPriceService: TickerPriceService
) : PriceAggregator {

    private val logger = getLogger<BitstampService>()

    override suspend fun getAvailableTickers(): List<TickerData> {
        return try {
            apiClient.getMarkets().filter { it.marketSymbol in tickerConfig }
        } catch (_: Exception) { // error is logged in the API client
            emptyList()
        }
    }

    override fun updateTickerPrice(marketSymbol: String, newPrice: BigDecimal, timestamp: Long) {
        if (marketSymbol !in tickerConfig) {
            logger.warn("Attempted to update price for unknown market symbol '$marketSymbol'.")
            return
        }
        tickerPriceService.updateTickerPrice(marketSymbol, newPrice, timestamp)
    }
}
