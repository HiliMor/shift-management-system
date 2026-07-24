# Run Locally

This guide describes how to run the shift management system locally for
development or presentation.

## Requirements

- Java 21
- Maven
- Docker Desktop
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

## 1. Start PostgreSQL

From the backend directory:

```bash
cd "/Users/hilimor/Java project/shift-management-backend"
docker compose up -d
```

The backend uses PostgreSQL and applies database migrations with Flyway when it
starts.

## 2. Start The Backend

From the backend directory:

```bash
mvn spring-boot:run
```

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

| Username | Password | Role |
| --- | --- | --- |
| `manager1` | `password` | `MANAGER` |
| `employee1` | `password` | `EMPLOYEE` |
| `employee2` | `password` | `EMPLOYEE` |

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

To stop PostgreSQL:

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
