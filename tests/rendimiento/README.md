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

`rnf01.jmx` simula 10 jugadores a la vez (con `-JHILOS=20`, los 20 del criterio original). Cada uno consulta 10 veces la disponibilidad y sus reservas en
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

### Ajuste del criterio: 10 jugadores a la vez (19 de noviembre de 2026)

El criterio pedía 20 jugadores consultando exactamente al mismo tiempo. Para Canchas Mónaco (3 canchas, de 2:00 a 10:00
p. m., reservas de 1 o 2 horas) es muy poco probable que 20 personas consulten en el mismo instante. El equipo, con
Miguel como Product Owner, bajó la carga del criterio a 10 jugadores a la vez, que es más realista para el complejo. El
umbral sigue igual: menos de 2,0 s.

Dos corridas con 10 jugadores, 10 vueltas cada uno (`resultados/10_jugadores_19nov.jtl` y `..._b.jtl`):

| Corrida | Consulta | Promedio | Percentil 90 | Máximo | ¿Cumple? |
|---|---|---|---|---|---|
| 1 | Disponibilidad | 0,769 s | 1,097 s | 1,567 s | sí |
| 1 | Mis reservas | 0,733 s | 1,004 s | 1,202 s | sí |
| 2 | Disponibilidad | 0,607 s | 0,813 s | 1,201 s | sí |
| 2 | Mis reservas | 0,610 s | 0,814 s | 1,207 s | sí |

Sin errores en ninguna de las dos. Con 20 jugadores a la vez sigue sin cumplir (tabla de arriba): si el complejo crece, es
el primer límite que aparece, y se resuelve con un plan de Render con más CPU.
