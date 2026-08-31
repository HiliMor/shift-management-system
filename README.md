# Shift Management System

A Java course project for managing team schedules, employee assignments,
availability constraints, and shift transfers/swaps, with a Hebrew/English React UI.

## Documentation

- [Install, run, and verify](docs/RUN_LOCALLY.md): first-time setup, demo accounts,
  tests, Postman, troubleshooting, and safe shutdown.
- [Architecture](docs/current-backend-architecture.md): layers, domain model,
  authorization, transactions, concurrency, JMS, and implementation boundaries.

The updated design document, bilingual user guide, and installation guide for
the instructor are submitted separately as DOCX files. Local planning notes are
not required to build or run the repository. Known limitations remain below;
removing duplicate documentation does not change the agreed project requirements.

## Implemented Workflows

| User | Available workflows |
| --- | --- |
| Manager | Create draft schedules; create, edit, and delete shifts; assign/remove employees manually; run basic automatic assignment; check readiness; publish and reopen schedules. |
| Manager | Create reusable templates and slots, generate shifts into a draft, and delete unused templates. |
| Manager | Create new employee accounts in a managed team with optional existing staffing roles; review and approve transfer/swap requests. |
| Employee | View published schedules of active teams; use weekly/monthly calendars or a list, with an optional personal-shift filter. |
| Employee | Submit/delete unavailability constraints; create transfer/swap requests; approve/reject incoming requests; cancel outgoing active requests. |
| Both | Sign in using JWT; view personal notifications, mark them as read, and follow links to schedules or requests. |

Staffing-role creation and assignment also have manager-scoped APIs; not every
administrative API has a UI. Managers have a separate read-only published view.
Scheduling checks cover active membership, required roles, capacity, unavailable
times, overlap, and minimum rest. Updates/deletions reject stale client state.
Calendar weeks start on Sunday. Templates generate shifts, not recurring employee
assignment series. JMS currently handles publication and request-creation events.

## Technology And Structure

- Java 21, Spring Boot, Spring Security/JWT, Spring Data JPA/Hibernate.
- PostgreSQL and Flyway migrations; Hibernate validates the schema.
- JMS with ActiveMQ Artemis and a transactional event outbox.
- React and Vite, with shared components and workflow hooks.

```text
shift-management-backend/   Java source, SQL migrations, tests, Docker Compose
shift-management-frontend/ React source and tests
docs/                      Architecture and run instructions
docs/postman/              API collection and local environment JSON
```

Start with [Run Locally](docs/RUN_LOCALLY.md). On an empty database, the explicit
initialization command creates demo login accounts; ordinary restarts never reset
or refill application data. There is no public registration endpoint.

## Verification

Tests stay in the repository. They do not become demo data, and frontend tests
are not included in the production bundle. See [verification commands and their
limits](docs/RUN_LOCALLY.md#verification).

The 2026-08-31 run passed 279 backend unit tests, 156 PostgreSQL integration tests,
17 frontend Node tests, four focused browser checks, and the frontend build.
These results are not a claim of complete end-to-end or production verification.

## Known Limitations

- No UI/API for creating teams or managers; initial setup is technical administration.
  New employee creation is supported, but adding existing accounts to teams,
  member editing/removal, invitations, and password reset/change are not implemented.
- Private manager notes and recurring employee assignment series are not implemented.
- Employees cannot view published schedules of teams they do not actively belong to.
- Template/slot editing and individual slot deletion are not implemented.
- JMS request-status/team-join notifications are not implemented. Some notification
  content remains English even when the surrounding UI is Hebrew.
- Full live end-to-end verification, a clean-machine installation rehearsal, broad
  load tests, and broker outage/redelivery/DLQ verification remain outstanding.
- Configuration contains development-only credentials and a JWT secret. The current
  broker container has no persistent data volume. This is not a public-deployment setup.

The [architecture](docs/current-backend-architecture.md) explains these boundaries
and the trade-off of serializing scheduling writes within each team.
