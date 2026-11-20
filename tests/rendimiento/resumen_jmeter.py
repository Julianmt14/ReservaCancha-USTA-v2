"""Resume el resultado de rnf01.jmx (TEC-04, #16) para el README: tiempos por consulta y si cumple RNF-01 (< 2,0 s).

Uso: python tests/rendimiento/resumen_jmeter.py resultados.jtl
"""
import csv
import statistics
import sys

UMBRAL = 2000  # ms; 2,0 s exactos no cumple

filas = list(csv.DictReader(open(sys.argv[1], encoding="utf-8")))
grupos = {}
for f in filas:
    grupos.setdefault(f["label"], []).append(f)


def seg(ms):
    return f"{ms / 1000:.3f}".replace(".", ",") + " s"


print("| Consulta | Peticiones | Promedio | Percentil 90 | Máximo | Errores | ¿Cumple? |")
print("|---|---|---|---|---|---|---|")
for etiqueta in sorted(grupos):
    t = sorted(int(f["elapsed"]) for f in grupos[etiqueta])
    errores = sum(1 for f in grupos[etiqueta] if f["success"] != "true" and f["responseCode"] != "200")
    p90 = t[max(0, int(round(0.9 * len(t))) - 1)]
    medida = etiqueta.startswith("1") or etiqueta.startswith("2")
    cumple = ("sí" if t[-1] < UMBRAL and not errores else "no") if medida else "se reporta aparte"
    print(f"| {etiqueta} | {len(t)} | {seg(statistics.mean(t))} | {seg(p90)} | {seg(t[-1])} | {errores} | {cumple} |")
