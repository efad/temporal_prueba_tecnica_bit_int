# Prueba técnica

Proyecto Java con Spring Boot para exponer servicios REST con circuit breaker y saga. La aplicación API se crea en un paso posterior. Hoy el repositorio fija la forma de trabajo y las decisiones de base.

El proyecto está proyectado para trabajar con **skills** y **spec**.

## Spec

`spec.md` es la fuente de verdad. Ahí viven el stack, la estructura por features, el uso del circuit breaker, la saga y los criterios de aceptación.

Cada cambio sigue Spec-Driven Development:

1. Actualizar `spec.md` o `specs/<servicio>.md`.
2. Implementar solo lo que ese spec ya acepta.
3. Enlazar las pruebas con los ids del requisito.

La regla `.cursor/rules/seguir-spec.mdc` pide leer el spec en cada prompt. Si un pedido lo contradice, se detiene el cambio y se corrige primero el spec.

## Skills

Los skills de agente viven en `.agents/skills/` y acompañan al repositorio. El instalado es `microservices-patterns`: límites de servicio, comunicación, datos distribuidos, saga y resiliencia.

Sus ejemplos están en Python. La implementación de este proyecto es Java, como fija el spec. Si el skill y el spec discrepan, manda el spec.

El inventario de lo instalado está en `skills-lock.json`.

## Estado

| Pieza | Estado |
| --- | --- |
| Spec base y regla de Cursor | Listos |
| Skill `microservices-patterns` | Instalado |
| Aplicación API Spring Boot | Pendiente del siguiente paso |
| Servicios de negocio | Pendientes de su spec en `specs/` |

## Stack previsto

Java 21, Spring Boot 4.1.2 y Maven multi-módulo. El detalle y las decisiones cerradas están en `spec.md`.
