# Portail B2B Lesieur Cristal — API

Spring Boot 3 backend for the Lesieur Cristal B2B portal: catalog, orders, samples, invoices, disputes, claims, appointments, and JWT-secured client/admin APIs. PostgreSQL + Liquibase. Outbound ERP traffic goes through a transactional outbox.

The Next.js frontend lives in a **separate repo**: [B2B-Portal-For-Lesieur-Cristal-Avril---Frontend](https://github.com/OuafikMohammed/B2B-Portal-For-Lesieur-Cristal-Avril---Frontend). Clone it next to this one as a folder named `frontend`.

```bash
cd parent
git clone https://github.com/OuafikMohammed/B2B-Portal-For-Lesieur-Cristal-Avril---Frontend.git frontend
```

```text
parent/
  frontend/           ← Next.js repo
  lc-b2b-backend/     ← this repo
```

## Run with Docker (recommended)

Needs Docker Desktop and the `frontend` sibling folder.

```bash
cd lc-b2b-backend
cp .env.example .env   # then set JWT_SECRET (32+ chars) and MAIL_USERNAME (must look like an email)
docker compose up --build
```

| Service | URL |
|---|---|
| App | http://localhost:3000 |
| API | http://localhost:8081 |
| Swagger | http://localhost:8081/swagger-ui.html |
| Activation emails (Mailpit) | http://localhost:8025 |
| Postgres | `localhost:5433` |

Stop with `docker compose down`. Add `-v` only if you also want to wipe the database volume.

Inside Docker, mail is sent to Mailpit, not Gmail. `MAIL_USERNAME` is still the **From** address, so it must be a valid email (for example `noreply@lesieurcristal.local`). Compose Postgres is a separate database from any local Postgres on port 5432.

## Run the API locally (no Docker)

Java 21, Maven, and PostgreSQL (`portail_b2b` on port 5432).

```bash
cp .env.example .env
# fill DB_* and MAIL_*
./mvnw spring-boot:run
```

Load `.env` in your IDE run configuration. The process does not read `.env` by itself.

## Environment

Do not commit `.env`. See `.env.example`.

- **Local Java** uses `DB_HOST=localhost`, `DB_PORT=5432`, and Gmail SMTP.
- **Compose** overrides host/port for Postgres and Mailpit. Optional: `POSTGRES_PASSWORD`, `NEXT_PUBLIC_API_URL`.

## Useful commands

```bash
./mvnw test
./mvnw -DskipTests package
docker compose logs -f backend
```

`./mvnw test` runs unit tests plus integration tests. The IT classes start a real PostgreSQL with Testcontainers (Docker required) and exercise login, order submit, the ERP outbox, and tenant isolation over HTTP.
