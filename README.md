# ReservaCancha Colombia — v1.0

Centralizar la gestión de reservas y pagos de canchas deportivas (Acta de Proyecto, Gerencia de Software — USTA Villavicencio, 28-sep-2026).

**Equipo**
- Julián Mejía: backend, seguridad, API, JWT, roles, Wompi.
- Miguel Franco: frontend, datos, despliegue, pruebas, integración API.

**Alcance v1.0:** usuarios y roles · canchas y horarios · disponibilidad · crear/consultar reservas · Wompi **sandbox** · consultas admin.
**Excluye:** app móvil nativa, facturación DIAN, PayPal/Stripe, torneos, SMS, QR.

## Estructura

```
ReservaCancha-Colombia/
  docs/     Acta-Proyecto-ReservaCancha-Colombia.pdf
  backend/  Spring Boot 3.3 + Java 21 + JPA + Security/JWT + Postgres/H2 + Wompi sandbox  (puerto 8080)
  frontend/ Next.js 16 + React 19 + Tailwind (puerto 3000) — `lib/api.ts` consume el backend
  docker-compose.yml  Postgres 16 para dev
```

## Arranque rápido

```bash
# 1) Base de datos (opcional; sin esto usa H2 en memoria)
docker compose up -d db

# 2) Backend
cd backend
# con Maven instalado:
mvn spring-boot:run
# o con variables Postgres:
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/reservacancha SPRING_DATASOURCE_USERNAME=rc SPRING_DATASOURCE_PASSWORD=rc123 SPRING_DATASOURCE_DRIVER=org.postgresql.Driver mvn spring-boot:run

# 3) Frontend
cd ../frontend
cp .env.example .env.local
npm install
npm run dev
```

Admin seed: `admin@reservacancha.co` / `admin123`.

## API (backend)

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/api/auth/register` | no | registro (rol default JUGADOR) |
| POST | `/api/auth/login` | no | login → JWT |
| GET | `/api/canchas` | no | listar activas |
| POST | `/api/canchas` | ADMIN | crear |
| GET | `/api/horarios/cancha/{id}` | no | horarios |
| POST | `/api/horarios` | ADMIN | crear horario |
| GET | `/api/disponibilidad?canchaId=&fecha=` | no | ocupadas + libres 06:00-22:00 (RNF-01 < 2,0 s) |
| POST | `/api/reservas` | JWT | crear (valida traslape → 409) |
| GET | `/api/reservas/mias` | JWT | mis reservas |
| POST | `/api/reservas/{id}/cancelar` | JWT dueño/ADMIN | cancelar |
| POST | `/api/pagos/iniciar/{reservaId}` | JWT | firma Wompi sandbox |
| POST | `/api/wompi/webhook` | no | webhook sandbox |
| GET | `/api/admin/resumen` | ADMIN | conteos |

## Wompi sandbox (10 flujos antes de semana 15)

1. Crear reserva → `POST /api/reservas`.
2. `POST /api/pagos/iniciar/{id}` → `{referencia, valorCentavos, firmaIntegridad}`.
3. Widget checkout con `NEXT_PUBLIC_WOMPI_PUBLIC_KEY`.
4. Webhook confirma → reserva `PAGADA`.

## Calidad (ISO/IEC 25010)

- RNF-01 desempeño: `GET /api/disponibilidad` < 2,0 s (JMeter). 2,0 exacto = incumple.
- SEC-01 seguridad: 0 accesos no autorizados (Postman/Newman: sin token, token inválido, rol insuficiente).

## Subir a GitHub

```bash
cd ReservaCancha-Colombia
git init -b main
git add .
git commit -m "v1.0 base: Spring Boot + Next.js + acta"
gh repo create ReservaCancha-Colombia --private --source=. --push
# o manual: crear repo en github.com y:
git remote add origin https://github.com/<usuario>/ReservaCancha-Colombia.git
git push -u origin main
```
