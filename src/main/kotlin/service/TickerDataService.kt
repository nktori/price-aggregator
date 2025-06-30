package service

import model.TickerData
import storage.MarketDataStorage
import util.getLogger

class TickerDataService(
    private val marketDataStorage: MarketDataStorage,
    private val priceAggregator: PriceAggregator
) {
    private val logger = getLogger<TickerDataService>()

    suspend fun getAvailableMarkets(): List<TickerData> = priceAggregator.getAvailableTickers()
        .also { tickers ->
            if (tickers.isNotEmpty()) {
                marketDataStorage.updateTickerData(tickers)
            } else {
                logger.debug("No available markets found.")
            }
        }
}
