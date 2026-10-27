# Sesión exploratoria EXP-01 — Formulario de cursos (CampusLion)

| Campo | Detalle |
|---|---|
| Charter | Explorar el formulario de creación y edición de cursos para descubrir errores en validaciones, navegación y mensajes. |
| Timebox | 20 minutos (inicio: __:__ — fin: __:__) |
| Tester | ______________________ |
| Fecha | __/__/2026 |
| Entorno | Windows 11, navegador ________ v____, frontend Angular (localhost:4200), backend Spring Boot (localhost:8080), H2 |
| Versión | commit ________ |

## Heurísticas a aplicar

- **Datos y límites:** campos vacíos, solo espacios, nombre de 4/5/150/151 caracteres, caracteres especiales, nombre duplicado, URL de YouTube de 10/11/12 caracteres.
- **Flujo y comportamiento:** cancelar a mitad, volver atrás, refrescar con el formulario lleno, doble clic en Guardar, agregar y quitar lecciones varias veces, editar un curso recién eliminado en otra pestaña.

## Registro

| ID | Acción realizada | Dato usado | Resultado esperado | Resultado observado | Evidencia | ¿Hallazgo? |
|---|---|---|---|---|---|---|
| EXP-01-01 | | | | | captura_01.png | |
| EXP-01-02 | | | | | | |
| EXP-01-03 | | | | | | |
| EXP-01-04 | | | | | | |
| EXP-01-05 | | | | | | |

## Debrief

- **Qué se cubrió:**
- **Qué se descubrió (enlazar a INC-xx):**
- **Dudas y qué investigar después:**
- **Qué conviene automatizar ahora (flujo estable):**

---

# Investigación ad hoc AH-01 — Fallo de PUT/DELETE tras reinicio en caliente (INC-01)

| Campo | Detalle |
|---|---|
| Objetivo | Reproducir el 409 en PUT/DELETE tras un reinicio en caliente con DevTools (10 minutos). |
| Acciones | Recompilar con el backend corriendo; PUT /api/courses/1 válido; repetir tras 2–3 reinicios; probar DELETE; reiniciar en frío y repetir. |
| Pasos mínimos para reproducir | |
| Resultado esperado | 200 / 204 |
| Resultado observado | |
| Evidencia | log del backend + captura de Postman |
