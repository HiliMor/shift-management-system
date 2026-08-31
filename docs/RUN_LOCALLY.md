# Run Locally

This guide describes how to run the shift management system locally for
development or presentation.

## Requirements

- Java 21
- Maven
- Docker Desktop, for PostgreSQL and ActiveMQ Artemis
- Node.js 20 or newer
- pnpm

Check the installed versions:

```bash
java -version
mvn -v
docker --version
node -v
pnpm -v
```

## 1. Start PostgreSQL And ActiveMQ Artemis

From the backend directory:

```bash
cd "/Users/hilimor/Java project/shift-management-backend"
docker compose up -d
```

The backend uses PostgreSQL and applies database migrations with Flyway when it
starts. It also uses ActiveMQ Artemis for JMS notification events.

Local infrastructure ports:

| Service | URL or port |
| --- | --- |
| PostgreSQL | `localhost:5432` |
| ActiveMQ Artemis JMS | `localhost:61616` |
| ActiveMQ Artemis console | `http://localhost:8161` |

## 2. Start The Backend

From the backend directory:

```bash
mvn spring-boot:run
```

This is the normal command for an existing database. Demo initialization is
disabled by default (`app.seed.enabled=false`); restarting does not recreate
deleted records, restore old assignments, or change the dates of existing data.

### First Start With Demo Data (Empty Database Only)

On a fresh local installation, use this command **instead of** the normal command
above to create the demo accounts and scenario:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--app.seed.enabled=true
```

Initialization runs once in a single transaction, after Flyway creates the
schema. If the database already contains application data, it is skipped entirely,
even with this flag. Existing or partially populated databases are not topped up.
Use normal startup on subsequent runs. Do not reset an existing database just to
apply a code update. Without initialization, a fresh database has no login accounts.
These known demo credentials are for local use only, not a public deployment.

Expected backend URL:

```text
http://localhost:8080
```

Health check:

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

## 3. Start The Frontend

Open a second terminal window.

From the frontend directory:

```bash
cd "/Users/hilimor/Java project/shift-management-frontend"
pnpm install
pnpm dev
```

Expected frontend URL:

```text
http://127.0.0.1:5173
```

## Demo Users

These accounts are created by the explicit first-time initialization above.
Existing accounts retain their current names and passwords on subsequent starts.

| Username | Password | Role |
| --- | --- | --- |
| `manager1` | `password` | `MANAGER` |
| `employee1` | `password` | `EMPLOYEE` |
| `employee2` | `password` | `EMPLOYEE` |
| `employee3` | `password` | `EMPLOYEE` |
| `employee4` | `password` | `EMPLOYEE` |
| `employee5` | `password` | `EMPLOYEE` |
| `employee6` | `password` | `EMPLOYEE` |
| `employee7` | `password` | `EMPLOYEE` |
| `employee8` | `password` | `EMPLOYEE` |

The same initialization creates this presentation scenario:

- A `צוות פיתוח` managed by `manager1`.
- Eight active employees with three staffing roles.
- A published schedule with shifts and assignments for employees.
- An empty seven-day draft schedule for manual assignment practice.
- An empty 21-day draft schedule for automatic assignment practice.
- An active daily template with three eight-hour development coverage slots.
- Eight preloaded schedule-published notifications for active employees.
- An active transfer request from `employee1` to `employee2`.

The manager can generate 21 shifts from the daily template into the seven-day
draft for manual assignment practice, or generate 63 shifts into the 21-day draft
for automatic assignment.

The dates are selected at initialization time, not advanced on every restart.
The preloaded notifications are fixtures inserted directly into PostgreSQL. To
demonstrate JMS delivery, publish a draft or create a new transfer/swap request
through the UI or API; those real actions use the outbox and Artemis queue.

## Reset Local Demo Data

**Optional and destructive: this removes all local application data, including
manual changes. Back up anything you need first.** It is not required for normal
restarts or updates. Only use it when you intentionally want to discard the
entire local database and start a new demo.

Stop the backend, then run from the backend directory:

```bash
cd "/Users/hilimor/Java project/shift-management-backend"
docker compose down -v
docker compose up -d
```

The `-v` option deletes the local PostgreSQL data volume. Compose also recreates
the broker container, so do not rely on its pending messages surviving this reset.
Project files, Git history, and GitHub are unaffected. Now use the explicit
first-time initialization command above. Normal startup alone creates the schema
but does not insert demo data.

## If Port 8080 Is Busy

Run the backend on another port:

```bash
cd "/Users/hilimor/Java project/shift-management-backend"
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Then run the frontend with the matching backend URL:

```bash
cd "/Users/hilimor/Java project/shift-management-frontend"
VITE_API_BASE_URL=http://localhost:8081 pnpm dev
```

## Stop The System

Stop backend and frontend terminal processes with `Ctrl+C`.

To stop PostgreSQL and ActiveMQ Artemis:

```bash
cd "/Users/hilimor/Java project/shift-management-backend"
docker compose down
```

## Notes

- Browser requests from the frontend require backend CORS configuration.
- `node_modules/`, `dist/`, and local package caches are generated locally and
  should not be committed to Git.
- `pnpm install` is only needed the first time, or after frontend dependencies
  change.
