package storage

import model.TickerData
import model.TickerPrice
import java.util.concurrent.ConcurrentHashMap

class InMemoryMarketDataStorage : MarketDataStorage {

    private val tickerData = ConcurrentHashMap<String, TickerData>()
    private val tickerPrices = ConcurrentHashMap<String, TickerPrice>()

    override fun updateTickerData(tickerData: List<TickerData>) {
        tickerData.forEach { ticker ->
            this.tickerData[ticker.marketSymbol] = ticker
        }
    }

    override fun getTickerData(marketSymbol: String): TickerData? {
        return tickerData[marketSymbol]
    }

    override fun updateTickerPrice(marketSymbol: String, newPrice: TickerPrice) {
        this.tickerPrices[marketSymbol] = newPrice
    }

    override fun getTickerPrice(marketSymbol: String): TickerPrice? {
        return tickerPrices[marketSymbol]
    }
}
