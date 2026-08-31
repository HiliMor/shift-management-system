# Install, Run And Verify

This guide covers local development and presentation. For design decisions see
[Architecture](current-backend-architecture.md); for feature gaps see
[Known Limitations](../README.md#known-limitations).

## Requirements

- Java JDK 21 and Maven 3.9; `mvn -v` should also report Java 21.
- Docker Desktop running, including Docker Compose.
- Node.js 20.19+ within version 20, or 22.12+ (matching the installed Vite version).
- pnpm 10, or the npx alternative below.
- Internet access for initial dependency/image downloads and a modern browser.
  An IDE, Postman and DBeaver are optional.

Install the prerequisites for your operating system and reopen the terminal.
If needed, set `JAVA_HOME` to the JDK directory and add Java/Maven's `bin`
directories to `PATH`. Docker Desktop must be started, not merely installed.

```bash
java -version
mvn -v
docker --version
docker compose version
node -v
```

To install pnpm:

```bash
npm install --global pnpm@10
pnpm -v
```

If global installation fails with a permissions error, no sudo is required:
replace `pnpm install` and `pnpm dev` below with `npx --yes pnpm@10 install`
and `npx --yes pnpm@10 dev`. For test/build, `npm test` and `npm run build`
are equivalent after dependencies are installed.

## 1. Obtain The Project And Start Infrastructure

Clone the repository or extract its ZIP:

```bash
git clone https://github.com/HiliMor/shift-management-system.git
cd "shift-management-system"
```

In all subsequent commands, replace `/path/to/shift-management-system` with
the actual directory. Keep paths containing spaces quoted.

```bash
cd "/path/to/shift-management-system/shift-management-backend"
docker compose up -d
docker compose ps
```

The [Compose file](../shift-management-backend/compose.yml) runs PostgreSQL and
ActiveMQ Artemis; neither needs a separate native installation.

| Service | Address | Local development credentials |
| --- | --- | --- |
| PostgreSQL | `localhost:5432`, database `shift_management` | `shift_user` / `shift_password` |
| Artemis JMS | `tcp://localhost:61616` | `artemis` / `artemis` |
| Artemis console | `http://localhost:8161` | `artemis` / `artemis` |

These credentials and the JWT secret in
[application.yml](../shift-management-backend/src/main/resources/application.yml)
are development defaults, not a public-deployment configuration.

## 2. Start The Backend

**First start, empty database:** run this from the backend directory to create
the demo accounts and scenario:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--app.seed.enabled=true"
```

**Existing database / subsequent starts:** use the normal command instead:

```bash
mvn spring-boot:run
```

Keep the terminal open. Flyway applies schema migrations and Hibernate validates
them. Initialization is disabled by default. Even when explicitly enabled, it
skips existing or partially populated databases entirely. Without initialization,
a fresh database has tables but no login accounts. Normal startup does not
recreate deleted records, undo transfers, or change dates. Do not reset the
database to apply a code update.

Check `http://localhost:8080/api/health`; expect `status: UP` and a timestamp.
Opening `http://localhost:8080/` may return `401`: this is the API server, not
the React application.

## 3. Start The Frontend

In a second terminal:

```bash
cd "/path/to/shift-management-system/shift-management-frontend"
pnpm install
pnpm dev
```

Open the URL printed by Vite, normally `http://127.0.0.1:5173`.
Dependency installation is needed only initially or after dependencies change.
CORS allows `http://localhost:5173` and `http://127.0.0.1:5173`; another
frontend port needs a matching backend CORS configuration. Postman is not
subject to browser CORS enforcement.

## Demo Accounts And Initial Check

Accounts exist only after explicit initialization of an empty database:

| Username | Password | Application role |
| --- | --- | --- |
| `manager1` | `password` | `MANAGER` |
| `employee1` through `employee8` | `password` | `EMPLOYEE` |

The scenario contains one development team, eight active employees, three
staffing roles, a published schedule with assignments, empty seven-day and
21-day drafts, a daily template with three eight-hour slots, eight preloaded
publication notifications, and a transfer request from employee1 to employee2.
Generate 21 shifts in the seven-day draft or 63 in the 21-day draft as needed.
Dates are chosen once at initialization, not moved forward on restart.

1. Sign in as manager1 and check the managed team/draft workflow.
2. Sign out and sign in as employee1; open a published schedule and its details.
3. To check real JMS delivery, publish a draft or create a **new** transfer/swap
   request, then refresh the intended recipient's notifications after a short wait.

Preloaded notifications/request data do not prove JMS delivery: they are fixtures
inserted directly into the database. A consumed broker queue may already be empty.
The backend terminal logs dispatch and notification creation; PostgreSQL retains
`event_outbox` and `notifications` rows. DBeaver can connect using the database
settings above. `sent_at` records dispatch, not end-to-end delivery confirmation.

New employees can be created through the manager's Team employees screen.
Creating another team/manager currently requires controlled technical database
provisioning, not a UI/API action. A manager requires both a `MANAGER` user with
a BCrypt password hash and a `team_managers` link; never insert a plain password
into `password_hash`. The initializer cannot add these to an existing database.

## Verification

Backend commands, from `shift-management-backend`:

```bash
mvn test
mvn -B -Ppostgres-it verify
```

The first command runs unit tests without Docker. The second also runs `*IT`
integration tests with Maven Failsafe and a disposable PostgreSQL 16 Testcontainer
on a random port. It applies the real migrations and does not use the development
database. Docker must be running; the application servers need not be running.
Messaging is disabled in these tests; initialization is disabled except in its
dedicated tests. The first run may download dependencies and images.

Frontend commands, from `shift-management-frontend`:

```bash
pnpm test
pnpm build
```

The Node suite covers confirmation, calendar/filtering, edit conversion and
employee-creation API contracts with mocks. For four browser response-ordering
checks, start Vite, open `http://127.0.0.1:5173/tests/published-schedules.html`,
and select **Run regression checks**. It uses the real React hook with delayed
fake responses, no login/database, and is not part of the build or Node test run.

### Representative Tests

| Concern | Example test class/file | What it verifies |
| --- | --- | --- |
| Business rules | `AssignmentServiceTest` | Assignment orchestration and rejection rules with mocked collaborators. |
| Concurrent scheduling | `AssignmentConcurrencyIT`, `AvailabilityConcurrencyIT` | Real lock waits, overlap/rest/capacity and constraint races. |
| Atomic transfer/swap | `SwapRequestExecutionIT` | Both owners change or neither does; invalidation and duplicate approvals. |
| Editing/publication | `ScheduleValidationIT`, `ScheduleWorkflowConcurrencyIT`, `ShiftEditingIT` | Rollback, assignment validity, concurrent writes and stale versions. |
| Safe deletion/templates | `DeletionPreconditionIT`, `TemplateConcurrencyIT` | Changed confirmation snapshots and generation/deletion conflicts. |
| Initialization/accounts | `DevelopmentDataSeederIT`, `TeamEmployeeIT` | Restart safety, atomic creation and authorization/duplicate handling. |
| Async UI responses | `tests/published-schedules.html` | Old responses cannot replace the current selection or reset state. |

Source: [backend tests](../shift-management-backend/src/test/java/com/hilimor/shiftmanagement)
and [frontend source/tests](../shift-management-frontend/src).
The 2026-08-31 run passed 279 unit, 156 PostgreSQL, 17 Node and four focused
browser checks, plus the frontend build. MockMvc tests using injected identities
are not login/JWT tests. This evidence does not replace a clean-machine rehearsal,
full authenticated UI flows, load tests or actual broker outage/redelivery checks.

## Postman

Import both files, not just the collection:

- [Shift Management API](postman/shift-management-api.postman_collection.json)
- [Shift Management Local environment](postman/local.postman_environment.json)

Select **Shift Management Local**, set `baseUrl` to the running backend
(default `http://localhost:8080`), then run a request from **Auth** to log in.
Login stores `accessToken` automatically; requests inherit Bearer authentication.
Re-login after expiry; there is no permanent token/refresh-token workflow.
Do not commit exported environments containing real tokens or passwords.

The collection is the runnable reference for the main API paths, request bodies
and responses; separate copies of the same curl/JSON examples are not maintained.
Review returned IDs and dates before writes. Some requests store `scheduleId`,
`shiftId`, `assignmentId` and `transferRequestId` automatically.

Two additional read-only routes used by the UI are not yet collection entries.
They require the same Bearer token and no request body:

| Method and path | Result |
| --- | --- |
| `GET /api/teams/me/memberships` | The signed-in user's active team memberships. |
| `GET /api/requests/manager` | Active requests in managed teams, including those waiting for employee approval; `/manager/pending` lists only the manager-approval stage. |

A typical flow: **Health Check**, **Login as Manager**, **List My Managed Teams**,
**List Team Employees**, **Create Draft Schedule**, **Create Shift**, **Create
Assignment**, **Publication Readiness**, **Publish Schedule**. For transfers,
log in as the requester, create the request, log in as the target and approve,
then log in as the manager and approve if required by the team's policy.
Target rejection and requester cancellation are alternative endings.
Read the resulting status: `200` with `INVALIDATED` means execution did not occur.

For **Teams / Create Team Employee**, set `teamId`, `newEmployeeUsername` and
`newEmployeePassword` locally; the credential fields are deliberately blank in
the repository environment. Creation supports new employees, not managers or
existing-account membership.

### Versioned Edits

Create/list/update responses include shift `version`. **Create Shift** saves
`shiftId` and `shiftVersion`; for an existing shift, set the IDs and run
**List Schedule Shifts**. **Update Shift** requires that non-negative version
along with the reviewed fields; success stores the new version.

After `409 STALE_VERSION`, reload and compare before resubmitting. Do not merely
replace the version on an old request body. To demonstrate the protection on a
test draft, save an edit, then try a second edit with the previous version: it
must be rejected and the first edit must remain. Missing/null/negative versions
return `400`; a deleted shift returns `404`.

### Confirmed Deletion

Re-import the current collection if older requests have no preview/revision.

1. Choose the resource ID and run **Preview Draft Deletion**, **Preview Template
   Deletion**, **Preview Shift Deletion**, or **Preview Assignment Deletion**.
2. Review the identity, dates, employee and child counts. The preview saves the
   matching `scheduleDeletionRevision`, `templateDeletionRevision`,
   `shiftDeletionRevision`, or `assignmentDeletionRevision`.
3. Run the matching DELETE without changing the ID. Success returns `204`.
   A conflict requires a new explicit preview and review, not automatic retry.

Previews clear old revisions before loading. Missing/malformed revisions return
`400`; changed records return `409`; already-deleted records return `404`.
Used templates and resources with source/target request history cannot be deleted.
To test stale confirmation, preview a disposable draft, create another shift,
then DELETE using the old revision: the draft and all shifts must remain.
In the UI, Cancel closes confirmation without sending DELETE.

## Updates, Ports And Troubleshooting

After pulling changes, restart both backend and frontend. This loads new routes
and keeps the version/revision API contract aligned. No demo reset is needed.

| Symptom | Check |
| --- | --- |
| `pnpm: command not found` / global permission error | Use the npx alternative under Requirements. |
| Backend cannot connect to DB/broker | Start Docker Desktop and check `docker compose ps` and ports 5432/61616. |
| `401` on a protected request | Re-login, check credentials/token and backend URL. |
| `403` | Check the user's role and association with the selected team/resource. |
| `409` | Read the error code/message; check capacity, availability, overlap/rest, version or deletion revision. |
| No accounts after a new install | Use explicit initialization on an empty DB; it never tops up a partial DB. |
| No new notification | Perform a real supported action, allow dispatch time, refresh as the correct recipient and inspect backend logs. |
| Frontend works but API requests fail | Check backend URL and CORS origin; the terminal shows the actual frontend port. |

For an existing DB on an alternate backend port:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

For first initialization on that port, use instead:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081 --app.seed.enabled=true"
```

From the frontend directory on macOS/Linux:

```bash
VITE_API_BASE_URL=http://localhost:8081 pnpm dev
```

On Windows PowerShell:

```powershell
$env:VITE_API_BASE_URL="http://localhost:8081"
pnpm dev
```

Also update Postman's `baseUrl` when changing backend ports.

## Stop Or Reset

Stop backend/frontend with `Ctrl+C`. From the backend directory, stop
infrastructure without deleting its containers:

```bash
docker compose stop
```

PostgreSQL uses a named volume. Artemis currently has no persistent data volume:
`docker compose down` removes the broker container and can discard queued messages.
Use `docker compose up -d` to start infrastructure again.

**Optional destructive reset:** only after stopping the backend and backing up
anything needed. This removes all local DB data, including manual work, and
recreates the broker. It does not delete project files or Git history.

```bash
docker compose down -v
docker compose up -d
mvn spring-boot:run "-Dspring-boot.run.arguments=--app.seed.enabled=true"
```

Never use reset as a normal update/install step on an existing database.
