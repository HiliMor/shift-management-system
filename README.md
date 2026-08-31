# Shift Management System

A Java course project for managing team schedules, employee assignments,
availability constraints, and shift transfers/swaps, with a Hebrew/English React UI.

## Implemented Workflows

| User | Available workflows |
| --- | --- |
| Manager | Create draft schedules; create, edit, and delete shifts; assign/remove employees manually; run basic automatic assignment; check readiness; publish and reopen schedules. |
| Manager | Create reusable templates and slots, generate shifts into a draft, and delete unused templates. |
| Manager | Create new employee accounts in a managed team with optional existing staffing roles; review and approve transfer/swap requests. |
| Employee | View published schedules of active teams; use weekly/monthly calendars or a list, with an optional personal-shift filter. |
| Employee | Submit/delete unavailability constraints; create transfer/swap requests; approve/reject incoming requests; cancel outgoing active requests. |
| Both | Sign in using JWT; view personal notifications, mark them as read, and follow links to schedules or requests. |

Staffing-role creation and assignment also have manager-scoped APIs. Managers
have a separate read-only published view.
Scheduling checks cover active membership, required roles, capacity, unavailable
times, overlap, and minimum rest. Updates/deletions reject stale client state.
Calendar weeks start on Sunday. Templates generate shifts across a date range,
and JMS delivers notifications for publication and request-creation events.

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

## Getting Started And Documentation

Start with [Install, run, and verify](docs/RUN_LOCALLY.md) for prerequisites,
first-time setup, demo accounts, Postman examples, and troubleshooting. On an empty
database, the explicit initialization command creates demo accounts and data;
ordinary restarts preserve existing application data.

The [architecture guide](docs/current-backend-architecture.md) describes the
layers, domain model, authorization, transactions, concurrency, and JMS flow.
The updated design document, bilingual user guide, and instructor installation
guide are submitted separately as DOCX files.

## Verification

The test suite covers business rules, API behavior, and PostgreSQL transaction
and concurrency scenarios, alongside frontend checks. Tests are included with
the source; frontend test files are not part of the production bundle.

The 2026-08-31 run passed 279 backend unit tests, 156 PostgreSQL integration tests,
17 frontend Node tests, four focused browser checks, and the frontend build.
Full live end-to-end flows, a clean-machine installation, load testing, and broker
outage/redelivery/DLQ behavior have not yet been verified. See the
[verification guide](docs/RUN_LOCALLY.md#verification) for commands and test scope.

## Current Scope

- Team and manager setup is a technical administration step. The manager UI
  supports creating new employees with team roles; adding existing accounts,
  editing/removing members, and ongoing role administration are not fully available
  through the UI. Employee schedule access is limited to active team memberships.
- Private manager notes, recurring employee assignment series, template/slot
  editing, individual slot deletion, and request-status/team-join notifications
  are not implemented in this version. Some notification content remains English
  within the bilingual UI.

The supplied configuration is intended for local development and course
demonstration. It uses development credentials and a development JWT secret;
the broker has no persistent data volume. Public deployment would require
additional security, persistence, and operational verification work.
