# Configuración técnica

Java 21, Spring Boot 4.1.2, Maven y Resilience4j 2.4.0 (`resilience4j-spring-boot4`).

## Puertos y variables

| Servicio | Puerto | Variable |
| --- | --- | --- |
| account-opening-orchestrator-service | 8080 | `ACCOUNT_SERVICE_URL`, `CARD_SERVICE_URL` |
| account-service | 8081 | — |
| card-service | 8082 | `ACCOUNT_SERVICE_URL` |

Valores locales por defecto: `http://localhost:8081` y `http://localhost:8082`.

## Resiliencia del orquestador y de card-service

| Parámetro | Valor |
| --- | --- |
| Timeout de conexión | 500 ms |
| Timeout de lectura | 5 s |
| Ventana | 4 llamadas, por conteo |
| Mínimo para evaluar | 4 |
| Umbral de fallos | 50 % |
| Llamada lenta | desde 1 s, umbral 50 % |
| Espera en abierto | 15 s |
| Llamadas en semiabierto | 2 |
| Bulkhead | 20 llamadas concurrentes |
| Reintentos automáticos | apagados |

Instancias: `accountService` y `cardService` en el orquestador. `accountService` en card-service. La salud del proceso no depende del estado del circuito. El estado se consulta en `/actuator/circuitbreakers` y en `GET /api/v1/account-openings/{sagaId}`.

## Comisiones y simulación

| cardType | Comisión |
| --- | --- |
| DEBIT | 15000.00 COP |
| CREDIT | 35000.00 COP |

`app.simulation.enabled=true` acepta `X-Delay-Milliseconds` y `X-Force-Failure`. Para abrir el circuito por latencia, repetir la apertura con `X-Delay-Milliseconds: 2000` cuatro veces y enviar una quinta sin espera.

## Datos

Cada proceso usa su H2 en memoria. Al detener el servicio, los datos se pierden.
