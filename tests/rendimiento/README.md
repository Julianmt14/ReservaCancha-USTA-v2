# Medición de RNF-01 (eficiencia de desempeño)

Ficha RNF-01 del acta: las consultas habituales de disponibilidad y reservas responden en **menos de 2,0 s**.
Este script mide tres consultas con 10 peticiones cada una, después de una de calentamiento. Crea un jugador de
prueba con correo `*@rnf01.test`.

```bash
cd backend && mvn spring-boot:run      # en otra terminal
python tests/rendimiento/medir_rnf01.py            # o: python tests/rendimiento/medir_rnf01.py https://mi-servidor
```

| Consulta | Promedio | Máximo |
|---|---|---|
| Disponibilidad (`GET /api/disponibilidad`) | 0,022 s | 0,031 s |
| Mis reservas (`GET /api/reservas/mias`) | 0,012 s | 0,013 s |
| Ocupación de la cancha (`GET /api/reservas/cancha/{id}`) | 0,011 s | 0,016 s |

Medición del 8 de octubre de 2026 en local, con H2 en memoria y los datos semilla. Cumple el umbral.

## TEC-04 (#16): JMeter en la dirección pública

`rnf01.jmx` simula 20 jugadores a la vez. Cada uno consulta 10 veces la disponibilidad y sus reservas en
https://reservacancha-api.onrender.com. Antes, en un grupo de preparación, mide aparte la primera petición y registra un
jugador de prueba (`jmeter-*@rnf01.test`). Una petición de 2,0 s o más falla la verificación.

```bash
jmeter -n -t tests/rendimiento/rnf01.jmx -l resultados.jtl -e -o reporte
python tests/rendimiento/resumen_jmeter.py resultados.jtl
```

Se usó JMeter 5.6.3. Para medir otro servidor: `-JHOST=localhost -JPUERTO=8080 -JPROTOCOLO=http`.

### Resultados del 19 de noviembre de 2026 (Render, plan gratis; Neon)

Antes del arreglo (`resultados/antes_19nov.jtl`), con 200 peticiones por consulta:

| Consulta | Promedio | Percentil 90 | Máximo | ¿Cumple? |
|---|---|---|---|---|
| Disponibilidad | 3,514 s | 5,001 s | 6,294 s | no |
| Mis reservas | 1,662 s | 2,608 s | 5,398 s | no |

La disponibilidad hacía una consulta por cada hora del horario y cargaba cada reserva con su usuario y su cancha. Desde
el PR #36 trae solo las franjas ocupadas en una consulta. Después del arreglo (`resultados/despues_19nov.jtl`):

| Consulta | Promedio | Percentil 90 | Máximo | ¿Cumple? |
|---|---|---|---|---|
| Disponibilidad | 1,395 s | 1,999 s | 3,404 s | no |
| Mis reservas | 1,569 s | 2,599 s | 4,492 s | no |

**Primera petición con el servidor dormido** (`resultados/arranque_frio_19nov.jtl`): después de 17 minutos sin
peticiones, `GET /api/canchas` no respondió en 120 s (timeout). El plan gratis de Render apaga el servicio tras 15
minutos sin uso.

**Conclusión.**
- Con 20 jugadores a la vez, RNF-01 no se cumple en la dirección pública. El arreglo bajó el promedio de la
  disponibilidad un 60 % y el máximo de 6,3 s a 3,4 s.
- Lo que queda no es la consulta. «Mis reservas» le devuelve una lista vacía al jugador de prueba y aun así tarda
  1,6 s en promedio: las peticiones hacen fila en el plan gratis de Render, que da 0,1 de CPU.
- Una petición sola tarda entre 0,4 y 0,9 s.
- Como pide el criterio de TEC-04, el resultado se le informa al administrador antes de la prueba de aceptación
  (VAL-02).
- Para cumplir habría que pasar a un plan con más CPU, que tiene costo, o mantener el servicio despierto. Esa decisión
  es del cliente.
