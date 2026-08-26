# Shift Management System

A course project for managing employee shifts across teams.

The project is being implemented gradually. The current focus is a working
Spring Boot backend with authentication, teams, schedules, shifts, manual
assignment rules, availability constraint support, staffing role support,
JMS-backed notifications, and the first React frontend workflows.

## Repository Structure

- `shift-management-backend/` - Spring Boot backend.
- `shift-management-frontend/` - React frontend.
- `IMPLEMENTATION_PLAN.md` - step-by-step implementation plan and current status.
- `spec-revised.md` - main project specification.

## Current Backend Status

Implemented:

- JWT login and authenticated API access.
- Users, teams, team members, and team managers.
- Managed team employee listing.
- Draft schedule creation.
- Shift create, list, update, and delete operations.
- Manual assignment create, list, and delete operations.
- Assignment validations for team membership, duplicate assignments, shift capacity, overlap, and minimum rest.
- Availability constraint persistence model.
- Availability constraint create, personal list, and delete operations.
- Availability constraint creation is rejected when it overlaps an existing assignment.
- Assignment creation is rejected when it overlaps an employee availability constraint.
- Initial staffing role persistence model.
- Staffing role assignment persistence between team members and staffing roles.
- Staffing role create and list operations.
- Optional required staffing role on shifts.
- Employee staffing role assignment and list operations.
- Assignment creation validates required staffing roles.
- Schedule publication from `DRAFT` to `PUBLISHED`.
- Schedule reopening from `PUBLISHED` to `DRAFT`.
- Employee published schedule list endpoint.
- Employee published schedule details endpoint with shifts and assignments.
- Manager published schedule list endpoint.
- Publication readiness report.
- Explicit confirmation for publishing schedules with unfilled shifts.
- Notification persistence model.
- Personal notification list, unread count, and mark-as-read backend endpoints.
- Event outbox persistence model for asynchronous messaging.
- Schedule publication records a pending `schedule.published` outbox event.
- ActiveMQ Artemis JMS configuration.
- Scheduled outbox dispatcher that sends pending events to JMS.
- JMS consumer that creates schedule-published notifications for active team members.
- Transfer request persistence model.
- Transfer request creation endpoint for published assignments.
- Outgoing, incoming, and pending-manager transfer request list endpoints.
- Target employee approval endpoint for transfer requests.
- Target employee rejection endpoint for transfer requests.
- Requester cancellation endpoint for active transfer requests.
- Transfer execution for teams with `EMPLOYEE` approval policy.
- Manager approval and transfer execution for teams with `MANAGER` approval policy.
- Basic business logging for schedule, assignment, transfer request, outbox, and notification workflows.
- Unified JSON error responses for API and security errors.
- Development seed data for a presentation scenario with users, a managed team, staffing roles, draft and published schedules, assignments, notifications, and an active transfer request.

Frontend:

- Login screen connected to `POST /api/auth/login`.
- JWT session stored in browser local storage.
- Expired JWT sessions return to the login screen with a clear message.
- Role-based workspace title and navigation.
- Published schedule list loaded from `GET /api/schedules/me/published`.
- Published schedule details loaded from `GET /api/schedules/me/published/{scheduleId}`.
- Shift and assignment display for selected published schedules.
- Employee availability constraint screen connected to create, list, and delete APIs.
- Managed team list loaded from `GET /api/teams/me/managed`.
- Manager draft schedule creation connected to `POST /api/schedules`.
- Managed draft schedule list loaded from `GET /api/schedules/me/managed/drafts`.
- Manager shift creation connected to `POST /api/schedules/{scheduleId}/shifts`.
- Manager manual assignment screen connected to `POST /api/assignments`.
- Assignment screen loads draft schedule shifts, team employees, and existing schedule assignments.
- Manager publication screen supports readiness checks, publishing draft schedules, and reopening published schedules.
- Notification center connected to personal notification list, unread count, and mark-as-read APIs.
- Transfer request screen connected to outgoing, incoming, and pending-manager request APIs.
- Transfer request screen supports employee approve/reject, requester cancel, and manager approve actions.

Planned next:

- Full swap requests.
- Templates and automatic assignment.

## Backend Documentation

End-to-end local run instructions are documented in:

```text
docs/RUN_LOCALLY.md
```

Backend setup, run instructions, and API examples are documented in:

```text
shift-management-backend/README.md
```

The current backend and system context architecture is documented in:

```text
docs/current-backend-architecture.md
```

Frontend setup and run instructions are documented in:

```text
shift-management-frontend/README.md
```

Postman collection import instructions are documented in:

```text
docs/postman/README.md
```

## Development Approach

The project is built in small phases. Each phase adds a limited piece of
functionality, tests the business rules, and updates the documentation before
moving to the next feature.
