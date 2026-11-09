---

## codigoVerificacion: CB-SAGA-9F3A

# Especificación base del proyecto

| Campo | Valor |
| --- | --- |
| Estado | Base aprobada. Servicios de negocio pendientes. |
| Versión | 0.1.0 |
| Fecha | 2026-11-09 |
| codigoVerificacion | `CB-SAGA-9F3A` |
| Forma de trabajo | Spec-Driven Development |

Este archivo es la fuente de verdad del proyecto. La regla `.cursor/rules/seguir-spec.mdc` obliga a leerlo en cada prompt. Un servicio concreto se añade en `specs/<servicio>.md` y se enlaza en [Servicios](#13-servicios). Hasta que ese archivo exista y sus criterios de aceptación estén completos, ese servicio no se implementa.

Si un prompt contradice este spec, el trabajo se detiene y se explica el conflicto. El cambio de una decisión se hace primero aquí.

## 1. Cómo se usa este spec

Orden de cada cambio:

1. Actualizar este archivo o el spec del servicio. Cada requisito lleva un id estable (`RF-…`, `RNF-…`, `DEC-…`, `CA-…`).
2. Completar la definición de listo de ese servicio: historia, pasos de la saga, clientes con circuit breaker, datos, errores y criterios de aceptación.
3. Implementar solo lo que esos ids cubren.
4. Nombrar las pruebas con el id que demuestran.
5. Marcar el requisito como implementado en el spec del servicio.

Estados de un spec de servicio: `borrador`, `listo`, `implementado`.

Este documento cumple el papel habitual de un `spec.md`: contexto, objetivos, fuera de alcance, actores, requisitos, criterios de aceptación, decisiones, contratos, datos, restricciones, atributos de calidad, estructura, pruebas, definición de hecho y preguntas abiertas.

## 2. Contexto

Se va a construir un proyecto Java con Spring Boot que expone servicios REST. Los procesos que cruzan varios servicios se coordinan con el patrón saga. Las llamadas de red hacia otra dependencia se protegen con circuit breaker y el resto de políticas de resiliencia definidas aquí.

Los servicios de negocio se entregarán después. Esta versión fija la base para que todos salgan con la misma forma.

El skill `microservices-patterns` (`.agents/skills/microservices-patterns/SKILL.md` y, cuando el caso lo exija, `references/details.md`) guía límites de servicio, comunicación, datos distribuidos, saga y resiliencia. Sus ejemplos están en Python. Este repositorio los implementa en Java. Si el skill y este spec discrepan, manda este spec.

## 3. Principios

1. Primero el spec, después el código.
2. Cada servicio posee su dominio y su base de datos.
3. Toda llamada de red saliente tiene timeout, circuit breaker y bulkhead.
4. Un proceso de varios servicios es una saga orquestada, con compensación en orden inverso.
5. Comandos y compensaciones son idempotentes.
6. El código de un caso de uso se encuentra en la carpeta de su feature.
7. Cada petición se puede seguir por `correlationId` y, si participa en una saga, por `sagaId`.

## 4. Objetivos

- Exponer APIs REST coherentes, validadas y documentadas con OpenAPI.
- Aislar el fallo de una dependencia para que no derribe al llamador.
- Completar o compensar una saga de forma explícita y repetible.
- Mantener cada feature legible: API, caso de uso, dominio e infraestructura en su paquete.
- Dejar los servicios futuros descritos con la plantilla de la sección 14 antes de codificarlos.

## 5. Fuera de alcance de la base

- Pantallas y clientes de interfaz.
- Los casos de negocio concretos, hasta que se añadan a `specs/`.
- Service mesh, service discovery y despliegue en Kubernetes.
- Event sourcing como modelo por defecto.
- Autenticación y autorización, hasta que un servicio las pida en su spec.
- Broker de mensajes, hasta que una saga asíncrona lo requiera en su spec.

## 6. Actores

| Actor | Interés |
| --- | --- |
| Cliente de la API | Invoca un caso de uso y recibe el resultado o el estado de la saga. |
| Servicio participante | Ejecuta un paso y su compensación. |
| Orquestador de la saga | Es el servicio dueño del proceso. Persiste el estado y llama a los participantes. |
| Operador | Observa circuitos, sagas detenidas y correlación de logs. |

## 7. Decisiones

| Id | Decisión |
| --- | --- |
| DEC-01 | Java 21. |
| DEC-02 | Spring Boot 4.1.2. Los parches de la línea 4.1 se aceptan. Subir de línea se registra en este spec. |
| DEC-03 | Maven multi-módulo, con wrapper `mvnw`. El padre vive en la raíz y no contiene lógica de negocio. |
| DEC-04 | Un módulo por servicio desplegable, más un módulo `platform` para el cruce técnico. |
| DEC-05 | Paquetes por feature. Dentro de cada feature: `api`, `application`, `domain`, `infrastructure`. |
| DEC-06 | Resiliencia con Resilience4j. El artefacto exacto del starter compatible con Spring Boot 4.1 se anota en esta fila al crear el primer `pom.xml`. |
| DEC-07 | Cliente HTTP: `RestClient`. Timeout de conexión y de respuesta obligatorios. |
| DEC-08 | Saga orquestada y, por defecto, síncrona sobre REST. El orquestador persiste el estado en su propia base. |
| DEC-09 | Cada servicio es dueño de su base. El producto de base de datos se elige en el spec del servicio. Recomendación inicial: PostgreSQL. |
| DEC-10 | El contrato entre servicios es OpenAPI. Cada llamador declara sus DTO en su infraestructura. El dominio no se comparte en un jar. |
| DEC-11 | Identificadores de código, paquetes y JSON en inglés. Este spec se escribe en español. |
| DEC-12 | `groupId` provisional: `com.pruebatecnica`. Se confirma al crear el build. |

## 8. Estructura del repositorio

```text
spec.md
specs/<servicio>.md
.cursor/rules/seguir-spec.mdc
.agents/skills/microservices-patterns/
pom.xml
platform/
services/<servicio>/
  src/main/java/com/pruebatecnica/<servicio>/<feature>/
    api/
    application/
    domain/
    infrastructure/
  src/main/resources/application.yml
  src/test/java/...
```

Reglas de dependencias dentro de un feature:

- `api` depende de `application`.
- `application` depende de `domain`.
- `infrastructure` implementa los puertos de `domain` y es llamado desde la composición de Spring.
- `domain` se queda en Java del modelo, sin Spring Web ni cliente HTTP.

`platform` solo contiene piezas técnicas reutilizables: filtro de correlación, `ProblemDetail`, abstracciones de saga y soporte de resiliencia. Los eventos de negocio y las entidades permanecen en su servicio.

Hasta que llegue el primer servicio no se generan módulos vacíos.

## 9. Servicios web

Prefijo `/api/v1`. Los recursos son sustantivos en plural. Una acción de negocio que no encaja como recurso se modela como subrecurso (`POST /api/v1/payments/{id}/refunds`).

| Situación | Respuesta |
| --- | --- |
| Creación síncrona terminada | `201` y cabecera `Location` |
| Saga aceptada y todavía en curso | `202` y cuerpo con `sagaId` y `status` |
| Lectura encontrada | `200` |
| Validación de entrada | `400` |
| Recurso ausente | `404` |
| Conflicto de idempotencia o de estado | `409` |
| Regla de negocio incumplida | `422` |
| Dependencia con circuito abierto o timeout agotado | `503` |

El cuerpo de error es `application/problem+json` (`ProblemDetail`), con `type`, `title`, `status`, `detail`, `instance` y `correlationId`. El `detail` describe el hecho para el cliente. La traza y el mensaje interno quedan en el log.

Toda petición lleva `X-Correlation-Id`. Si el cliente no lo envía, el filtro lo crea, lo guarda en el MDC y lo devuelve en la respuesta. Una saga añade `X-Saga-Id` cuando ya existe identificador.

Los `POST` que inician una saga o ejecutan un paso exigen `Idempotency-Key`. La misma clave con el mismo cuerpo devuelve el resultado ya guardado. La misma clave con otro cuerpo responde `409`.

La entrada se valida con Bean Validation en el borde `api`. Los records de request y response viven en `api` y no se sustituyen por entidades de persistencia.

OpenAPI se publica con springdoc en una versión compatible con Spring Boot 4.1, fijada al crear el `pom.xml`.

## 10. Circuit breaker y resiliencia

El circuit breaker se usa en el cliente de infraestructura que llama por red a otro servicio o a una API externa. Ahí evita que fallos repetidos saturen hilos, conexiones y al propio llamador.

La base de datos del propio servicio se protege con el pool, el timeout de consulta y el timeout de transacción.

Cada dependencia remota tiene su propia instancia de circuit breaker, timeout y bulkhead. Una instancia global para todo el proceso mezcla fallos que no tienen relación.

Política por defecto, ajustable en el spec del servicio:

| Política | Valor |
| --- | --- |
| Timeout de conexión | 500 ms |
| Timeout de respuesta | 2 s |
| Ventana | 20 llamadas, por conteo |
| Mínimo de llamadas antes de evaluar | 10 |
| Umbral de fallos | 50 % |
| Llamada lenta | a partir de 2 s, umbral 50 % |
| Espera en abierto | 30 s |
| Llamadas de prueba en semiabierto | 3 |
| Bulkhead | semáforo, 20 llamadas concurrentes |
| Reintentos | 2, solo si la operación es idempotente |

El resultado que ve el circuit breaker es el de la petición lógica completa. Los reintentos forman parte de esa petición y usan espera exponencial con jitter. Se reintenta ante timeout y ante `408`, `429`, `502`, `503` y `504`. Un `4xx` de negocio cierra la llamada como resultado funcional y no cuenta como fallo del circuito.

El fallback por defecto responde indisponibilidad controlada para que la saga compense. Un fallback que inventa un éxito de negocio queda fuera de esta base.

Ejemplo de forma, con los nombres reales del cliente:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentClient:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
  timelimiter:
    instances:
      paymentClient:
        timeout-duration: 2s
  bulkhead:
    instances:
      paymentClient:
        max-concurrent-calls: 20
```

Las métricas de Resilience4j y el endpoint de salud quedan expuestos por Actuator. La readiness del proceso representa si este servicio puede atender, y el estado del circuito de una dependencia se publica como métrica y alerta.

## 11. Saga

La saga la orquesta el servicio dueño del proceso de negocio. Los participantes ofrecen un comando y una compensación por su API. Cada paso persiste su resultado antes de pasar al siguiente.

Estados de la instancia: `STARTED`, `COMPLETED`, `COMPENSATING`, `FAILED`, `COMPENSATION_FAILED`.

| Dato de la instancia | Uso |
| --- | --- |
| `sagaId` | Identidad de la ejecución |
| `type` | Nombre del proceso |
| `status` | Estado de la tabla anterior |
| `currentStep` | Paso en curso |
| `payload` | Contexto mínimo para continuar o compensar |
| `correlationId` | Unión con la petición de origen |
| `version` | Control de concurrencia optimista |

Cada paso declara `name`, la acción, la compensación y el estado de ambas. Las compensaciones se ejecutan en orden inverso. Repetir una compensación ya aplicada es un éxito. Si una compensación falla, el estado queda en `COMPENSATION_FAILED`, se registra el error y la operación puede reintentarse. El proceso no se marca como compensado mientras ese paso siga pendiente.

Una saga cuyo spec la declare síncrona responde al terminar todos los pasos, dentro de los timeouts de sus clientes. Una saga declarada asíncrona responde `202` y ofrece una consulta de estado. El paso a broker se diseña en el spec de esa saga, con publicación confiable desde la base del orquestador.

La transacción local de un paso abarca su propia base. La saga es la unidad de consistencia entre servicios, y esa consistencia es eventual.

Forma esperada del paso:

```java
public interface SagaStep {
    String name();
    StepResult execute(SagaContext context);
    void compensate(SagaContext context);
}
```

El spec de cada proceso incluye esta tabla antes de implementarse:

| Paso | Acción | Compensación | Idempotente |
| --- | --- | --- | --- |
| 1 |  |  | sí |
| 2 |  |  | sí |

## 12. Java y Spring

- Inyección por constructor. Los campos de un bean no se inyectan con `@Autowired`.
- Records para requests, responses, comandos y resultados de paso.
- Bean Validation en el borde de la API.
- `@Transactional` en el caso de uso de aplicación, sobre la transacción de la base propia. Una clase que Spring deba proxeer por transacción permanece no `final`.
- El dominio expresa reglas con tipos y métodos. Las excepciones de negocio salen del dominio y el consejo de la API las traduce a `ProblemDetail`.
- SLF4J con logs estructurados. El log incluye `correlationId` y `sagaId` cuando existen. Quedan fuera del log secretos, tokens y datos que el spec del servicio marque como sensibles.
- Configuración en `application.yml` y variables de entorno. Los secretos no entran al repositorio.
- `Optional` se reserva para un retorno cuya ausencia es un resultado normal.
- El mapeo entre API, dominio y persistencia vive en el feature. Se hace a mano mientras el mapeo sea pequeño.
- Las pruebas de dominio y de saga se ejecutan sin levantar el contexto de Spring. `@WebMvcTest` cubre la API. WireMock cubre los clientes y el circuito. Testcontainers cubre la base cuando el servicio persiste.

Una prueba de arquitectura comprueba que `domain` no depende de Spring Web ni de los clientes, y que un feature no usa la infraestructura de otro feature.

## 13. Servicios

| Servicio | Spec | Estado |
| --- | --- | --- |
| — | — | Pendiente de que se proporcionen los servicios |

## 14. Plantilla de un servicio futuro

Copiar a `specs/<servicio>.md` y completar antes de crear el módulo.

```markdown
# <Servicio>

- Estado: borrador
- Dueño del proceso: sí | no
- Base de datos:

## Historia

Como <actor> quiero <capacidad> para <resultado>.

## Requisitos

| Id | Requisito |
| --- | --- |
| RF-<SVC>-01 | |

## Saga

| Paso | Acción | Compensación | Idempotente |
| --- | --- | --- | --- |
| 1 | | | sí |

## Clientes de red

| Cliente | Dependencia | Timeout | Circuit breaker | Fallback |
| --- | --- | --- | --- | --- |
| | | 2s | instancia propia | 503 y compensar |

## API

| Método | Ruta | Éxito | Error principal |
| --- | --- | --- | --- |
| POST | /api/v1/... | 201 o 202 | 400 / 409 / 422 / 503 |

## Datos propios

Tablas o documentos que este servicio posee.

## Criterios de aceptación

- CA-<SVC>-01. Dado _. Cuando _. Entonces _.
```

## 15. Requisitos de la base

| Id | Requisito |
| --- | --- |
| RF-BASE-01 | Todo servicio expone sus recursos bajo `/api/v1` y documenta el contrato con OpenAPI. |
| RF-BASE-02 | Toda llamada de red saliente usa timeout, circuit breaker y bulkhead propios de esa dependencia. |
| RF-BASE-03 | Un proceso entre servicios se implementa como saga orquestada con el estado y las compensaciones de la sección 11. |
| RF-BASE-04 | Los comandos y las compensaciones honran `Idempotency-Key`. |
| RF-BASE-05 | Los errores hacia el cliente usan `ProblemDetail` e incluyen `correlationId`. |
| RF-BASE-06 | El código de cada caso de uso queda dentro de su feature, con las cuatro capas de DEC-05. |
| RNF-BASE-01 | Una dependencia que supera el umbral abre el circuito y las llamadas siguientes fallan rápido. |
| RNF-BASE-02 | El build es reproducible con el Maven wrapper y Java 21. |
| RNF-BASE-03 | Los logs de una petición se pueden reunir por `correlationId`. |
| RNF-BASE-04 | La configuración sensible entra por el entorno y permanece fuera de git. |

## 16. Criterios de aceptación de la base

- CA-BASE-01. Dado un cliente con la política por defecto, cuando la dependencia acumula fallos por encima del 50 % en la ventana de 20 llamadas, entonces el circuito pasa a abierto y la llamada siguiente termina en `503` sin esperar el timeout de red.
- CA-BASE-02. Dado el circuito abierto, cuando transcurren 30 s, entonces un número limitado de llamadas de prueba puede cerrarlo de nuevo tras éxitos consecutivos.
- CA-BASE-03. Dada una saga de tres pasos, cuando el paso 3 falla, entonces se compensan el paso 2 y después el paso 1, y el estado final es `FAILED` con las compensaciones aplicadas.
- CA-BASE-04. Dada una compensación ya aplicada, cuando se repite, entonces el resultado es éxito y el efecto de negocio queda igual.
- CA-BASE-05. Dada una compensación que falla, cuando termina el intento, entonces el estado es `COMPENSATION_FAILED` y el error queda registrado con `sagaId`.
- CA-BASE-06. Dada la misma `Idempotency-Key` y el mismo cuerpo, cuando el comando se repite, entonces la respuesta es la ya almacenada.
- CA-BASE-07. Dada una petición sin `X-Correlation-Id`, cuando la API responde, entonces la respuesta incluye uno nuevo y el log lo contiene.

## 17. Definición de hecho

Un servicio está hecho cuando:

- su spec está en `implementado` y cada `RF` tiene prueba que cita su id;
- la saga del spec cubre fallo y compensación, incluida la compensación repetida;
- cada cliente de red tiene prueba del circuito abierto;
- OpenAPI refleja las rutas y los errores;
- `mvnw test` termina en verde;
- el spec y el código cuentan la misma historia.

## 18. Preguntas abiertas

| Id | Pregunta | Mientras tanto |
| --- | --- | --- |
| PA-01 | Nombre de producto y `groupId` definitivos | `com.pruebatecnica` |
| PA-02 | Motor de base de cada servicio | PostgreSQL cuando haga falta persistir |
| PA-03 | ¿Alguna saga será asíncrona y con qué broker? | REST síncrono según DEC-08 |
| PA-04 | ¿Qué autenticación piden los servicios? | La base sale sin seguridad de usuario |
| PA-05 | Lista de servicios y sus procesos | Sección 13 vacía a propósito |

## 19. Verificación

Para comprobar que un prompt está usando este archivo, en un chat nuevo de esta carpeta pregunta: «¿Cuál es el codigoVerificacion del spec?». La respuesta esperada es `CB-SAGA-9F3A`.
