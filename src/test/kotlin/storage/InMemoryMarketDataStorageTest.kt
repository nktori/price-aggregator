package storage

import kotlinx.datetime.Instant
import model.TickerData
import model.TickerPrice
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class InMemoryMarketDataStorageTest {

    private lateinit var storage: InMemoryMarketDataStorage

    @BeforeEach
    fun setup() {
        storage = InMemoryMarketDataStorage()
    }

    @Test
    fun `should store and update ticker data`() {
        val ticker1 = TickerData("BTC/USD", "btcusd", "BTC", 8, "USD", 0)
        val ticker2 = TickerData("ETH/USD", "ethusd", "ETH", 8, "USD", 1)
        val ticker3 = TickerData("ETH/BTC", "ethbtc", "ETH", 8, "ETH", 8)
        val tickers = listOf(ticker1, ticker2, ticker3)

        storage.updateTickerData(tickers)

        assertEquals(ticker1, storage.getTickerData("btcusd"))
        assertEquals(ticker2, storage.getTickerData("ethusd"))
        assertEquals(ticker3, storage.getTickerData("ethbtc"))
        assertNull(storage.getTickerData("xrpusd"))

        val updatedTicker = TickerData("BTC/USD", "btcusd", "BTC", 9, "USD", 1)
        storage.updateTickerData(listOf(updatedTicker))

        assertEquals(updatedTicker, storage.getTickerData("btcusd"))
    }

    @Test
    fun `should return null for symbol with no ticker data`() {
        assertNull(storage.getTickerData("xrpusd"))
    }

    @Test
    fun `should store and update ticker price`() {
        val price = TickerPrice(BigDecimal("0.00092822"), Instant.fromEpochSeconds(1234567890L))

        storage.updateTickerPrice("ethbtc", price)

        var retrievedPrice = storage.getTickerPrice("ethbtc")
        assertNotNull(retrievedPrice)
        assertEquals(0, price.price.compareTo(retrievedPrice?.price))
        assertEquals(price.timestamp, retrievedPrice?.timestamp)

        val updatedPrice = TickerPrice(BigDecimal("0.11111111"), Instant.fromEpochSeconds(1234567899L))
        storage.updateTickerPrice("btcusd", updatedPrice)

        retrievedPrice = storage.getTickerPrice("btcusd")
        assertNotNull(retrievedPrice)
        assertEquals(0, updatedPrice.price.compareTo(retrievedPrice?.price))
        assertEquals(updatedPrice.timestamp, retrievedPrice?.timestamp)
    }

    @Test
    fun `should return null for symbol with no price data`() {
        assertNull(storage.getTickerPrice("xrpusd"))
    }
}
