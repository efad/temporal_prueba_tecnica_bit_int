# Apertura de cuenta con tarjeta y comisión

- Estado: implementado
- Historia de origen: `docs/HU-BACK-banco-apertura-cuenta-tarjeta-comision-saga.html`
- Dueño del proceso: `account-opening-orchestrator-service`
- Base de datos: H2 en memoria, una por servicio

## Historia

Como cliente del banco quiero abrir una cuenta con tarjeta y pagar la comisión de emisión para quedar con los tres efectos consistentes, o con ninguno si un paso falla.

## Servicios

| Servicio | Puerto | Papel |
| --- | --- | --- |
| account-opening-orchestrator-service | 8080 | Expone la apertura y orquesta la saga. Solo llama a account-service y card-service. |
| account-service | 8081 | Crea y revierte la cuenta. Cobra y devuelve la comisión. |
| card-service | 8082 | Crea y revierte la tarjeta. Pide el cobro de la comisión a account-service. |

No hay autorización de usuario. No hay bus de eventos ni coreografía.

## Saga

| Paso | Acción | Compensación | Idempotente |
| --- | --- | --- | --- |
| 1 | `POST /api/v1/accounts` | `POST /api/v1/accounts/{id}/reversals` | sí |
| 2 | `POST /api/v1/cards` | `POST /api/v1/cards/{id}/reversals` | sí |
| 3 | `POST /api/v1/cards/{id}/commission-charges`, que debita la cuenta | El reverso de la tarjeta devuelve la comisión si ya se cobró | sí |

Si el circuito está abierto o falla el paso 1 antes de existir la cuenta, la saga queda en `PENDING_RETRY`. La misma `Idempotency-Key` y el mismo cuerpo la reanudan. Si falla la tarjeta o la comisión después de crear la cuenta, se compensa primero la tarjeta y después la cuenta. El estado queda en `FAILED`. Si una compensación falla, queda en `COMPENSATION_FAILED` y se intenta el resto.

## Valores que completa el orquestador

La petición pública trae `customerId`, `productType`, `accountAlias` y `cardType`. El orquestador deriva:

| Dato | Valor |
| --- | --- |
| accountType | el `productType` (`SAVINGS` o `CHECKING`) |
| currency | `COP` |
| initialDeposit | `0.00` |
| commissionAmount | `15000.00` para `DEBIT` y `35000.00` para `CREDIT` |
| alias | `accountAlias`, guardado en la cuenta |

## Clientes de red

| Cliente | Dependencia | Timeout de lectura | Circuit breaker | Fallback |
| --- | --- | --- | --- | --- |
| accountService | account-service | 5 s | instancia propia | no continúa la saga |
| cardService | card-service | 5 s | instancia propia | no continúa la saga |
| accountService dentro de card-service | account-service | 5 s | instancia propia | la comisión falla y la saga compensa |

El timeout de conexión es 500 ms. El umbral de llamada lenta es 1 s, con ventana de 4 llamadas, mínimo 4, 50 % de lentitud o de fallos, 15 s en abierto y 2 llamadas de prueba en semiabierto. El bulkhead admite 20 llamadas concurrentes. Los reintentos automáticos están apagados: la latencia del escenario se mide por petición y la `Idempotency-Key` cubre el reintento del cliente.

Una llamada de 2 s termina bien y cuenta como lenta. El circuito se abre por lentitud sostenida, no solo por errores.

## API del orquestador

| Método | Ruta | Éxito | Error principal |
| --- | --- | --- | --- |
| POST | `/api/v1/account-openings` | 201 `COMPLETED` | 422 `FAILED`, 503 `PENDING_RETRY`, 500 `COMPENSATION_FAILED` |
| GET | `/api/v1/account-openings/{sagaId}` | 200 con pasos, compensaciones y estado de los circuitos | 404 |

La respuesta incluye `sagaId`, `accountId`, `cardId`, `sagaStatus` y `failureReason`. Exige `Idempotency-Key`.

## Simulación local

Con `app.simulation.enabled=true`:

| Cabecera | Efecto |
| --- | --- |
| `X-Delay-Milliseconds` | El servicio que recibe la llamada espera esos milisegundos, con tope de 10 s. El orquestador reenvía la cabecera. |
| `X-Force-Failure` | `CARD_CREATE` falla la creación de tarjeta. `CARD_COMMISSION` falla el cobro. `ACCOUNT_CREATE` falla la cuenta. |

## Criterios de aceptación

- CA-01. Los tres servicios existen y el orquestador solo completa la apertura llamando a account-service y card-service.
- CA-02. La saga es orquestación síncrona sobre REST.
- CA-03. El fallo de tarjeta o de comisión compensa los recursos ya creados.
- CA-04. La latencia sostenida abre el circuito, la llamada siguiente no sale a la red y, tras la espera, el semiabierto puede cerrarlo.
- CA-05. `mvn test` cubre la saga en unitarias y una integración mínima.
- CA-06. `client/apertura-cuenta.http` y `client/apertura-cuenta.postman_collection.json` recorren éxito, fallo de tarjeta y latencia.
- CA-07. El contenedor C4 está en `docs/c4-container.md` y la configuración en `docs/configuracion-tecnica.md`.
