// APF2 - Seccion 12 - Prueba de ESTRES sobre la API de CampusLion
// Sube la carga por encima de la demanda prevista (50 VU) hasta 400 VU
// para encontrar el punto de degradacion y comprobar la recuperacion.
// Ejecutar: k6 run estres.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Trend, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api/courses';

// Etapas (duraciones en segundos) y etiqueta de cada tramo
const ETAPAS = [
  { d: 30, target: 50, etiqueta: '1_subida_50' },
  { d: 60, target: 50, etiqueta: '2_meseta_50' },
  { d: 30, target: 100, etiqueta: '3_subida_100' },
  { d: 60, target: 100, etiqueta: '4_meseta_100' },
  { d: 30, target: 200, etiqueta: '5_subida_200' },
  { d: 60, target: 200, etiqueta: '6_meseta_200' },
  { d: 30, target: 400, etiqueta: '7_subida_400' },
  { d: 60, target: 400, etiqueta: '8_meseta_400' },
  { d: 30, target: 0, etiqueta: '9_recuperacion' },
];

const duracionEtapa = new Trend('duracion_por_etapa', true);
const errorEtapa = new Rate('error_por_etapa');

const thresholds = {
  // Umbrales globales: en estres se espera que alguno falle; eso es el hallazgo
  http_req_duration: ['p(95)<200'],
  http_req_failed: ['rate<0.01'],
};
// Un umbral por etapa hace que k6 muestre las metricas de cada tramo
for (const e of ETAPAS) {
  thresholds[`duracion_por_etapa{etapa:${e.etiqueta}}`] = ['p(95)<200'];
  thresholds[`error_por_etapa{etapa:${e.etiqueta}}`] = ['rate<0.01'];
}

export const options = {
  stages: ETAPAS.map((e) => ({ duration: `${e.d}s`, target: e.target })),
  thresholds,
};

function etapaActual() {
  let t = exec.instance.currentTestRunDuration / 1000;
  for (const e of ETAPAS) {
    if (t < e.d) return e.etiqueta;
    t -= e.d;
  }
  return ETAPAS[ETAPAS.length - 1].etiqueta;
}

export default function () {
  const etapa = etapaActual();
  const res = http.get(`${BASE}?page=0&pageSize=10`, { tags: { etapa } });
  const ok = check(res, { 'status 200': (x) => x.status === 200 });
  duracionEtapa.add(res.timings.duration, { etapa });
  errorEtapa.add(!ok, { etapa });
  sleep(0.5);
}

export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'resultados/estres_resumen.txt': textSummary(data, { indent: ' ', enableColors: false }),
  };
}
