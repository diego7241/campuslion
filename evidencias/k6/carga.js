// APF2 - Seccion 12 - Prueba de CARGA sobre la API de CampusLion
// Demanda prevista: 50 usuarios concurrentes consultando cursos.
// Ejecutar (con el backend levantado en localhost:8080):
//   k6 run --summary-export=resultados/carga_summary.json carga.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api/courses';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // ramp-up
    { duration: '2m', target: 50 },   // carga sostenida
    { duration: '30s', target: 0 },   // ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'],          // criterio RNF-01
    http_req_failed: ['rate<0.01'],            // errores < 1 %
    checks: ['rate>0.99'],
    'http_req_duration{endpoint:listado}': ['p(95)<200'],
    'http_req_duration{endpoint:por_id}': ['p(95)<200'],
    'http_req_duration{endpoint:busqueda}': ['p(95)<200'],
  },
};

export default function () {
  // Mezcla de operaciones: 50 % listado, 30 % por id, 20 % busqueda
  const r = Math.random();
  let res;
  if (r < 0.5) {
    res = http.get(`${BASE}?page=0&pageSize=10`, { tags: { endpoint: 'listado' } });
    check(res, {
      'listado 200': (x) => x.status === 200,
      'listado trae cursos': (x) => Array.isArray(x.json('courses')),
    });
  } else if (r < 0.8) {
    const id = Math.floor(Math.random() * 4) + 1; // 4 cursos semilla
    res = http.get(`${BASE}/${id}`, { tags: { endpoint: 'por_id' } });
    check(res, { 'por id 200': (x) => x.status === 200 });
  } else {
    res = http.get(`${BASE}/searchByName?name=Course`, { tags: { endpoint: 'busqueda' } });
    check(res, { 'busqueda 200': (x) => x.status === 200 });
  }
  sleep(1); // think time
}

export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'resultados/carga_resumen.txt': textSummary(data, { indent: ' ', enableColors: false }),
  };
}
