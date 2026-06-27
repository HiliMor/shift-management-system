# Shift Management Backend

Spring Boot backend for the shift management system.

## Current Status

Implemented:

- Basic Spring Boot project structure.
- Public health endpoint: `GET /api/health`.
- Minimal Spring Security configuration.
- PostgreSQL connection configuration.
- Local PostgreSQL Docker Compose file.
- Flyway database migrations.
- Initial user and team domain model.
- Spring Data repositories for users, teams, team members, and team managers.
- Development seed data.

Not implemented yet:

- Login and JWT.
- Schedules, shifts, and assignments.

## Requirements

- Java 21
- Maven
- Docker Desktop, for local PostgreSQL

## Run PostgreSQL

From this directory:

```bash
docker compose up -d
```

## Run The Backend

From this directory:

```bash
mvn spring-boot:run
```

If port `8080` is already in use, run temporarily on another port:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## Check The Health Endpoint

Open in the browser:

```text
http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP",
  "timestamp": "..."
}
```

## Important Notes

- `/api/health` is public.
- All other endpoints are currently protected by Spring Security.
- Real login is not implemented yet. Spring Security may print a generated development password on startup.
- Database tables are created by Flyway migrations under `src/main/resources/db/migration`.
- Hibernate is configured with `ddl-auto: validate`, so it validates that the Java entities match the database schema instead of creating tables automatically.

## Development Seed Data

When `app.seed.enabled=true`, the application inserts initial development data if the `users` table is empty.

Seed users:

| Username | Role     | Password |
| -------- | -------- | -------- |
| manager1 | MANAGER  | password |
| employee1 | EMPLOYEE | password |
| employee2 | EMPLOYEE | password |

These users are not connected to real login yet. They are stored so the database model can be tested and reused in the next phase.

Seed team:

| Team | Swap approval policy | Default rest hours | Time zone |
| ---- | -------------------- | ------------------ | --------- |
| Operations | MANAGER | 8 | Asia/Jerusalem |
