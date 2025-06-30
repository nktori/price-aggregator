package bitstamp

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import model.TickerData
import util.JsonMapper
import util.getLogger

class BitstampApiClient(private val httpClient: HttpClient) {

    private val logger = getLogger<BitstampApiClient>()

    companion object {
        private const val BASE_URL = "www.bitstamp.net"
        private const val MARKETS_INFO_PATH = "/api/v2/markets/"

        fun createHttpClient(
            engine: HttpClientEngine,
            config: HttpClientConfig<*>.() -> Unit = {}
        ): HttpClient {
            return HttpClient(engine) {
                expectSuccess = true
                install(ContentNegotiation) {
                    json(JsonMapper.defaultMapper)
                }
                install(DefaultRequest) {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = BASE_URL
                    }
                }
                config()
            }
        }
    }

    suspend fun getMarkets(): List<TickerData> {
        return try {
            logger.info("Fetching market info from ${BASE_URL}${MARKETS_INFO_PATH}...")
            val response: List<TickerData> = httpClient.get(MARKETS_INFO_PATH).body()
            logger.info("Successfully fetched ${response.size} market entries.")
            response
        } catch (e: Exception) {
            logger.error("Failed to fetch market info: ${e.message}")
            throw e
        }
    }
}
