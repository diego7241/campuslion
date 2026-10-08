"""APF2 - Prueba de rendimiento basica sobre la API de CampusLion (http://localhost:8080)."""
import statistics
import time
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime

BASE = "http://localhost:8080/api/courses"
TOTAL_REQUESTS = 300
SCENARIOS = [
    ("GET listado paginado", "GET", f"{BASE}?page=0&pageSize=10"),
    ("GET curso por id", "GET", f"{BASE}/1"),
    ("GET busqueda por nombre", "GET", f"{BASE}/searchByName?name=curso"),
]
CONCURRENCY_LEVELS = [1, 10, 50]


def one_request(method, url):
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(urllib.request.Request(url, method=method), timeout=30) as resp:
            resp.read()
            status = resp.status
    except urllib.error.HTTPError as err:
        status = err.code
    except Exception:
        status = 0
    return (time.perf_counter() - start) * 1000, status


def run(method, url, concurrency):
    started = time.perf_counter()
    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        results = list(pool.map(lambda _: one_request(method, url), range(TOTAL_REQUESTS)))
    elapsed = time.perf_counter() - started
    times = sorted(t for t, _ in results)
    errors = sum(1 for _, s in results if s != 200)
    p95 = times[int(len(times) * 0.95) - 1]
    return {
        "mean": statistics.mean(times), "median": statistics.median(times),
        "p95": p95, "max": times[-1], "rps": TOTAL_REQUESTS / elapsed, "errors": errors,
    }


lines = [f"APF2 - Rendimiento basico CampusLion - {datetime.now():%Y-%m-%d %H:%M}",
         f"{TOTAL_REQUESTS} peticiones por escenario. Tiempos en milisegundos.", ""]
header = f"{'Escenario':<26}{'Concurr.':>9}{'Media':>9}{'Mediana':>9}{'P95':>9}{'Max':>9}{'Req/s':>9}{'Errores':>9}"
lines += [header, "-" * len(header)]
for name, method, url in SCENARIOS:
    one_request(method, url)  # calentamiento
    for level in CONCURRENCY_LEVELS:
        r = run(method, url, level)
        lines.append(f"{name:<26}{level:>9}{r['mean']:>9.1f}{r['median']:>9.1f}{r['p95']:>9.1f}"
                     f"{r['max']:>9.1f}{r['rps']:>9.1f}{r['errors']:>9}")

output = "\n".join(lines)
print(output)
with open(r"C:\Users\PC\Desktop\evidencias\APF2_rendimiento.txt", "w", encoding="utf-8") as f:
    f.write(output + "\n")
