# Prueba técnica

Tres microservicios Spring Boot abren una cuenta, emiten una tarjeta y cobran la comisión de emisión con una saga orquestada y circuit breaker.

El proyecto se trabaja con **spec** y **skills**. `spec.md` es la fuente de verdad y `specs/apertura-cuenta-tarjeta-comision.md` describe esta historia. El skill `microservices-patterns` guía los patrones; sus ejemplos están en Python y aquí se implementan en Java. Si discrepan, manda el spec.

## Servicios

| Servicio | Puerto |
| --- | --- |
| account-opening-orchestrator-service | 8080 |
| account-service | 8081 |
| card-service | 8082 |

## Ejecución local

Hace falta Java 21. En esta máquina quedó instalado con Scoop en `C:\Users\fer\scoop\apps\temurin21-jdk\current`, y Scoop dejó esa ruta como `JAVA_HOME` de usuario.

```powershell
$env:JAVA_HOME = "C:\Users\fer\scoop\apps\temurin21-jdk\current"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
mvn test
```

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

El detalle de puertos, umbrales y compensaciones está en `docs/configuracion-tecnica.md` y `docs/c4-container.md`.
