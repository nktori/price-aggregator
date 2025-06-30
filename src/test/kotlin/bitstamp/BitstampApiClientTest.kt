package bitstamp

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BitstampApiClientTest {

    private fun mockHttpClient(
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): HttpClient = BitstampApiClient.createHttpClient(
        MockEngine { request -> handler(this, request) }
    )

    @Test
    fun `should return a list of TickerData on successful response`() = runTest {
        val jsonResponse = """
            [
                {
                    "name": "BTC/USD",
                    "market_symbol": "btcusd",
                    "base_currency": "BTC",
                    "base_decimals": 8,
                    "counter_currency": "USD",
                    "counter_decimals": 0,
                    "minimum_order_value": "10",
                    "trading": "Enabled",
                    "instant_order_counter_decimals": 2,
                    "instant_and_market_orders": "Enabled",
                    "description": "Bitcoin / U.S. dollar",
                    "market_type": "SPOT"
                },
                {
                    "name": "ETH/BTC",
                    "market_symbol": "ethbtc",
                    "base_currency": "ETH",
                    "base_decimals": 8,
                    "counter_currency": "BTC",
                    "counter_decimals": 8,
                    "minimum_order_value": "0.00020000",
                    "trading": "Enabled",
                    "instant_order_counter_decimals": 8,
                    "instant_and_market_orders": "Enabled",
                    "description": "Ethereum / Bitcoin",
                    "market_type": "SPOT"
                }
            ]
        """.trimIndent()

        val httpClient = mockHttpClient { request ->
            respond(
                jsonResponse,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val apiClient = BitstampApiClient(httpClient)

        val result = apiClient.getMarkets()

        assertEquals(2, result.size)
        assertEquals("btcusd", result[0].marketSymbol)
        assertEquals("ethbtc", result[1].marketSymbol)
        assertEquals("BTC/USD", result[0].name)
    }

    @Test
    fun `should throw ServerResponseException on 500 Internal Server Error`() = runTest {
        val httpClient = mockHttpClient {
            respond(
                "Internal Server Error Occurred",
                HttpStatusCode.InternalServerError,
                headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            )
        }
        val apiClient = BitstampApiClient(httpClient)

        assertThrows<ServerResponseException> {
            apiClient.getMarkets()
        }
    }

    @Test
    fun `should throw ClientRequestException on 404 Not Found`() = runTest {
        val httpClient = mockHttpClient {
            respondError(
                HttpStatusCode.NotFound,
                "{\"message\": \"Not found.\"}",
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val apiClient = BitstampApiClient(httpClient)

        assertThrows<ClientRequestException> {
            apiClient.getMarkets()
        }
    }

    @Test
    fun `should throw exception on malformed JSON response`() = runTest {
        val malformedJson = """
            [
                {"name": "BTC/USD", "market_symbol": "btcusd", "base_currency": "BTC",
                // broken JSON
        """.trimIndent()

        val httpClient = mockHttpClient {
            respond(
                malformedJson,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val apiClient = BitstampApiClient(httpClient)

        assertThrows<JsonConvertException> {
            apiClient.getMarkets()
        }
    }

    @Test
    fun `should throw exception on missing JSON field`() = runTest {
        val missingFieldJson = """
            [
                {
                    "name": "BTC/USD",
                    "market_symbol": "btcusd"
                }
            ]
        """.trimIndent()

        val httpClient = mockHttpClient {
            respond(
                missingFieldJson,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val apiClient = BitstampApiClient(httpClient)

        assertThrows<JsonConvertException> {
            apiClient.getMarkets()
        }
    }

    @Test
    fun `should handle empty list response`() = runTest {
        val emptyJson = "[]"
        val httpClient = mockHttpClient {
            respond(
                emptyJson,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val apiClient = BitstampApiClient(httpClient)

        val result = apiClient.getMarkets()

        assertTrue { result.isEmpty() }
    }

}
