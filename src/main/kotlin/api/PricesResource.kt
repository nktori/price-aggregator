package api

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import service.TickerPriceService

private val SYMBOL_REGEX = Regex("^[A-Z0-9]{3,4}-[A-Z0-9]{3,4}$")

fun Route.prices(tickerPriceService: TickerPriceService) {
    route("/prices") {
        get("/{symbol}") {
            val symbol = call.parameters["symbol"]
                ?: return@get call.respondText(
                    "Missing symbol",
                    status = HttpStatusCode.BadRequest
                )

            if (!SYMBOL_REGEX.matches(symbol)) {
                return@get call.respondText(
                    "Invalid symbol format. Expected BASE_CCY-QUOTE_CCY. Got $symbol",
                    status = HttpStatusCode.BadRequest
                )
            }

            val normalizedSymbol = symbol.replace("-", "").lowercase()
            val latestPrice = tickerPriceService.getTickerPrice(normalizedSymbol)
                ?: return@get call.respondText(
                    "Ticker price for symbol '$symbol' not found.",
                    status = HttpStatusCode.NotFound
                )

            call.respond(latestPrice)
        }
    }
}
