package bitstamp

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import model.TickerData
import util.JsonMapper
import util.getLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class BitstampWsClient(
    private val httpClient: HttpClient,
    private val bitstampService: BitstampService,
    availableTickers: List<TickerData>,
    private val reconnectDelay: Duration = 5.seconds,
    private val wsUrlHost: String = WS_URL_HOST
) {

    private val logger = getLogger<BitstampWsClient>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val subscriptionChannels: List<String> = availableTickers.map { marketTicker ->
        "${CHANNEL_PREFIX}${marketTicker.marketSymbol}"
    }

    companion object {
        private const val WS_URL_HOST = "ws.bitstamp.net"
        private const val CHANNEL_PREFIX = "live_trades_"

        fun createHttpClient(
            engine: HttpClientEngine,
            config: HttpClientConfig<*>.() -> Unit = {}
        ): HttpClient {
            return HttpClient(engine) {
                install(WebSockets)
                config()
            }
        }
    }

    private var connectionJob: Job? = null

    fun start() {
        if (connectionJob?.isActive == true) {
            logger.warn("BitstampWsClient is already running.")
            return
        }

        connectionJob = scope.launch {
            while (isActive) {
                try {
                    httpClient.wss(method = HttpMethod.Get, host = wsUrlHost, path = "/") {
                        subscribeToChannels(this)
                        processIncomingFrames(this)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error(
                        "WebSocket connection error: ${e.message}. Retrying in $reconnectDelay...",
                        e
                    )
                }
                delay(reconnectDelay)
            }
            logger.info("Connection job cancelled or completed.")
        }
    }

    fun stop() {
        connectionJob?.cancel()
        scope.cancel()
        logger.info("BitstampWsClient stopped")
    }

    private suspend fun subscribeToChannels(session: DefaultClientWebSocketSession) {
        subscriptionChannels.forEach { channel ->
            val subscribeMessage = JsonMapper.defaultMapper.encodeToString(
                JsonObject(
                    mapOf(
                        "event" to JsonPrimitive("bts:subscribe"),
                        "data" to JsonObject(mapOf("channel" to JsonPrimitive(channel)))
                    )
                )
            )
            session.outgoing.send(Frame.Text(subscribeMessage))
        }
    }

    private suspend fun processIncomingFrames(session: DefaultClientWebSocketSession) {
        for (frame in session.incoming) {
            when (frame) {
                is Frame.Text -> handleTextMessage(frame.readText())
                is Frame.Close -> handleCloseFrame(frame.readReason())
                is Frame.Binary, is Frame.Ping, is Frame.Pong -> Unit
            }
        }
    }

    private fun handleTextMessage(rawMessage: String) {
        val message = runCatching {
            JsonMapper.defaultMapper.decodeFromString<Message>(rawMessage)
        }.onFailure {
            logger.warn("Failed to parse Bitstamp message: ${it.message}")
        }.getOrNull()

        message?.let {
            when (it.event) {
                "trade" -> handleTradeEvent(it.channel, it.data, rawMessage)
                "bts:subscription_succeeded" -> logger.info("Subscription successful for channel: ${it.channel}")
                else -> logger.debug("Received unhandled event type: ${it.event} on channel: ${it.channel}. Message: $rawMessage")
            }
        } ?: logger.warn("Unable to process message: $rawMessage")
    }

    private fun handleTradeEvent(channel: String, data: Data?, rawMessage: String) {
        data?.let { tradeData ->
            val symbolFromChannel = channel.substringAfter(CHANNEL_PREFIX)
            val price = tradeData.price
            val timestamp = tradeData.timestamp

            if (symbolFromChannel.isBlank() || price == null || timestamp == null) {
                logger.debug("Incomplete trade data: Symbol=$symbolFromChannel, Price=$price, Timestamp=$timestamp, Raw=$rawMessage")
            } else {
                bitstampService.updateTickerPrice(symbolFromChannel, price, timestamp)
                logger.debug("Received trade update for $symbolFromChannel: Price=$price, Timestamp=$timestamp")
            }

        } ?: logger.warn("Missing data in trade message: $rawMessage")
    }

    private fun handleCloseFrame(reason: CloseReason?) {
        logger.info("WebSocket closed: $reason")
    }
}
