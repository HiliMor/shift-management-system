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
- JWT-based login.
- Authenticated current-user endpoint: `GET /api/auth/me`.
- Initial schedule domain model.
- Schedule creation endpoint: `POST /api/schedules`.
- Initial shift domain model.
- Shift creation endpoint: `POST /api/schedules/{scheduleId}/shifts`.
- Shift list endpoint: `GET /api/schedules/{scheduleId}/shifts`.
- Shift update endpoint: `PUT /api/schedules/{scheduleId}/shifts/{shiftId}`.
- Shift delete endpoint: `DELETE /api/schedules/{scheduleId}/shifts/{shiftId}`.

Not implemented yet:

- Schedule list, update, delete, publish, and reopen endpoints.
- Assignments.
- Team-scoped authorization for manager actions.

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

## Authentication Endpoints

Login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"manager1","password":"password"}'
```

The response contains a bearer token:

```json
{
  "tokenType": "Bearer",
  "accessToken": "...",
  "expiresInSeconds": 3600,
  "user": {
    "id": 1,
    "username": "manager1",
    "fullName": "Demo Manager",
    "applicationRole": "MANAGER"
  }
}
```

Check the current authenticated user:

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```

## Schedule Endpoints

Create a draft schedule for a managed team:

```bash
curl -X POST http://localhost:8080/api/schedules \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"teamId":1,"startDate":"2026-07-05","endDate":"2026-07-11"}'
```

Expected response:

```json
{
  "id": 1,
  "teamId": 1,
  "teamName": "Operations",
  "startDate": "2026-07-05",
  "endDate": "2026-07-11",
  "status": "DRAFT",
  "publicationNumber": 0,
  "publishedAt": null
}
```

Only managers assigned to the requested team can create schedules for that team.

Create a shift inside a draft schedule:

```bash
curl -X POST http://localhost:8080/api/schedules/1/shifts \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"startTime":"2026-07-05T06:00:00Z","endTime":"2026-07-05T14:00:00Z","description":"Morning shift","requiredWorkers":2,"minRestHours":8}'
```

Expected response:

```json
{
  "id": 1,
  "scheduleId": 1,
  "startTime": "2026-07-05T06:00:00Z",
  "endTime": "2026-07-05T14:00:00Z",
  "description": "Morning shift",
  "requiredWorkers": 2,
  "minRestHours": 8
}
```

Only managers assigned to the schedule's team can create shifts.
Shifts can be created only while the schedule is still `DRAFT`.
Shift dates must be inside the schedule date range according to the team's time zone.

List shifts in a schedule:

```bash
curl http://localhost:8080/api/schedules/1/shifts \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
[
  {
    "id": 1,
    "scheduleId": 1,
    "startTime": "2026-07-05T06:00:00Z",
    "endTime": "2026-07-05T14:00:00Z",
    "description": "Morning shift",
    "requiredWorkers": 2,
    "minRestHours": 8
  }
]
```

Only managers assigned to the schedule's team can list shifts for now.

Update a shift inside a draft schedule:

```bash
curl -X PUT http://localhost:8080/api/schedules/1/shifts/1 \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"startTime":"2026-07-05T14:00:00Z","endTime":"2026-07-05T22:00:00Z","description":"Evening shift","requiredWorkers":3,"minRestHours":10}'
```

Expected response:

```json
{
  "id": 1,
  "scheduleId": 1,
  "startTime": "2026-07-05T14:00:00Z",
  "endTime": "2026-07-05T22:00:00Z",
  "description": "Evening shift",
  "requiredWorkers": 3,
  "minRestHours": 10
}
```

Only managers assigned to the schedule's team can update shifts.
Shifts can be updated only while the schedule is still `DRAFT`.
Updated shift dates must stay inside the schedule date range according to the team's time zone.

Delete a shift from a draft schedule:

```bash
curl -X DELETE http://localhost:8080/api/schedules/1/shifts/1 \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```text
204 No Content
```

Only managers assigned to the schedule's team can delete shifts.
Shifts can be deleted only while the schedule is still `DRAFT`.

## Important Notes

- `/api/health` is public.
- `/api/auth/login` is public.
- All other endpoints are protected by JWT authentication.
- Database tables are created by Flyway migrations under `src/main/resources/db/migration`.
- Hibernate is configured with `ddl-auto: validate`, so it validates that the Java entities match the database schema instead of creating tables automatically.
- The JWT secret in `application.yml` is for local development only and must be replaced before production use.

## Development Seed Data

When `app.seed.enabled=true`, the application inserts initial development data if the `users` table is empty.

Seed users:

| Username | Role     | Password |
| -------- | -------- | -------- |
| manager1 | MANAGER  | password |
| employee1 | EMPLOYEE | password |
| employee2 | EMPLOYEE | password |

These users can be used with `POST /api/auth/login`.

Seed team:

| Team | Swap approval policy | Default rest hours | Time zone |
| ---- | -------------------- | ------------------ | --------- |
| Operations | MANAGER | 8 | Asia/Jerusalem |
