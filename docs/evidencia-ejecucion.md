# Evidencia de pruebas

Comando: `mvn test` con Java 21.0.13.

Resultado del 2026-11-09: 15 pruebas, 0 fallos.

| Módulo | Pruebas | Qué cubre |
| --- | --- | --- |
| account-service | 5 | Reverso idempotente, idempotencia, conflicto de clave y rechazo de cobro sobre cuenta revertida. El dominio no depende de web ni de JPA. |
| account-opening-orchestrator-service | 6 unitarias | Éxito, fallo de tarjeta, fallo de comisión, compensación fallida, circuito abierto con reintento y conflicto de idempotencia. |
| account-opening-orchestrator-service | 4 de integración | Éxito HTTP, compensación real contra los stubs, apertura del circuito por latencia y recuperación en semiabierto. |

La integración usa el orquestador Spring Boot y servidores HTTP locales que hacen de account-service y card-service.
