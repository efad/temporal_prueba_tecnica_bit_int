# Prueba técnica

Tres microservicios Spring Boot abren una cuenta, emiten una tarjeta y cobran la comisión de emisión con una saga orquestada y circuit breaker.

El proyecto se trabaja con **spec** y **skills**. `spec.md` es la fuente de verdad y `specs/apertura-cuenta-tarjeta-comision.md` describe esta historia. El skill `microservices-patterns` guía los patrones; aquí se implementan en Java. Si discrepan, manda el spec.

## Servicios

| Servicio | Puerto |
| --- | --- |
| account-opening-orchestrator-service | 8080 |
| account-service | 8081 |
| card-service | 8082 |

## Ejecución local

Hace falta JDK 21 y Maven (Eclipse Temurin 21 sirve)  apuntando `JAVA_HOME` a esa instalación. En PowerShell, para la sesión actual:

```powershell
$env:JAVA_HOME = "C:\ruta\a\tu\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
java -version
mvn test
```

`java -version` tiene que mostrar la 21. Si ya está instalado directamente `mvn test`.

Para levantar los tres procesos, en terminales distintas y con Java 21:

```powershell
mvn -pl services/account-service spring-boot:run
mvn -pl services/card-service spring-boot:run
mvn -pl services/account-opening-orchestrator-service spring-boot:run
```

La apertura se prueba con `client/apertura-cuenta.http` o importando `client/apertura-cuenta.postman_collection.json`. OpenAPI queda en `/swagger-ui.html` de cada servicio.

## Escenarios

1. `POST /api/v1/account-openings` responde `201` y `sagaStatus=COMPLETED`.
2. La misma petición con `X-Force-Failure: CARD_CREATE` responde `422`, `FAILED`, y revierte la cuenta.
3. Cuatro aperturas con `X-Delay-Milliseconds: 2000` y una quinta sin espera: la quinta responde `503` y `PENDING_RETRY` porque el circuito de account-service está abierto.

La evidencia de `mvn test` está en `docs/evidencia-ejecucion.md`. El detalle de puertos, umbrales y compensaciones está en `docs/configuracion-tecnica.md` y `docs/c4-container.md`.
