# Registro de incidencias — CampusLion (APF2)

Entorno: backend Spring Boot 4 / Java 25, base H2 en memoria, perfil `test`. Pruebas ejecutadas el 2026-10-07.
Evidencia: `APF2_seguridad_ANTES.txt` y `APF2_seguridad_DESPUES.txt` (27 pruebas), `APF2_regresion_completa.log` (94 tests).

| ID | Incidencia | Cómo se detectó | Severidad | Causa | Corrección | Verificación | Estado |
|---|---|---|---|---|---|---|---|
| INC-01 | Tras reiniciar el backend en caliente con Spring Boot DevTools, PUT y DELETE devuelven 409 aunque los datos sean válidos | Pruebas exploratorias desde la UI y directo contra la API | Media (solo desarrollo) | Causa probable: el reinicio en caliente deja el contexto de persistencia inconsistente. No se diagnosticó a fondo; el síntoma desaparece con un reinicio completo | Reiniciar el proceso por completo | PUT devolvió 200 tras el reinicio completo | Corregida (mitigación operativa) |
| INC-02 | La API no exige autenticación: GET, POST y DELETE funcionan sin credenciales | S01–S03 | Alta si se despliega | El proyecto base no implementa seguridad (el README original lo declara) | Pendiente: incorporar Spring Security con roles | — | Abierta (fuera de alcance de esta fase) |
| INC-03 | El nombre de una lección acepta HTML (`<img src=x onerror=...>`); solo se validaba el nombre del curso | S10 | Media | `LessonDTO.name` solo validaba longitud | `@Pattern` en `LessonDTO.name` que permite letras, números, espacios y puntuación común | S10 pasó de 201 a 400; 2 tests de regresión | Corregida |
| INC-04 | La consola web de H2 (`/h2-console`) era accesible | S17 | Alta en producción | DevTools la habilita por defecto | `spring.h2.console.enabled=false` | S17 pasó de 200 a 404; test de regresión | Corregida |
| INC-05 | Swagger UI, `/v3/api-docs` y el índice de `/actuator` son públicos | S18–S20 | Baja | Configuración de documentación pensada para desarrollo | Aceptada en desarrollo; para producción deshabilitar las propiedades `springdoc.*` | — | Abierta (riesgo aceptado) |
| INC-06 | Las respuestas no incluían cabeceras de seguridad | S23, S24, S26 | Media | No hay Spring Security que las agregue | `SecurityHeadersFilter`: `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` y CSP en `/api/**` | S23, S24 y S26 pasaron a CUMPLE; test de regresión | Corregida |

Nota: `Strict-Transport-Security` (S25) se marca N/A porque solo aplica sobre HTTPS y el entorno de prueba usa HTTP.

Limitación de la corrección INC-03: el patrón rechaza caracteres como comillas, `<`, `>`, `=` y `/` en el nombre de lección.

## Resumen antes / después

| | Antes | Después |
|---|---|---|
| Pruebas de seguridad que cumplen | 15 de 27 | 20 de 27 |
| Hallazgos abiertos | 12 | 6 (INC-02 e INC-05) |
| No aplica | 0 | 1 (HSTS sobre HTTP) |
