# Shift Management Backend

Spring Boot backend for the shift management system.

## Current Status

Implemented:

- Basic Spring Boot project structure.
- Public health endpoint: `GET /api/health`.
- Minimal Spring Security configuration.
- PostgreSQL connection configuration.
- Local PostgreSQL Docker Compose file.

Not implemented yet:

- Users and teams.
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
- Database tables are not created yet. They will be added in the next steps with the first JPA entities.
