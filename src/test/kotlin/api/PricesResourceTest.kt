package api

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.datetime.Instant
import model.TickerPrice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import service.TickerPriceService
import util.JsonMapper
import java.math.BigDecimal

class PricesResourceTest {

    private val mockTickerPriceService: TickerPriceService = mock()

    @Test
    fun `should return price for existing ticker`() = testApplication {
        val client = configureServerAndGetClient()

        val priceString = "12345.67"
        val timeString = "2000-01-01T00:00:00Z"

        val tickerPrice = TickerPrice(BigDecimal(priceString), Instant.parse(timeString))
        whenever(mockTickerPriceService.getTickerPrice(eq("btcusd"))).thenReturn(tickerPrice)

        val response = client.get("/prices/BTC-USD")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())

        val responseBody = response.bodyAsText().replace("\\s".toRegex(), "")
        val expectedJson = """{"price":"$priceString","timestamp":"$timeString"}"""
        assertEquals(expectedJson, responseBody)
    }

    @Test
    fun `should return 404 for non-existing price`() = testApplication {
        val client = configureServerAndGetClient()

        whenever(mockTickerPriceService.getTickerPrice(eq("btcusd"))).thenReturn(null)

        val response = client.get("/prices/BTC-USD")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(ContentType.Text.Plain.withCharset(Charsets.UTF_8), response.contentType())
        assertEquals("Ticker price for symbol 'BTC-USD' not found.", response.bodyAsText())
    }

    @Test
    fun `should return 400 for invalid symbol format`() = testApplication {
        val client = configureServerAndGetClient()

        val response = client.get("/prices/INVALID-SYMBOL")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ContentType.Text.Plain.withCharset(Charsets.UTF_8), response.contentType())
        assertEquals("Invalid symbol format. Expected BASE_CCY-QUOTE_CCY. Got INVALID-SYMBOL", response.bodyAsText())
    }

    private fun ApplicationTestBuilder.configureServerAndGetClient(): HttpClient {
        application {
            this.install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json(JsonMapper.defaultMapper)
            }
            this.routing {
                prices(mockTickerPriceService)
            }
        }
        val client = createClient {
            install(ContentNegotiation) {
                json(JsonMapper.defaultMapper)
            }
        }
        return client
    }
}
