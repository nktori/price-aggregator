package bitstamp

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import util.BTCUSD
import util.ETHBTC
import util.JsonMapper
import java.math.BigDecimal
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BitstampWsClientTest {

    private val mockBitstampService: BitstampService = mock()

    private lateinit var bitstampWsClient: BitstampWsClient

    @AfterEach
    fun teardown() {
        if (::bitstampWsClient.isInitialized) {
            bitstampWsClient.stop()
        }
    }

    @Test
    fun `handle disconnection`() = runTest {
        testApplication {
            val firstConnection = CompletableDeferred<Unit>()
            val reconnection = CompletableDeferred<Unit>()
            var connectionCount = 0

            bitstampWsClient = createTestClient {
                connectionCount++
                if (connectionCount == 1) {
                    firstConnection.complete(Unit)
                    incoming.receive()
                    delay(100.milliseconds)
                    close(CloseReason(CloseReason.Codes.NORMAL, "Simulating disconnect"))
                } else if (connectionCount == 2) {
                    reconnection.complete(Unit)
                } else {
                    fail("Unexpected connection count: $connectionCount")
                }
            }

            bitstampWsClient.start()

            withTimeout(5.seconds) {
                firstConnection.await()
            }

            withTimeout(10.seconds) {
                reconnection.await()
            }

            assertTrue(firstConnection.isCompleted)
            assertTrue(reconnection.isCompleted)
        }
    }

    @Test
    fun `sends subscription messages for all channels`() = runTest {
        testApplication {
            val deferredMessages = CompletableDeferred<List<String>>()

            bitstampWsClient = createTestClient {
                val subscriptionMessages = mutableListOf<String>()
                try {
                    repeat(2) {
                        val frame = incoming.receive() as Frame.Text
                        subscriptionMessages.add(frame.readText())
                    }
                    deferredMessages.complete(subscriptionMessages)
                    delay(100.milliseconds)
                } catch (e: Exception) {
                    fail("Websocket test server error: ${e.message}")
                }
            }

            bitstampWsClient.start()

            val receivedMessages = withTimeout(5.seconds) {
                deferredMessages.await()
            }

            val expectedBtcUsdSubscription = JsonMapper.defaultMapper.encodeToString(
                JsonObject(
                    mapOf(
                        "event" to JsonPrimitive("bts:subscribe"),
                        "data" to JsonObject(mapOf("channel" to JsonPrimitive("live_trades_btcusd")))
                    )
                )
            )

            val expectedEthBtcSubscription = JsonMapper.defaultMapper.encodeToString(
                JsonObject(
                    mapOf(
                        "event" to JsonPrimitive("bts:subscribe"),
                        "data" to JsonObject(mapOf("channel" to JsonPrimitive("live_trades_ethbtc")))
                    )
                )
            )

            val expectedMessages = setOf(expectedBtcUsdSubscription, expectedEthBtcSubscription)

            assertEquals(expectedMessages, receivedMessages.toSet())
        }
    }

    @Test
    fun `processes incoming trades`() = runTest {
        testApplication {
            val numberOfTrades = 300
            val deferredTrades = CompletableDeferred<Unit>()

            bitstampWsClient = createTestClient {
                repeat(numberOfTrades) { i ->
                    val tradeMessage = JsonMapper.defaultMapper.encodeToString(
                        JsonObject(
                            mapOf(
                                "data" to JsonObject(
                                    mapOf(
                                        "id" to JsonPrimitive(1234567890L + i),
                                        "timestamp" to JsonPrimitive("${1234567890L + i}"),
                                        "amount" to JsonPrimitive("0.00010000"),
                                        "amount_str" to JsonPrimitive("0.00010000"),
                                        "price" to JsonPrimitive("${100.00 + i}"),
                                        "price_str" to JsonPrimitive("${100.00 + i}"),
                                        "type" to JsonPrimitive(0)
                                    )
                                ),
                                "event" to JsonPrimitive("trade"),
                                "channel" to JsonPrimitive("live_trades_btcusd")
                            )
                        )
                    )
                    outgoing.send(Frame.Text(tradeMessage))
                }
                deferredTrades.complete(Unit)
            }

            bitstampWsClient.start()

            withTimeout(5.seconds) {
                deferredTrades.await()
            }

            val tickerCaptor = argumentCaptor<String>()
            val priceCaptor = argumentCaptor<BigDecimal>()
            val timestampCaptor = argumentCaptor<Long>()

            verify(mockBitstampService, timeout(1000).times(numberOfTrades)).updateTickerPrice(
                tickerCaptor.capture(),
                priceCaptor.capture(),
                timestampCaptor.capture()
            )

            val allTickers = tickerCaptor.allValues
            val allPrices = priceCaptor.allValues
            val allTimestamps = timestampCaptor.allValues

            assertEquals(numberOfTrades, allTickers.size)
            assertEquals(numberOfTrades, allPrices.size)
            assertEquals(numberOfTrades, allTimestamps.size)

            assertEquals("btcusd", allTickers[0])
            assertEquals(0, BigDecimal("100.00").compareTo(allPrices[0]))
            assertEquals(1234567890L, allTimestamps[0])

            assertEquals("btcusd", allTickers[1])
            assertEquals(0, BigDecimal("101.00").compareTo(allPrices[1]))
            assertEquals(1234567891L, allTimestamps[1])

            assertEquals("btcusd", allTickers[299])
            assertEquals(0, BigDecimal("399.00").compareTo(allPrices[299]))
            assertEquals(1234568189L, allTimestamps[299])
        }
    }


    @Test
    fun `handle malformed trade data`() = runTest {
        testApplication {
            val deferredTrades = CompletableDeferred<Unit>()

            bitstampWsClient = createTestClient {
                val tradeMessage = JsonMapper.defaultMapper.encodeToString(
                    JsonObject(
                        mapOf(
                            "data" to JsonObject(
                                mapOf(
                                    "id" to JsonPrimitive(1234567890L),
                                    // "timestamp" to JsonPrimitive("${1234567890L}"), missing timestamp
                                    "amount" to JsonPrimitive("0.00010000"),
                                    "price" to JsonPrimitive("${100.00}"),
                                    "type" to JsonPrimitive(0)
                                )
                            ),
                            "event" to JsonPrimitive("trade"),
                            "channel" to JsonPrimitive("live_trades_btcusd")
                        )
                    )
                )
                outgoing.send(Frame.Text(tradeMessage))

                deferredTrades.complete(Unit)
            }

            bitstampWsClient.start()

            withTimeout(5.seconds) {
                deferredTrades.await()
            }

            verify(mockBitstampService, never()).updateTickerPrice(any(), any(), any())
        }
    }

    private fun ApplicationTestBuilder.createTestClient(
        webSocketServerBehavior: suspend DefaultWebSocketServerSession.() -> Unit
    ): BitstampWsClient {
        application {
            install(WebSockets)
            routing {
                webSocket("/") {
                    webSocketServerBehavior()
                }
            }
        }

        val wsHttpClient = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        val clientInstance = BitstampWsClient(
            httpClient = wsHttpClient,
            bitstampService = mockBitstampService,
            availableTickers = listOf(BTCUSD, ETHBTC),
            reconnectDelay = 1.seconds,
            wsUrlHost = "localhost"
        )
        return clientInstance
    }
}
