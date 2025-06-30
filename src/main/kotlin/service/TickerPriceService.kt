package service

import kotlinx.datetime.Instant
import model.TickerPrice
import storage.MarketDataStorage
import util.getLogger
import java.math.BigDecimal
import java.math.RoundingMode

class TickerPriceService(
    private val marketDataStorage: MarketDataStorage
) {

    private val logger = getLogger<TickerPriceService>()

    fun updateTickerPrice(marketSymbol: String, newPrice: BigDecimal, timestamp: Long) {
        val tickerData = marketDataStorage.getTickerData(marketSymbol)
        if (tickerData == null) {
            logger.debug("Ticker data for market symbol '$marketSymbol' not found.")
            return
        }
        val price = newPrice.setScale(tickerData.counterDecimal, RoundingMode.DOWN)
        val instant = Instant.fromEpochSeconds(timestamp)
        marketDataStorage.updateTickerPrice(marketSymbol, TickerPrice(price, instant))
        logger.info("Ticker price for $marketSymbol has been updated to $price at $instant.")
    }

    fun getTickerPrice(marketSymbol: String): TickerPrice? = marketDataStorage.getTickerPrice(marketSymbol)

}
