# Evidencias APF3 — CampusLion

Pruebas del frontend Angular (sección 14 y 15 del informe). Backend sin cambios desde APF2.

| Carpeta / archivo | Casos | Informe |
|---|---|---|
| `capturas/pc/` | PC-01 a PC-04 (Chrome escritorio) | §14.1, Anexo G |
| `capturas/responsive/` | MOV-01, MOV-03, MOV-04.1–04.4, MOV-05–07 (DevTools 390 × 844) | §14.1, Anexo G |
| `capturas/usabilidad/`, `usabilidad_resumen.md` | USA-01 a USA-04, P01–P03 | §14.2, Anexo H |
| `capturas/accesibilidad/` | ACC-01 Lighthouse 13.5.0 (94/100), ACC-02 Tab, ACC-03 zoom 200 % | §14.3, Anexo I |
| `capturas/regresion/` | REG-01 a REG-06, después de INC-07 | §15.1, Anexo J |
| `frontend_tests_ANTES_fix.txt` | `ng test`: 55/65, 10 fallidas (INC-08) | §15.2, Anexo K |
| `frontend_tests_DESPUES_fix.txt` | `ng test`: 65/65 | §15.2, Anexo K |

Correcciones: commit `2d1137c` (INC-07 e INC-08). Repetir la regresión del frontend:

```
cd crud-angular
npx ng test --watch=false --include "src/app/**/*.spec.ts"
```
