# APF2 — Mejoras de evidencia (secciones 11 y 12)

Requisito previo: backend iniciado **desde cero** (sin reinicio en caliente, ver INC-01):

```
cd crud-spring
mvnw spring-boot:run
```

## 1. Pruebas funcionales de API con Postman (sección 11)

1. Postman → Import → `evidencias/postman/CampusLion_APF2.postman_collection.json`.
2. Clic derecho en la colección → **Run collection** → ejecutar en orden (18 solicitudes, 40+ aserciones).
3. Guardar evidencia: captura del Runner con el resumen y **Export Results** como `evidencias/postman/resultados_runner.json`.

Cubre casos positivos, negativos, límites, reglas de negocio (RF-01 a RF-06) y dos controles de seguridad (INC-03, INC-06).

## 2. Rendimiento con k6 (sección 12)

Instalar k6 en Windows: `winget install k6 --source winget` (o `choco install k6`).

```
cd evidencias/k6
k6 run carga.js        # 50 VU, ramp-up 30 s, 2 min sostenido, mezcla de lecturas
k6 run estres.js       # 50 → 100 → 200 → 400 VU y recuperación (≈ 7 min)
k6 run escritura.js    # 20 VU creando cursos (POST) durante 1 min
```

Los resúmenes quedan en `evidencias/k6/resultados/`. Reiniciar el backend entre `escritura.js` y las demás pruebas.

| Script | Tipo | Umbrales |
|---|---|---|
| carga.js | Carga (demanda prevista) | p95 < 200 ms, errores < 1 %, checks > 99 % |
| estres.js | Estrés (por encima del pico) | Mismos umbrales por etapa; se espera que fallen al saturar |
| escritura.js | Carga de escritura | p95 POST < 500 ms, errores < 1 % |

## 3. Sesión exploratoria y ad hoc (sección 11)

Completar `evidencias/exploratoria/SESION_EXPLORATORIA_01.md` durante la sesión, con capturas en la misma carpeta.
