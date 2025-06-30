package service

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import storage.MarketDataStorage
import util.BTCUSD
import util.ETHBTC

class TickerDataServiceTest {

    private val mockMarketDataStorage: MarketDataStorage = mock()
    private val mockPriceAggregator: PriceAggregator = mock()

    private val tickerDataService = TickerDataService(mockMarketDataStorage, mockPriceAggregator)

    @Test
    fun `should fetch from markets and update storage`() = runTest {
        val tickers = listOf(BTCUSD, ETHBTC)
        whenever(mockPriceAggregator.getAvailableTickers()).thenReturn(tickers)

        val result = tickerDataService.getAvailableMarkets()

        verify(mockPriceAggregator).getAvailableTickers()
        verify(mockMarketDataStorage).updateTickerData(eq(tickers))
        assertEquals(tickers, result)
    }

    @Test
    fun `should handle empty markets list`() = runTest {
        whenever(mockPriceAggregator.getAvailableTickers()).thenReturn(emptyList())

        val result = tickerDataService.getAvailableMarkets()

        verify(mockPriceAggregator).getAvailableTickers()
        verify(mockMarketDataStorage, never()).updateTickerData(any())
        assertTrue { result.isEmpty() }
    }
}
