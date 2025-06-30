package storage

import model.TickerData
import model.TickerPrice

interface MarketDataStorage {

    fun updateTickerData(tickerData: List<TickerData>)

    fun getTickerData(marketSymbol: String): TickerData?

    fun updateTickerPrice(marketSymbol: String, newPrice: TickerPrice)

    fun getTickerPrice(marketSymbol: String): TickerPrice?

}
