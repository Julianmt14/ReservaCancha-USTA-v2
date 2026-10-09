# Estándares del equipo

Proyecto: ReservaCancha Colombia · Versión 1.0 · 4 de octubre de 2026
Equipo: Julián Mejía (backend, seguridad, pagos) y Miguel Franco (frontend, pruebas, despliegue)

Somos dos y casi nunca estamos conectados a la vez, así que no podemos depender de preguntarnos todo por
WhatsApp. Esto es lo que acordamos para trabajar sin estorbarnos: cómo escribimos el código, cómo lo
nombramos, cuándo una tarea puede empezar, cuándo se da por terminada y cómo nos revisamos. Si algo de aquí
no se puede comprobar abriendo el repositorio, lo reescribimos o lo quitamos.

Este archivo se cambia como cualquier otro: por *pull request*, revisado por el otro.

---

## 1. Estilo y nombres

**Guías que adoptamos**

| Parte | Lenguaje y herramientas | Guía |
|---|---|---|
| Backend | Java 21, Spring Boot 3.3, Maven | [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) |
| Frontend | TypeScript, Next.js 16, React 19, Tailwind 4 | Configuración oficial de Next.js (`eslint-config-next`: core-web-vitals y typescript) más el estilo de Prettier de abajo |

**Formateadores (ya están configurados)**

| Parte | Aplicar | Comprobar |
|---|---|---|
| Backend | `mvn spotless:apply` (google-java-format 1.22.0) | `mvn spotless:check` |
| Frontend | `npm run format` (Prettier: sin punto y coma, comillas simples, 110 columnas) | `npm run format:check` |

El `.editorconfig` de la raíz fija UTF-8, saltos de línea LF y sangría de 2 espacios. Para el frontend también
corre `npm run lint`, con un tope de 24 avisos en `package.json` (ver la sección 7).

**Idioma del código**

- Backend: el dominio va en español, igual que el acta y que el administrador (`Cancha`, `Reserva`, `Horario`,
  `Pago`). Los términos técnicos del framework se dejan como son (`controller`, `repository`, `token`).
- Mensajes de error, comentarios y mensajes de commit: en español.
- Frontend: lo que ve el usuario va en español. El código heredado usa nombres en inglés (`courts`, `bookings`,
  `payments`) y no lo vamos a renombrar por renombrar: dentro de un módulo no mezclamos los dos idiomas, y los
  adaptadores de `frontend/lib/api` son los que traducen al dominio del backend.

**Tres reglas propias de nombres**

1. Las clases y entidades del dominio van en singular (`Reserva`); las rutas REST, en plural y minúscula
   (`/api/reservas`, `/api/canchas`).
2. Un test se llama como una frase que dice qué pasa, no qué método prueba: `reservaConTraslapeResponde409`,
   `jugadorNoAccedeALasRutasDeAdministracion`.
3. Las variables de entorno van en mayúsculas con el prefijo del servicio (`WOMPI_PUBLIC_KEY`, `JWT_SECRET`,
   `SPRING_DATASOURCE_URL`). Las que debe ver el navegador empiezan por `NEXT_PUBLIC_`. En el repositorio solo
   viven valores de ejemplo.

---

## 2. Commits y ramas

**Formato del mensaje** (Conventional Commits)

```
tipo(alcance): qué se hace, en infinitivo y en minúscula
```

- Primera línea de 72 caracteres como máximo, sin punto final.
- Si el porqué no cabe en el título, va en el cuerpo, después de una línea en blanco.
- Un commit es un cambio que se explica en una frase. Si hace falta una "y", probablemente son dos.
- Ejemplo real del repositorio: `fix(reservas): rechazar reservas en fechas u horas que ya pasaron`.

**Tipos permitidos**

| Tipo | Cuándo |
|---|---|
| `feat` | funcionalidad nueva |
| `fix` | corrección de un error |
| `docs` | solo documentación |
| `style` | formato, sin cambiar comportamiento |
| `refactor` | reorganizar código sin cambiar comportamiento |
| `test` | pruebas |
| `chore` | configuración, dependencias, integración de ramas |

El alcance es el módulo que se toca: `auth`, `security`, `reservas`, `canchas`, `horarios`, `pagos`, `admin`,
`backend`, `frontend`, `readme`.

**Ramas**

| Rama | Para qué |
|---|---|
| `main` | lo estable y entregable; solo recibe `dev` |
| `dev` | donde se junta lo que ya fue revisado |
| `julian` | el trabajo de Julián |
| `miguel` | el trabajo de Miguel |

- Cada quien hace commits en su rama y abre un *pull request* hacia `dev`. Nadie hace commit directo en `main`
  ni en `dev`.
- Al cerrar un sprint, `dev` se integra en `main`. Los commits de integración usan `chore(merge)` y
  `chore(release)`.
- Cada commit lleva el nombre de quien lo hizo. El historial de `main` y `dev` no se reescribe, salvo que los
  dos lo acordemos por escrito.

---

## 3. Definition of Ready

Una historia entra al sprint solo si cumple todo esto. Lo revisamos entre los dos al planear.

1. Existe como *issue* en GitHub, escrita como «como [rol] quiero [acción] para [beneficio]».
2. Tiene criterios de aceptación escritos como casos: «entrada → resultado esperado», con los códigos de
   respuesta cuando es una API.
3. Se puede terminar en menos de 6 horas de trabajo de una persona. Si no, se parte (en el acta dijimos de 6 a
   8 horas por persona por semana).
4. Lo que necesita ya existe o ya está pedido: datos del administrador (horarios y precios), llaves de Wompi
   sandbox, endpoints de los que depende.
5. Si toca backend y frontend a la vez, la ruta, el método, el cuerpo y los códigos de respuesta están escritos
   en el *issue* antes de empezar.
6. Tiene una persona responsable y se sabe en qué rama se trabaja.

Si no cumple, no entra al sprint. No se discute: se completa y entra en el siguiente.

---

## 4. Definition of Done

Una historia está terminada cuando cumple las ocho condiciones. En la columna de la derecha está cómo lo
comprueba alguien que no estuvo en la conversación.

| # | Condición | Cómo se comprueba |
|---|---|---|
| 1 | Las pruebas del backend pasan. | `cd backend && mvn test` termina sin fallos. |
| 2 | Cada criterio de aceptación que toca la API tiene al menos una prueba automática. | En `backend/src/test` hay una prueba cuyo nombre describe ese criterio, y pasa. |
| 3 | Si el cambio es de interfaz, el *pull request* trae una captura y los pasos para verlo. | El *pull request* incluye la imagen y los pasos con `npm run dev`; quien revisa los repite. |
| 4 | El frontend pasa el linter y compila. | `cd frontend && npm run lint` y `npm run build` terminan con código 0. |
| 5 | El código está formateado. | `mvn spotless:check` y `npm run format:check` terminan con código 0. |
| 6 | Si el cambio toca endpoints, permisos o respuestas, la prueba de seguridad SEC-01 no falla. | Con el backend arriba, `npx newman run tests/security/SEC-01.postman_collection.json` termina con 0 fallos. |
| 7 | Si cambia un endpoint, una variable de entorno o un comando, el README cambia en el mismo cambio. | El diff del *pull request* incluye `README.md` o `backend/README.md`. |
| 8 | Otro integrante la revisó y la aprobó. | La aprobación aparece en el *pull request* y no quedan conversaciones `[bloqueante]` abiertas. |

Los puntos 3, 6 y 7 solo aplican cuando el cambio los toca. Los demás aplican siempre. La única excepción
al punto 8 está en la sección 6 (cambios que solo tocan documentación y no hay quien revise).

---

## 5. Política de revisión

**Quién revisa.** Siempre la otra persona: somos dos, así que nadie aprueba lo suyo.

**Plazo.**

- Primera respuesta (aprobar, comentar o pedir cambios): 48 horas desde que se abre el *pull request*.
- Cuando el autor corrige, el revisor responde en 24 horas.
- Antes de parciales o entregas se avisa en el grupo con un día de anticipación y el plazo puede subir a 72 horas.

**Tamaño.** Un *pull request* debería quedar por debajo de unas 400 líneas cambiadas, sin contar formato ni
archivos de dependencias. Si es más grande, se parte.

**Lo que bloquea** (el cambio no se integra hasta resolverlo):

1. Falla una prueba, el linter, el build o el formato.
2. Hay un secreto en el diff: contraseña, llave, token o un `.env`.
3. Una ruta protegida queda abierta, o SEC-01 falla.
4. Cambia un endpoint, un código de respuesta o una variable de entorno y el README no se actualiza.
5. Hay lógica nueva en el backend sin una prueba que la cubra.
6. Algo que antes funcionaba deja de funcionar.
7. Hay conflictos con `dev` sin resolver.
8. Un mensaje de commit no sigue la convención o no explica el cambio (se corrige antes de integrar).

**Lo que no bloquea** (se comenta como sugerencia y el autor decide):

- Preferencias de nombres que igual cumplen la guía.
- Refactors que no cambian el comportamiento.
- Redacción de comentarios o documentación.
- Optimizaciones que nadie ha medido.
- Los avisos de ESLint que ya existían (es deuda heredada, ver sección 7).
- Orden de imports y espacios: eso lo arregla el formateador.

**Cómo se comenta.** Cada comentario empieza con `[bloqueante]` o `[sugerencia]`, habla del código (archivo y
línea), propone una salida y justifica en media línea. Nunca se señala el problema sin ofrecer algo. Ejemplo:

> `[bloqueante]` `ReservaService.crear`: el chequeo de traslape y el guardado no son atómicos, y dos jugadores
> pueden reservar el mismo horario. Propongo una restricción única en la base o bloquear la fila de la cancha.
> Es dinero real.

**Si no nos ponemos de acuerdo.** Primero con un dato: una prueba o una medición. Si sigue el desacuerdo,
decide quien lleva ese módulo según el acta (Julián: backend, seguridad y pagos; Miguel: frontend, pruebas y
despliegue).

---

## 6. Cuando esto no se pueda cumplir

Preguntamos en voz alta en qué escenarios sería imposible y ajustamos los acuerdos:

| Escenario | Qué hacemos |
|---|---|
| El otro está en parciales y pasan 72 horas sin revisión. | A las 48 horas se insiste por el grupo. A las 72, si el cambio es solo documentación, el autor lo integra y deja escrito en el PR «integrado sin revisión por ausencia». El código nunca entra sin revisión. |
| La noche antes de una entrega falla el formato. | No es excusa: `mvn spotless:apply` y `npm run format` lo arreglan en un minuto. Lo que no se arregla así, una prueba que falla, se resuelve o se deja fuera; no se entrega a medias. |
| No hay internet o Wompi sandbox está caído. | Ninguna prueba automática depende de Wompi (el backend corre con H2 en memoria), así que la DoD se puede cumplir. El flujo de pago se prueba a mano cuando vuelva y se anota en el PR. |
| Se rompe `main` justo antes de una demo. | Se corrige en la rama personal y pasa por `dev` con revisión prioritaria (4 horas, avisando por el grupo). No se edita `main` directo. |
| El cambio es de una línea (un typo). | Pasa por *pull request* igual, pero la revisión puede ser un vistazo. Solo aplican los puntos de la DoD que lo toquen. |

---

## 7. Lo que todavía no cumplimos

Mejor escribirlo que fingir:

- El frontend heredado tiene 24 avisos de ESLint (15 vienen de dos reglas nuevas de React 19, que bajamos de
  error a aviso a propósito: reescribir esos efectos sin pruebas de interfaz era más riesgoso que dejarlos).
  El tope de 24 solo puede bajar; cuando se toque un archivo con avisos, se limpian.
- El frontend no tiene pruebas automáticas. Por eso el punto 3 de la DoD pide captura y pasos.
- RNF-01 (consultas de disponibilidad en menos de 2,0 s) se midió el 8 de octubre de 2026 con
  `tests/rendimiento/medir_rnf01.py` en local; falta medirlo con JMeter, el instrumento de la ficha.
- La integración inicial de `julian` y `miguel` en `dev`, la de `dev` en `main` y la de este mismo documento
  se hicieron sin *pull request* revisado, porque estos acuerdos todavía no existían. De aquí en adelante,
  con *pull request* revisado, empezando por el próximo cambio.

---

## 8. Aceptación

Cada integrante acepta con su nombre completo y la frase de la tabla. La aceptación de los dos quedó
registrada el 4 de octubre de 2026. Desde aquí, cambiar estos estándares necesita la aprobación de los dos
en un *pull request*.

| Nombre completo | Aceptación | Fecha |
|---|---|---|
| Julián Ricardo Mejía Torres | Conozco y acepto estos estándares. | 4 de octubre de 2026 |
| Juan Miguel Franco Baca | Conozco y acepto estos estándares. | 4 de octubre de 2026 |
