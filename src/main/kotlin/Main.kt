import api.prices
import bitstamp.BitstampApiClient
import bitstamp.BitstampService
import bitstamp.BitstampWsClient
import io.ktor.client.engine.okhttp.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import service.TickerDataService
import service.TickerPriceService
import storage.InMemoryMarketDataStorage
import util.JsonMapper
import util.getLogger

private val logger = getLogger<Application>()

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {

    val config = environment.config
    val tickers = config.property("app.tickers").getList().toSet()

    install(ContentNegotiation) {
        json(JsonMapper.defaultMapper)
    }

    val marketDataStorage = InMemoryMarketDataStorage()
    val tickerPriceService = TickerPriceService(marketDataStorage)

    val bitstampHttpClient = BitstampApiClient.createHttpClient(OkHttp.create())
    val bitstampService = BitstampService(BitstampApiClient(bitstampHttpClient), tickers, tickerPriceService)

    val bitstampWsHttpClient = BitstampWsClient.createHttpClient(OkHttp.create())
    lateinit var bitstampWsClient: BitstampWsClient

    val tickerDataService = TickerDataService(marketDataStorage, bitstampService)

    val appScope = CoroutineScope(Dispatchers.Default + Job())

    appScope.launch {
        try {
            val availableTickers = tickerDataService.getAvailableMarkets()
            availableTickers.forEach { ticker ->
                logger.info(" - Available Ticker: ${ticker.name} (${ticker.marketSymbol})")
            }

            bitstampWsClient = BitstampWsClient(bitstampWsHttpClient, bitstampService, availableTickers)
            bitstampWsClient.start()
        } catch (e: Exception) {
            logger.error("Error during initial Bitstamp data retrieval or WS startup: {}.", e.message, e)
        }
    }

    routing {
        prices(tickerPriceService)
    }

    monitor.subscribe(ApplicationStopping) {
        logger.info("Shutting down....")
        bitstampWsClient.stop()
        bitstampWsHttpClient.close()
        bitstampHttpClient.close()
        appScope.cancel("Application is stopping...")
    }
}
