# Cryptocurrency Price Aggregator

A lightweight Kotlin application using the [Ktor](https://ktor.io/) library, fetching real-time market data from the Bitstamp API.

## How To Run

### Prerequisites

- JDK 21+

### Build the application

```shell  
# Linux/macOS
./gradlew build
# Windows
gradlew.bat build
```

### Run the application

```shell
# Linux/macOS
./gradlew run
# Windows
gradlew.bat run
```

#### Note

The application will start on http://localhost:8080 by default, as configured in `application.conf`.

You can specify the cryptocurrency pairs to aggregate by modifying the `app.tickers` property in `src/main/resources/application.conf`. 

For example:

```hocon
app {
    tickers = [ "btcusd", "ethusd", "ethbtc"]
}
```

## API Documentation

The application exposes a single REST API endpoint to retrieve the latest ticker price for a given symbol.

Endpoint: `GET /prices/{symbol}`

* `{symbol`}: The trading pair symbol in BASE_CCY-QUOTE_CCY format (e.g., BTC-USD, ETH-BTC). 

Example Usage:

```shell
$ curl localhost:8080/prices/ETH-BTC
{
    "price": "0.12345678",
    "timestamp": "2020-01-01T12:00:00Z"
}
```

Example Errors:

* Ticker not found for the given symbol.
```shell
$ curl localhost:8080/prices/XRP-USD
Ticker price for symbol 'XRP-USD' not found.
```

* Invalid symbol format.
```shell
$ curl localhost:8080/prices/INVALID
Invalid symbol format. Expected BASE_CCY-QUOTE_CCY. Got INVALID
```

## Design Decisions and Trade-offs

* Ktor Library
  * Kotlin-native and lightweight.

* In-Memory Data Storage
  * Simplicity and performance, very fast lookups and updates.
  * Small lightweight application, no need currently for distributed or persistent storage.
  * Does not support historical data, only the latest price as only latest price is needed for the API.

* Minimal API response
  * Only returns the latest price and timestamp for the requested symbol.
  * Price currency can be derived from the symbol.

## Enhancements and Future Work

* Support multiple exchanges
  * Implement further exchange-specific integration to handle trade data, independent integration per exchange.
  * Modify ticker data storage to link each ticker to the exchange it is available from.
  * Extend API response to include exchange information.

* Persistent Data Storage
  * Add a persistent data store for historical data and data persistence across application restarts.
  * Enables horizontal scaling by using a distributed database instead of in-memory storage.

* Scaling
  * Make the service stateless to allow horizontal scaling.
    * Use storage for data instead of in-memory storage.
  * Could split data ingestion into its own service to scale this independently.
    * Could go further and have a separate service for each exchange. Publishing data via a message broker to the aggregator service.
  * Caching layer for frequently accessed data.
  * Improve websocket handling
    * More robust reconnect, back-off strategies, and connection monitoring.
