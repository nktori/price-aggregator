package bitstamp

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import service.TickerPriceService
import util.BTCUSD
import util.ETHBTC
import util.ETHUSD
import util.XRPUSD
import java.math.BigDecimal

class BitstampServiceTest {

    private val mockApiClient: BitstampApiClient = mock()
    private val mockTickerPriceService: TickerPriceService = mock()

    private val tickerConfig = setOf("btcusd", "ethusd", "ethbtc")
    private val bitstampService = BitstampService(mockApiClient, tickerConfig, mockTickerPriceService)

    @Test
    fun `should filter markets based on tickerConfig`() = runTest {
        val apiResponse = listOf(BTCUSD, ETHUSD, ETHBTC, XRPUSD)
        whenever(mockApiClient.getMarkets()).thenReturn(apiResponse)

        val result = bitstampService.getAvailableTickers()

        verify(mockApiClient).getMarkets()

        assertEquals(3, result.size)
        assertTrue {
            result.contains(BTCUSD)
            result.contains(ETHUSD)
            result.contains(ETHBTC)
        }
        assertFalse { result.contains(XRPUSD) }
    }

    @Test
    fun `should return empty list on API client error`() = runTest {
        whenever(mockApiClient.getMarkets()).thenAnswer { throw RuntimeException("ApiClient error") }

        val result = bitstampService.getAvailableTickers()

        verify(mockApiClient).getMarkets()
        assertTrue { result.isEmpty() }
    }

    @Test
    fun `should call tickerPriceService if symbol is in config`() {
        val symbol = "ethbtc"
        val price = BigDecimal("0.0227723")
        val timestamp = 1234567890L

        bitstampService.updateTickerPrice(symbol, price, timestamp)

        verify(mockTickerPriceService).updateTickerPrice(eq(symbol), eq(price), eq(timestamp))
    }

    @Test
    fun `should not call tickerPriceService if symbol is not in config`() {
        val symbol = "xrpusd"
        val price = BigDecimal("100.00")
        val timestamp = 1234567890L

        bitstampService.updateTickerPrice(symbol, price, timestamp)

        verify(mockTickerPriceService, never()).updateTickerPrice(any(), any(), any())
    }
}
