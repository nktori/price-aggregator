package service

import kotlinx.datetime.Instant
import model.TickerPrice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import storage.MarketDataStorage
import util.ETHBTC
import java.math.BigDecimal

class TickerPriceServiceTest {

    private val mockStorage: MarketDataStorage = mock()

    private val tickerPriceService = TickerPriceService(mockStorage)

    @Test
    fun `should update price with correct scaling and timestamp conversion`() {
        val marketSymbol = "ethbtc"
        val rawNewPrice = BigDecimal("0.0227723")
        val timestampLong = 1234567890L

        whenever(mockStorage.getTickerData(marketSymbol)).thenReturn(ETHBTC)

        tickerPriceService.updateTickerPrice(marketSymbol, rawNewPrice, timestampLong)

        val expectedScaledPrice = BigDecimal("0.02277230")
        val expectedInstant = Instant.fromEpochSeconds(timestampLong)
        verify(mockStorage).updateTickerPrice(
            eq(marketSymbol),
            check { stored ->
                assertEquals(0, expectedScaledPrice.compareTo(stored.price))
                assertEquals(expectedInstant, stored.timestamp)
            }
        )
    }

    @Test
    fun `should not update if ticker data is not found`() {
        val marketSymbol = "xrpusd"
        val rawNewPrice = BigDecimal("100.00")
        val timestampLong = 1234567890L

        whenever(mockStorage.getTickerData(marketSymbol)).thenReturn(null)

        tickerPriceService.updateTickerPrice(marketSymbol, rawNewPrice, timestampLong)

        verify(mockStorage, never()).updateTickerPrice(any(), any())
    }

    @Test
    fun `should return price from storage`() {
        val marketSymbol = "ethusd"
        val expectedPrice = TickerPrice(BigDecimal("2345.6"), Instant.fromEpochSeconds(1234567890L))

        whenever(mockStorage.getTickerPrice(marketSymbol)).thenReturn(expectedPrice)

        val result = tickerPriceService.getTickerPrice(marketSymbol)

        assertEquals(expectedPrice, result)
        verify(mockStorage).getTickerPrice(marketSymbol)
    }

    @Test
    fun `should return null if price not found in storage`() {
        val marketSymbol = "ethusd"

        whenever(mockStorage.getTickerPrice(marketSymbol)).thenReturn(null)

        val result = tickerPriceService.getTickerPrice(marketSymbol)

        assertNull(result)
        verify(mockStorage).getTickerPrice(marketSymbol)
    }
}
