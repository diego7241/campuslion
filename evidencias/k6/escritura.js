// APF2 - Seccion 12 - Carga de ESCRITURA (POST /api/courses)
// 20 usuarios creando cursos durante 1 minuto. Cada nombre es unico.
// Ejecutar: k6 run escritura.js   (usar una base recien iniciada)
import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api/courses';
const HEADERS = { headers: { 'Content-Type': 'application/json' } };

export const options = {
  stages: [
    { duration: '15s', target: 20 },
    { duration: '1m', target: 20 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    'http_req_duration{operacion:crear}': ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const body = JSON.stringify({
    name: `Carga Escritura Usuario ${__VU} Iteracion ${__ITER}`,
    category: 'Back-end',
    lessons: [{ name: 'Leccion de carga', youtubeUrl: 'Fj3Zvf-N4bk' }],
  });
  const res = http.post(BASE, body, { ...HEADERS, tags: { operacion: 'crear' } });
  check(res, { 'creado 201': (x) => x.status === 201 });
  sleep(1);
}

export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'resultados/escritura_resumen.txt': textSummary(data, { indent: ' ', enableColors: false }),
  };
}
