"""APF2 - Pruebas de seguridad basicas sobre CampusLion (app local, entorno propio)."""
import json
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime

HOST = "http://localhost:8080"
API = HOST + "/api/courses"
rows = []


def call(method, url, body=None, headers=None):
    data = body.encode("utf-8") if isinstance(body, str) else body
    req = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, dict(resp.headers), resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as err:
        return err.code, dict(err.headers), err.read().decode("utf-8", "replace")


def course(name, extra=None):
    payload = {"name": name, "category": "Back-end",
               "lessons": [{"name": "Leccion de prueba", "youtubeUrl": "dQw4w9WgXcQ"}]}
    payload.update(extra or {})
    return json.dumps(payload)


JSON = {"Content-Type": "application/json"}
LEAK = re.compile(r"(\bat [a-z]+\.[\w.]+\(|Exception in|java\.lang\.|org\.springframework|org\.hibernate|stacktrace)", re.I)


def record(prueba, esperado, obtenido, ok, na=False):
    rows.append((prueba, esperado, obtenido, "N/A (HTTP)" if na else ("CUMPLE" if ok else "HALLAZGO")))


def count_courses():
    status, _, body = call("GET", API + "?page=0&pageSize=100")
    return json.loads(body)["totalElements"] if status == 200 else -1


baseline = count_courses()

# 1. Autenticacion
s, _, b = call("GET", API)
record("S01 GET sin credenciales", "401/403", f"{s}", s in (401, 403))
s, _, b = call("POST", API, course("Curso Seguridad Temporal"), JSON)
temp_id = re.search(r'"_id"\s*:\s*(\d+)', b)
record("S02 POST sin credenciales", "401/403", f"{s}", s in (401, 403))
if temp_id:
    s, _, _ = call("DELETE", f"{API}/{temp_id.group(1)}")
    record("S03 DELETE sin credenciales", "401/403", f"{s}", s in (401, 403))

# 2. Inyeccion SQL
for i, payload in enumerate(["' OR '1'='1", "x'; DROP TABLE course;--", "%' UNION SELECT * FROM course--"], 4):
    s, _, b = call("GET", f"{API}/searchByName?name={urllib.parse.quote(payload)}")
    try:
        n = len(json.loads(b)) if s == 200 else 0
    except ValueError:
        n = 0
    record(f"S{i:02d} SQLi en busqueda: {payload[:28]}", "sin resultados / sin error 500", f"{s}, {n} resultados", s != 500 and n == 0)
s, _, b = call("POST", API, course("x'; DROP TABLE course;--"), JSON)
record("S07 SQLi en nombre de curso", "400 (rechazado)", f"{s}", s == 400)
record("S08 Integridad tras SQLi (total de cursos)", f"{baseline}", f"{count_courses()}", count_courses() == baseline)

# 3. XSS
s, _, b = call("POST", API, course("<script>alert(1)</script>"), JSON)
record("S09 XSS en nombre de curso", "400 (rechazado)", f"{s}", s == 400)
s, _, b = call("POST", API, course("Curso XSS", {"lessons": [{"name": "<img src=x onerror=alert(1)>", "youtubeUrl": "dQw4w9WgXcQ"}]}), JSON)
if s == 201:
    new = re.search(r'"_id"\s*:\s*(\d+)', b)
    if new:
        call("DELETE", f"{API}/{new.group(1)}")
record("S10 XSS en nombre de leccion", "400 (rechazado)", f"{s}", s == 400)

# 4. Payloads anomalos
s, _, b = call("POST", API, course("A" * 200000), JSON)
record("S11 Nombre de 200.000 caracteres", "400", f"{s}", s == 400)
s, _, b = call("POST", API, "{no es json", JSON)
record("S12 JSON malformado", "400 sin filtrar detalles internos", f"{s}, fuga={bool(LEAK.search(b))}", s == 400 and not LEAK.search(b))
s, _, b = call("GET", API + "/abc")
record("S13 Id no numerico", "400 sin filtrar detalles internos", f"{s}, fuga={bool(LEAK.search(b))}", s == 400 and not LEAK.search(b))
s, _, b = call("GET", API + "/-1")
record("S14 Id negativo", "400/404", f"{s}", s in (400, 404))
s, _, b = call("GET", API + "/99999")
record("S15 Id inexistente: sin fuga de datos internos", "404 sin stacktrace", f"{s}, fuga={bool(LEAK.search(b))}", s == 404 and not LEAK.search(b))

# 5. Asignacion masiva de campos
s, _, b = call("POST", API, course("Curso Mass Assignment", {"_id": 9999, "status": "Inactive"}), JSON)
new = re.search(r'"_id"\s*:\s*(\d+)', b)
ignored = bool(new) and new.group(1) != "9999"
if new:
    call("DELETE", f"{API}/{new.group(1)}")
record("S16 Campos _id/status inyectados en el JSON", "ignorados por el servidor", f"{s}, id asignado={new.group(1) if new else '-'}", s == 201 and ignored)

# 6. Endpoints expuestos
for i, (path, why) in enumerate([("/h2-console", "consola de base de datos"), ("/swagger-ui.html", "documentacion Swagger"),
                                 ("/v3/api-docs", "esquema OpenAPI"), ("/actuator", "indice de actuator"),
                                 ("/actuator/env", "variables de entorno"), ("/actuator/beans", "beans de Spring")], 17):
    s, _, _ = call("GET", HOST + path)
    record(f"S{i} Exposicion de {path} ({why})", "404/401/403", f"{s}", s in (401, 403, 404))

# 7. Cabeceras de seguridad y CORS
s, h, _ = call("GET", API)
lower = {k.lower(): v for k, v in h.items()}
for i, header in enumerate(["x-content-type-options", "x-frame-options", "strict-transport-security", "content-security-policy"], 23):
    record(f"S{i} Cabecera {header}", "presente", lower.get(header, "AUSENTE"), header in lower,
           na=(header == "strict-transport-security" and HOST.startswith("http://")))
s, h, _ = call("GET", API, headers={"Origin": "http://sitio-malicioso.example"})
lower = {k.lower(): v for k, v in h.items()}
record("S27 CORS con origen no autorizado", "sin Access-Control-Allow-Origin", lower.get("access-control-allow-origin", "ausente"), "access-control-allow-origin" not in lower)

width = max(len(r[0]) for r in rows)
out = [f"APF2 - Seguridad basica CampusLion - {datetime.now():%Y-%m-%d %H:%M}", ""]
out.append(f"{'Prueba':<{width}}  {'Esperado':<34}{'Obtenido':<30}Veredicto")
out.append("-" * (width + 80))
for p, e, o, v in rows:
    out.append(f"{p:<{width}}  {e:<34}{o[:28]:<30}{v}")
cumple = sum(1 for r in rows if r[3] == "CUMPLE")
na = sum(1 for r in rows if r[3].startswith("N/A"))
out += ["", f"Total: {len(rows)} pruebas | CUMPLE: {cumple} | HALLAZGO: {len(rows) - cumple - na} | N/A: {na}"]
text = "\n".join(out)
print(text)
label = sys.argv[1] if len(sys.argv) > 1 else "ACTUAL"
with open(rf"C:\Users\PC\Desktop\evidencias\APF2_seguridad_{label}.txt", "w", encoding="utf-8") as f:
    f.write(text + "\n")
