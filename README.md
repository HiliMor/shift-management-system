# Shift Management System

A course project for managing employee shifts across teams.

The project is being implemented gradually. The current focus is a working
Spring Boot backend with authentication, teams, schedules, shifts, manual
assignment rules, availability constraint support, staffing role support,
basic automatic assignment, shift templates, JMS-backed notifications, and
React frontend workflows for the main manager and employee flows.

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
- Manager-only draft schedule deletion, including its shifts and assignments.
- Draft/template deletion shows a fresh confirmation preview and requires its revision; changed child records return `409` without deletion. Drafts with transfer/swap request history cannot be deleted.
- Shift create, list, update, and delete operations.
- Shift edits require the version read by the client; stale edits return `409 STALE_VERSION` without overwriting newer data. Postman saves returned versions automatically.
- Shift edits revalidate existing assignments; invalid changes return `409` and leave the stored shift unchanged.
- Schedule publication/reopening, draft deletion, shift writes, assignment writes, and template generation share a team write lock and reload state after waiting. Assigned-shift edits also lock employees before validation.
- Manual assignment create, list, and delete operations.
- Assignment validations for team membership, duplicate assignments, shift capacity, overlap, and minimum rest.
- Manual and automatic assignment capacity checks use a PostgreSQL row-level write lock so concurrent requests cannot overfill the same shift.
- Manual and automatic assignment also lock employee rows before validation, preventing concurrent assignment creation from bypassing overlap and minimum-rest checks across schedules.
- Basic automatic assignment for draft schedules.
- Automatic assignment ranks eligible employees by fewer assigned minutes in the schedule.
- Automatic assignment returns a report with created assignments and remaining open slots.
- Shift template and template slot persistence model.
- Manager template create/list endpoints.
- Manager safe template deletion for unused templates.
- Template creation/deletion, slot creation, and generation coordinate through the team lock; deleted templates return `404` and used/duplicate templates return `409`. Expected database lock failures return `409 CONCURRENT_MODIFICATION`.
- Manager template slot create/list endpoints.
- Manager template shift generation endpoint.
- Availability constraint persistence model.
- Availability constraint create, personal list, and delete operations.
- Availability constraint creation is rejected when it overlaps an existing assignment.
- Availability creation/deletion shares the employee lock with manual/automatic assignment and transfer/swap execution, so competing operations validate committed state.
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
- Readiness and publication validate existing assignments, including capacity, membership, roles, availability, overlap, and rest. Confirmation allows open slots, not invalid assignments.
- Notification persistence model.
- Personal notification list, unread count, and mark-as-read backend endpoints.
- Event outbox persistence model for asynchronous messaging.
- Schedule publication records a pending `schedule.published` outbox event.
- ActiveMQ Artemis JMS configuration.
- Scheduled outbox dispatcher that sends pending events to JMS.
- JMS consumer that creates schedule-published notifications for active team members.
- Request-created events that notify the target employee and team managers through JMS.
- Transfer and swap request persistence model.
- Transfer request creation endpoint for published assignments.
- Swap request creation endpoint for exchanging two published assignments.
- Outgoing, incoming, manager team, and pending-manager request list endpoints.
- Target employee approval endpoint for transfer and swap requests.
- Target employee rejection endpoint for transfer and swap requests.
- Requester cancellation endpoint for active transfer and swap requests.
- Transfer execution for teams with `EMPLOYEE` approval policy.
- Manager approval and transfer execution for teams with `MANAGER` approval policy.
- Swap execution for teams with `EMPLOYEE` or `MANAGER` approval policy.
- Separate approved transfer/swap execution component in the backend request workflow.
- Transfer/swap validation failures persist as `INVALIDATED` without changing assignment owners.
- Request writes are serialized per team; final execution also locks shifts and employees to coordinate with manual/automatic assignment. Duplicate approvals return a conflict, and successful execution invalidates competing active requests.
- Basic business logging for schedule, assignment, transfer and swap request, outbox, and notification workflows.
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
- Manager automatic assignment screen connected to `POST /api/schedules/{scheduleId}/auto-assign`.
- Automatic assignment screen displays the created-assignment report and remaining open slots.
- Manager publication screen supports readiness checks, publishing draft schedules, and reopening published schedules.
- Notification center connected to personal notification list, unread count, and mark-as-read APIs.
- Transfer and swap request screen connected to outgoing, incoming, and manager team request APIs.
- Manager request view shows active requests waiting for either employee or manager approval, with actions enabled only at the correct workflow stage.
- Transfer and swap request screen supports employee request creation from a selected published schedule.
- Transfer and swap request screen supports employee approve/reject, requester cancel, and manager approve actions.
- Manager screens use focused step-based navigation for draft, build, assign, and publish workflows.
- Manager workflow uses one central selected draft schedule as the context for creating shifts, assigning employees, automatic assignment, template generation, and publication.
- The build step visually separates template management, template-based shift generation, and single-shift creation.
- Manager template screen supports template creation, slot creation, and shift generation.

Planned next:

- Follow the submission remediation roadmap in `IMPLEMENTATION_PLAN.md`: remaining cross-workflow concurrency gaps, safe demo initialization, approved functional requirements, and submission verification.

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
