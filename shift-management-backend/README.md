# Shift Management Backend

Spring Boot backend for the shift management system.

For end-to-end local run instructions, see:

```text
../docs/RUN_LOCALLY.md
```

## Current Status

Implemented:

- Basic Spring Boot project structure.
- Public health endpoint: `GET /api/health`.
- Minimal Spring Security configuration.
- PostgreSQL connection configuration.
- Local PostgreSQL and ActiveMQ Artemis Docker Compose file.
- Flyway database migrations.
- Initial user and team domain model.
- Spring Data repositories for users, teams, team members, and team managers.
- Opt-in, empty-database-only development demo initialization.
- JWT-based login.
- Authenticated current-user endpoint: `GET /api/auth/me`.
- Managed teams endpoint: `GET /api/teams/me/managed`.
- Managed team employee endpoint: `GET /api/teams/{teamId}/employees`.
- CORS support for the local React development server.
- Initial schedule domain model.
- Schedule creation endpoint: `POST /api/schedules`.
- Draft deletion preview: `GET /api/schedules/{scheduleId}/deletion-preview`; deletion: `DELETE /api/schedules/{scheduleId}?revision={revision}`.
- Initial shift domain model.
- Shift creation endpoint: `POST /api/schedules/{scheduleId}/shifts`.
- Shift list endpoint: `GET /api/schedules/{scheduleId}/shifts`.
- Shift update endpoint: `PUT /api/schedules/{scheduleId}/shifts/{shiftId}`.
- Shift responses expose `version`; updates require that version and reject stale edits with `409 STALE_VERSION`.
- Shift edits validate existing assignments and roll back invalid changes.
- `ScheduleWriteLock` coordinates publication/reopening, draft deletion, shift writes, assignment writes, and template generation with request execution through the same team row lock.
- Shift deletion preview: `GET /api/schedules/{scheduleId}/shifts/{shiftId}/deletion-preview`; deletion: `DELETE /api/schedules/{scheduleId}/shifts/{shiftId}?revision={revision}`.
- Initial assignment domain model.
- Manual assignment endpoint: `POST /api/assignments`.
- Assignment list endpoint: `GET /api/schedules/{scheduleId}/assignments`.
- Assignment deletion preview: `GET /api/assignments/{assignmentId}/deletion-preview`; deletion: `DELETE /api/assignments/{assignmentId}?revision={revision}`.
- Assignment validation for team membership, duplicate assignment, shift capacity, availability constraints, overlap, and minimum rest.
- Manual and automatic assignment capacity checks use a PostgreSQL row-level write lock so concurrent requests cannot overfill the same shift.
- Employee row locks serialize concurrent manual/automatic assignment creation across schedules, so overlap and minimum-rest checks see committed assignments from preceding operations.
- Basic automatic assignment endpoint: `POST /api/schedules/{scheduleId}/auto-assign`.
- Automatic assignment ranks eligible employees by fewer assigned minutes in the schedule.
- Automatic assignment returns a report with created assignments and remaining open slots.
- Shift template and template slot persistence model.
- Optional source template slot reference on generated shifts.
- Template create endpoint: `POST /api/teams/{teamId}/templates`.
- Template list endpoint: `GET /api/teams/{teamId}/templates`.
- Template deletion preview: `GET /api/templates/{templateId}/deletion-preview`; deletion: `DELETE /api/templates/{templateId}?revision={revision}`.
- Template slot create endpoint: `POST /api/templates/{templateId}/slots`.
- Template slot list endpoint: `GET /api/templates/{templateId}/slots`.
- Template shift generation endpoint: `POST /api/templates/{templateId}/generate`.
- Template writes share the team lock: duplicate names, deletion, slot creation, and generation are checked against refreshed committed state.
- Initial availability constraint domain model.
- Availability constraint creation endpoint: `POST /api/availability-constraints`.
- Personal availability constraint list endpoint: `GET /api/availability-constraints/me`.
- Availability constraint delete endpoint: `DELETE /api/availability-constraints/{constraintId}`.
- Availability constraint creation is rejected when it overlaps an existing assignment.
- Availability creation/deletion shares the employee lock with manual/automatic assignment and transfer/swap execution; conflicting operations recheck state after waiting.
- Assignment creation is rejected when it overlaps an employee availability constraint.
- Initial staffing role persistence model.
- Staffing role assignment persistence between team members and staffing roles.
- Staffing role create endpoint: `POST /api/teams/{teamId}/staffing-roles`.
- Staffing role list endpoint: `GET /api/teams/{teamId}/staffing-roles`.
- Optional required staffing role on shifts.
- Employee staffing role assignment endpoint: `POST /api/teams/{teamId}/employees/{employeeId}/staffing-roles`.
- Employee staffing role list endpoint: `GET /api/teams/{teamId}/employees/{employeeId}/staffing-roles`.
- Assignment creation validates required staffing roles.
- Schedule publish endpoint: `POST /api/schedules/{scheduleId}/publish`.
- Schedule reopen endpoint: `POST /api/schedules/{scheduleId}/reopen`.
- Employee published schedule list endpoint: `GET /api/schedules/me/published`.
- Employee published schedule details endpoint: `GET /api/schedules/me/published/{scheduleId}`.
- Manager draft schedule list endpoint: `GET /api/schedules/me/managed/drafts`.
- Manager published schedule list endpoint: `GET /api/schedules/me/managed/published`.
- Publication readiness report endpoint: `GET /api/schedules/{scheduleId}/publication-readiness`.
- Explicit confirmation for publishing schedules with unfilled shifts.
- Notification persistence model.
- Personal notification list endpoint: `GET /api/notifications`.
- Personal unread notification count endpoint: `GET /api/notifications/unread-count`.
- Mark notification as read endpoint: `POST /api/notifications/{notificationId}/read`.
- Event outbox persistence model for pending asynchronous events.
- Schedule publication records a pending `schedule.published` event in `event_outbox`.
- Spring JMS configuration with ActiveMQ Artemis.
- Scheduled outbox dispatcher that sends pending events to JMS queue `notification.events`.
- JMS consumer that creates schedule-published notifications for active team members.
- Request-created events that notify the target employee and team managers through JMS.
- Notification creation is idempotent by `eventId` and recipient.
- Transfer and swap request persistence model.
- Transfer request creation endpoint: `POST /api/requests/transfers`.
- Swap request creation endpoint: `POST /api/requests/swaps`.
- Outgoing request list endpoint: `GET /api/requests/me/outgoing`.
- Incoming request list endpoint: `GET /api/requests/me/incoming`.
- Manager team request list endpoint: `GET /api/requests/manager` (active requests from the manager's teams, including requests waiting for target employee approval).
- Pending manager approval request list endpoint: `GET /api/requests/manager/pending`.
- Target employee approval endpoint for transfer and swap requests: `POST /api/requests/{requestId}/employee-approve`.
- Target employee rejection endpoint for transfer and swap requests: `POST /api/requests/{requestId}/employee-reject`.
- Requester cancellation endpoint for active transfer and swap requests: `POST /api/requests/{requestId}/cancel`.
- Transfer execution for teams with `EMPLOYEE` approval policy.
- Manager approval endpoint: `POST /api/requests/{requestId}/manager-approve`.
- Transfer execution for teams with `MANAGER` approval policy.
- Swap execution for teams with `EMPLOYEE` or `MANAGER` approval policy.
- Separate `SwapRequestExecutor` component for approved transfer and swap execution.
- `SwapRequestLock` serializes request creation and status changes per team and refreshes state after waiting. Final execution also locks shifts and employees in the same order as manual/automatic assignment.
- Failed execution validation persists `INVALIDATED` without partial ownership changes. Successful execution invalidates active requests that reference either changed assignment.
- Basic business logging for schedule, assignment, transfer and swap request, outbox, and notification workflows.
- Unified JSON error responses for API and security errors.

Not implemented yet:

- Schedule list, update, and delete endpoints.
- Remaining team-scoped authorization for future manager workflows.

## Requirements

- Java 21
- Maven
- Docker Desktop, for local PostgreSQL and ActiveMQ Artemis

## Run PostgreSQL And ActiveMQ Artemis

From this directory:

```bash
docker compose up -d
```

The backend expects:

- PostgreSQL on `localhost:5432`
- ActiveMQ Artemis JMS on `localhost:61616`
- ActiveMQ Artemis console on `http://localhost:8161`

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

## API Error Responses

Most API and security errors use a unified JSON response shape:

```json
{
  "status": 409,
  "error": "Conflict",
  "code": "SHIFT_OVERLAP",
  "message": "Employee already has an overlapping assignment",
  "path": "/api/assignments",
  "timestamp": "2026-08-10T18:00:00Z"
}
```

Common error codes:

- `VALIDATION_ERROR` - request body validation failed.
- `MALFORMED_REQUEST` - request body is missing or invalid JSON.
- `UNAUTHORIZED` - authentication is missing or invalid.
- `FORBIDDEN` - the authenticated user is not allowed to perform the action.
- `NOT_FOUND` - the requested resource is not visible or does not exist.
- `STALE_VERSION` (`409`) - a submitted edit version is outdated; reload and review the current fields before saving.
- `CONCURRENT_MODIFICATION` (`409`) - a Spring/JPA pessimistic-lock failure or lock timeout prevented the operation; reload and retry deliberately.
- Business validation codes such as `SHIFT_OVERLAP`, `SHIFT_CAPACITY`, `MINIMUM_REST`, `TEAM_MEMBERSHIP`, and `STAFFING_ROLE_REQUIRED`.

The lock-error handler does not catch every database error. Unrelated integrity
errors and unexpected exceptions still use the server-error path; unique-name
and template-reference conflicts are prevented by coordinated checks rather
than relabeled indiscriminately. There is no automatic write retry. Production
database timeout settings are unchanged; the two-second `lock_timeout` used by
`TemplateConcurrencyIT` applies only to its disposable test database connections.

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

## Team Endpoints

List teams managed by the authenticated manager:

```bash
curl http://localhost:8080/api/teams/me/managed \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
[
  {
    "id": 1,
    "name": "צוות פיתוח",
    "swapApprovalPolicy": "MANAGER",
    "defaultMinRestHours": 8,
    "timeZone": "Asia/Jerusalem"
  }
]
```

Users who do not manage teams receive an empty list.

List the authenticated user's active team memberships and staffing roles:

```bash
curl http://localhost:8080/api/teams/me/memberships \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
[
  {
    "teamId": 1,
    "teamName": "צוות פיתוח",
    "staffingRoleNames": ["Backend Developer"]
  }
]
```

The endpoint is self-scoped: it returns only active memberships and staffing roles for the authenticated user.

List active employees in a managed team:

```bash
curl http://localhost:8080/api/teams/1/employees \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
[
  {
    "id": 2,
    "username": "employee1",
    "fullName": "אלון כהן",
    "staffingRoleIds": [10],
    "staffingRoleNames": ["Backend Developer"]
  },
  {
    "id": 3,
    "username": "employee2",
    "fullName": "נועה לוי",
    "staffingRoleIds": [11],
    "staffingRoleNames": ["Frontend Developer"]
  }
]
```

Only managers assigned to the requested team can list its employees.

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
  "teamName": "צוות פיתוח",
  "startDate": "2026-07-05",
  "endDate": "2026-07-11",
  "status": "DRAFT",
  "publicationNumber": 0,
  "publishedAt": null
}
```

Only managers assigned to the requested team can create schedules for that team.

Delete a draft schedule managed by the authenticated manager:

```bash
curl http://localhost:8080/api/schedules/1/deletion-preview \
  -H "Authorization: Bearer <TOKEN>"

# Review schedule identity/dates, shiftCount, and assignmentCount in the response.
curl -X DELETE "http://localhost:8080/api/schedules/1?revision=<REVISION_FROM_PREVIEW>" \
  -H "Authorization: Bearer <TOKEN>"
```

Only `DRAFT` schedules can be deleted. The operation removes the schedule's
assignments and shifts before removing the schedule itself. Published schedules
cannot be deleted through this endpoint. Drafts with transfer/swap request
history also return `409`, preserving their referenced assignments and requests.

The required revision represents the parent and child IDs/versions, so a new
shift, edited shift, or changed assignment invalidates an earlier confirmation.
Missing/malformed revisions return `400`; stale revisions return `409` without
deleting anything. Fetch another preview, review it, and explicitly confirm again.
The preview does not reserve the data while the user reads it. Authorization and
the draft/history rules are checked again under the team lock on DELETE.

List draft schedules managed by the authenticated manager:

```bash
curl http://localhost:8080/api/schedules/me/managed/drafts \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
[
  {
    "id": 1,
    "teamId": 1,
  "teamName": "צוות פיתוח",
    "startDate": "2026-07-05",
    "endDate": "2026-07-11",
    "status": "DRAFT",
    "publicationNumber": 0,
    "publishedAt": null
  }
]
```

Users who do not manage teams receive an empty list.

List published schedules managed by the authenticated manager:

```bash
curl http://localhost:8080/api/schedules/me/managed/published \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
[
  {
    "id": 2,
    "teamId": 1,
  "teamName": "צוות פיתוח",
    "startDate": "2026-07-12",
    "endDate": "2026-07-18",
    "status": "PUBLISHED",
    "publicationNumber": 1,
    "publishedAt": "2026-07-20T08:59:10.000000Z"
  }
]
```

Users who do not manage teams receive an empty list.

Run automatic assignment for a draft schedule:

```bash
curl -X POST http://localhost:8080/api/schedules/1/auto-assign \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
{
  "scheduleId": 1,
  "totalShifts": 2,
  "assignmentsCreated": 1,
  "totalOpenSlotsBefore": 2,
  "totalOpenSlotsAfter": 1,
  "shifts": [
    {
      "shiftId": 1,
      "startTime": "2026-07-05T06:00:00Z",
      "endTime": "2026-07-05T14:00:00Z",
      "description": "Morning shift",
      "requiredWorkers": 2,
      "assignedWorkersBefore": 1,
      "assignmentsCreated": 1,
      "openSlotsBefore": 1,
      "openSlotsAfter": 0,
      "message": "All open slots were assigned",
      "createdAssignments": [
        {
          "id": 10,
          "shiftId": 1,
          "employeeId": 2,
          "employeeUsername": "employee1",
          "employeeFullName": "Demo Employee",
          "assignedAt": "2026-07-05T09:00:00Z"
        }
      ]
    }
  ]
}
```

Only managers assigned to the schedule's team can run automatic assignment.
Automatic assignment can run only while the schedule is a draft.
The algorithm uses the same eligibility rules as manual assignment: active team membership, required staffing role, no duplicate assignment, employee availability, no overlapping shift, and minimum rest.
It fills open slots greedily and ranks candidates by fewer assigned minutes in the same schedule, then by employee name for deterministic tie-breaking.
The response reports both successful assignments and shifts that remain open because no eligible employee was available.

## Template Endpoints

Create a shift template for a managed team:

```bash
curl -X POST http://localhost:8080/api/teams/1/templates \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "name": "Routine Week",
    "description": "Three standard shifts per day",
    "cycleDays": 7,
    "defaultMinRestHours": 8
  }'
```

Expected response:

```json
{
  "id": 1,
  "teamId": 1,
  "teamName": "צוות פיתוח",
  "name": "Routine Week",
  "description": "Three standard shifts per day",
  "cycleDays": 7,
  "defaultMinRestHours": 8,
  "active": false
}
```

List templates for a managed team:

```bash
curl http://localhost:8080/api/teams/1/templates \
  -H "Authorization: Bearer <TOKEN>"
```

Add a slot to a template:

```bash
curl -X POST http://localhost:8080/api/templates/1/slots \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "dayOffset": 0,
    "startTime": "08:00:00",
    "durationMinutes": 480,
    "description": "Morning shift",
    "requiredWorkers": 2,
    "requiredStaffingRoleId": null
  }'
```

Expected response:

```json
{
  "id": 1,
  "shiftTemplateId": 1,
  "dayOffset": 0,
  "startTime": "08:00:00",
  "durationMinutes": 480,
  "description": "Morning shift",
  "requiredWorkers": 2,
  "requiredStaffingRoleId": null,
  "requiredStaffingRoleName": null
}
```

List slots for a template:

```bash
curl http://localhost:8080/api/templates/1/slots \
  -H "Authorization: Bearer <TOKEN>"
```

Generate shifts from a template into a draft schedule:

```bash
curl -X POST http://localhost:8080/api/templates/1/generate \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "scheduleId": 1
  }'
```

Expected response:

```json
{
  "templateId": 1,
  "scheduleId": 1,
  "shiftsCreated": 3,
  "skippedExistingShifts": 0,
  "skippedOutsideSchedule": 0,
  "shifts": [
    {
      "id": 1,
      "scheduleId": 1,
      "startTime": "2026-07-06T05:00:00Z",
      "endTime": "2026-07-06T13:00:00Z",
      "description": "Morning shift",
      "requiredWorkers": 2,
      "minRestHours": 8,
      "requiredStaffingRoleId": null,
      "requiredStaffingRoleName": null,
      "templateSlotId": 1
    }
  ]
}
```

Only managers assigned to the template's team can create or list templates, slots, and generated shifts.
Template names must be unique inside the same team.
A slot's `dayOffset` must fit inside the template cycle, and a required staffing role must belong to the template's team.
Generation is allowed only into draft schedules that belong to the same team as the template.
Running generation again skips shifts that were already created from the same template slot at the same start time.

Delete a template that has not been used to generate shifts:

```bash
curl http://localhost:8080/api/templates/1/deletion-preview \
  -H "Authorization: Bearer <TOKEN>"

# Review template identity and slotCount in the response.
curl -X DELETE "http://localhost:8080/api/templates/1?revision=<REVISION_FROM_PREVIEW>" \
  -H "Authorization: Bearer <TOKEN>"
```

Only managers of the template's team can delete it. Deleting a template also
deletes its template slots. A template that is referenced by existing shifts
returns `409 Conflict` and is kept.

Template creation locks the team before checking the normalized name. Two
concurrent creations of the same name in one team produce one template and one
`409`; the same name is allowed in different teams. Slot creation, template
deletion, and generation acquire that team lock and refresh the template before
using it. Locks last until commit/rollback. Read-only listing takes no write lock.

If deletion commits first, a waiting generation, slot creation, or repeated
deletion returns `404 Template not found`. If generation commits first, deletion
sees the generated shifts and returns `409` without deleting the template or
slots. Deleting the last draft that uses the template permits a later deletion.
Adding a slot before generation includes it in that run; adding it after
generation does not retroactively change existing shifts. Generate again to add
its occurrences while skipping those already created. Deletion requires the
revision from a fresh deletion preview. The revision includes slot IDs/versions
as well as the template ID/version. A slot added after preview makes deletion
return `409`, even if the template row's own version has not changed. Missing or
malformed revisions return `400`. Do not automatically refresh and retry DELETE.

Publish a draft schedule:

```bash
curl -X POST http://localhost:8080/api/schedules/1/publish \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
{
  "id": 1,
  "teamId": 1,
    "teamName": "צוות פיתוח",
  "startDate": "2026-07-05",
  "endDate": "2026-07-11",
  "status": "PUBLISHED",
  "publicationNumber": 1,
  "publishedAt": "2026-07-20T08:59:10.000000Z"
}
```

Only managers assigned to the schedule's team can publish it.
Only draft schedules can be published.
Publishing sets the schedule status to `PUBLISHED`, records `publishedAt`, and increments `publicationNumber`.
Publishing also stores a pending `schedule.published` event in `event_outbox`.
The scheduled outbox dispatcher sends this event to JMS queue `notification.events`.
The JMS consumer creates one notification for each active member of the schedule's team.
Publishing without a request body is allowed only when the schedule readiness report has `readyToPublish: true`.
When the readiness report has `readyToPublish: false`, publishing without confirmation returns `409 Conflict`.

Publish a draft schedule with unfilled shifts after explicit manager confirmation:

```bash
curl -X POST http://localhost:8080/api/schedules/1/publish \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"confirmUnfilled":true}'
```

Use this only after reviewing the publication readiness report.

Reopen a published schedule:

```bash
curl -X POST http://localhost:8080/api/schedules/1/reopen \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
{
  "id": 1,
  "teamId": 1,
    "teamName": "צוות פיתוח",
  "startDate": "2026-07-05",
  "endDate": "2026-07-11",
  "status": "DRAFT",
  "publicationNumber": 1,
  "publishedAt": "2026-07-20T08:59:10.000000Z"
}
```

Only managers assigned to the schedule's team can reopen it.
Only published schedules can be reopened.
Reopening returns the schedule to `DRAFT` so shifts and assignments can be edited again.
Reopening does not increment `publicationNumber` and does not clear `publishedAt`; those fields keep the history of the latest publication.
Publishing the reopened schedule again increments `publicationNumber`.

View publication readiness before publishing:

```bash
curl http://localhost:8080/api/schedules/1/publication-readiness \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response for a schedule that is not fully assigned:

```json
{
  "schedule": {
    "id": 1,
    "teamId": 1,
    "teamName": "צוות פיתוח",
    "startDate": "2026-07-05",
    "endDate": "2026-07-11",
    "status": "DRAFT",
    "publicationNumber": 0,
    "publishedAt": null
  },
  "readyToPublish": false,
  "totalShifts": 2,
  "totalRequiredWorkers": 4,
  "totalAssignedWorkers": 3,
  "totalOpenSlots": 1,
  "unfilledShifts": [
    {
      "shiftId": 1,
      "startTime": "2026-07-05T06:00:00Z",
      "endTime": "2026-07-05T14:00:00Z",
      "description": "Morning shift",
      "requiredWorkers": 2,
      "assignedWorkers": 1,
      "openSlots": 1,
      "filled": false
    }
  ]
}
```

Only managers assigned to the schedule's team can view publication readiness.
This report is read-only. It does not publish the schedule and does not change assignments.
Publishing a schedule with open slots requires `confirmUnfilled: true`.

Both readiness and publication revalidate existing assignments: active team
membership, required staffing role, availability, overlap with other assignments,
minimum rest before/after the shift, and assigned count not exceeding capacity.
The current assignment is excluded from its own overlap/rest queries. A full
shift is valid; only excess staffing is a capacity conflict.

If any assignment is invalid, readiness and publication return `409 Conflict`
with a business code and the affected shift ID (and employee ID for eligibility
failures), instead of reporting the schedule as ready. Publication performs these
checks even when `confirmUnfilled` is `true`. Rejected publication leaves the
schedule in `DRAFT`, does not increment its publication number, and creates no
`schedule.published` outbox event. Correct the conflicting assignment or shift
before retrying; confirmation only permits open slots.

List published schedules for the authenticated employee:

```bash
curl http://localhost:8080/api/schedules/me/published \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
[
  {
    "id": 1,
    "teamId": 1,
    "teamName": "צוות פיתוח",
    "startDate": "2026-07-05",
    "endDate": "2026-07-11",
    "status": "PUBLISHED",
    "publicationNumber": 1,
    "publishedAt": "2026-07-20T08:59:10.000000Z"
  }
]
```

The authenticated user sees only published schedules for teams where they are an active team member.
Draft schedules are not returned.
This endpoint returns schedule headers only.

Get one published schedule with shifts and assignments for the authenticated employee:

```bash
curl http://localhost:8080/api/schedules/me/published/1 \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
{
  "schedule": {
    "id": 1,
    "teamId": 1,
    "teamName": "צוות פיתוח",
    "startDate": "2026-07-05",
    "endDate": "2026-07-11",
    "status": "PUBLISHED",
    "publicationNumber": 1,
    "publishedAt": "2026-07-20T08:59:10.000000Z"
  },
  "shifts": [
    {
      "id": 1,
      "scheduleId": 1,
      "startTime": "2026-07-05T06:00:00Z",
      "endTime": "2026-07-05T14:00:00Z",
      "description": "Morning shift",
      "requiredWorkers": 2,
      "minRestHours": 8,
      "requiredStaffingRoleId": null,
      "requiredStaffingRoleName": null,
      "assignments": [
        {
          "id": 1,
          "shiftId": 1,
          "employeeId": 2,
          "employeeUsername": "employee1",
          "employeeFullName": "Demo Employee",
          "assignedAt": "2026-07-11T07:17:41.000000Z"
        }
      ]
    }
  ]
}
```

The authenticated user can view details only for published schedules belonging to active teams they are a member of.
Draft schedules and schedules from unrelated teams return `404 Not Found`.

## Notification Endpoints

List notifications for the authenticated user:

```bash
curl http://localhost:8080/api/notifications \
  -H "Authorization: Bearer <TOKEN>"
```

Expected empty response before any notifications have been created:

```json
[]
```

Expected response shape after notifications exist:

```json
[
  {
    "id": 1,
    "eventId": "6f22d2a9-2e22-4d38-a2f8-bf820ae2a6d1",
    "type": "SCHEDULE_PUBLISHED",
    "title": "Schedule published",
    "message": "לוח צוות הפיתוח פורסם.",
    "relatedEntityType": "SCHEDULE",
    "relatedEntityId": 1,
    "createdAt": "2026-07-31T18:00:00Z",
    "readAt": null,
    "read": false
  }
]
```

Count unread notifications for the authenticated user:

```bash
curl http://localhost:8080/api/notifications/unread-count \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
{
  "unreadCount": 0
}
```

Mark one owned notification as read:

```bash
curl -X POST http://localhost:8080/api/notifications/1/read \
  -H "Authorization: Bearer <TOKEN>"
```

The authenticated user can access only their own notifications.
Missing notifications and notifications owned by another user return `404 Not Found`.

Create a shift inside a draft schedule:

```bash
curl -X POST http://localhost:8080/api/schedules/1/shifts \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"startTime":"2026-07-05T06:00:00Z","endTime":"2026-07-05T14:00:00Z","description":"Morning shift","requiredWorkers":2,"minRestHours":8,"requiredStaffingRoleId":null}'
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
  "minRestHours": 8,
  "requiredStaffingRoleId": null,
  "requiredStaffingRoleName": null,
  "templateSlotId": null,
  "version": 0
}
```

Only managers assigned to the schedule's team can create shifts.
Shifts can be created only while the schedule is still `DRAFT`.
Shift dates must be inside the schedule date range according to the team's time zone.
`requiredStaffingRoleId` is optional. A missing or `null` value means the shift has no professional role requirement.
When `requiredStaffingRoleId` is provided, it must reference a staffing role from the schedule's team.
Assignment creation validates that the employee has the required staffing role in the shift's team.

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
    "minRestHours": 8,
    "requiredStaffingRoleId": null,
    "requiredStaffingRoleName": null,
    "templateSlotId": null,
    "version": 0
  }
]
```

Only managers assigned to the schedule's team can list shifts for now.

Update a shift inside a draft schedule:

```bash
curl -X PUT http://localhost:8080/api/schedules/1/shifts/1 \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"startTime":"2026-07-05T14:00:00Z","endTime":"2026-07-05T22:00:00Z","description":"Evening shift","requiredWorkers":3,"minRestHours":10,"requiredStaffingRoleId":null,"version":0}'
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
  "minRestHours": 10,
  "requiredStaffingRoleId": null,
  "requiredStaffingRoleName": null,
  "templateSlotId": null,
  "version": 1
}
```

Only managers assigned to the schedule's team can update shifts.
Shifts can be updated only while the schedule is still `DRAFT`.
The request must include the non-negative `version` returned when reading that
shift. Missing/null/negative versions return `400 VALIDATION_ERROR`; an outdated
version returns `409 STALE_VERSION` without changing any fields. A missing or
deleted shift returns `404`. The example uses an initial version of `0`, not a
constant to reuse for every update. Existing API clients must send this field.

The server compares versions after locking and refreshing the shift. Validation
and saving happen in the same transaction; a flush before building the response
ensures it contains the saved version. Reuse the returned version for your next
edit. An unchanged save may keep the same version. On a stale conflict, reload,
review newer values, then reapply your intended changes; do not automatically
retry the old body with a freshly fetched version. The React shift edit form is
still planned in roadmap part 9; the current editing client is the API/Postman.

Updated shift dates must stay inside the schedule date range according to the team's time zone.
Updating a shift with `requiredStaffingRoleId: null` clears the professional role requirement.
Existing employees must remain eligible for the edited shift, and
`requiredWorkers` cannot be reduced below the number already assigned. An
invalid edit returns `409` with a business code such as `SHIFT_OVERLAP`,
`AVAILABILITY_CONFLICT`, `MINIMUM_REST`, `STAFFING_ROLE_REQUIRED`,
`TEAM_MEMBERSHIP`, or `SHIFT_CAPACITY`. All edited fields roll back and existing
assignments remain unchanged. Unassigned shifts can still be edited normally.

Delete a shift from a draft schedule:

```bash
curl http://localhost:8080/api/schedules/1/shifts/1/deletion-preview \
  -H "Authorization: Bearer <TOKEN>"

# Review shift identity/times and assignmentCount before confirming.
curl -X DELETE "http://localhost:8080/api/schedules/1/shifts/1?revision=<REVISION_FROM_PREVIEW>" \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```text
204 No Content
```

Only managers assigned to the schedule's team can delete shifts.

The preview includes `shift`, `assignmentCount`, and `revision`. Deletion checks
the shift version, schedule version, and all assignment IDs/versions under the
team lock. It removes the shift and its assignments only if that snapshot still
matches. Transfer/swap history referencing any assignment in the shift blocks
deletion with `409`, including cancelled or completed requests.
Shifts can be deleted only while the schedule is still `DRAFT`.

## Assignment Endpoints

Create a manual assignment:

```bash
curl -X POST http://localhost:8080/api/assignments \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"shiftId":1,"employeeId":2}'
```

Expected response:

```json
{
  "id": 1,
  "shiftId": 1,
  "employeeId": 2,
  "employeeUsername": "employee1",
  "employeeFullName": "Demo Employee",
  "assignedAt": "2026-07-11T07:17:41.000000Z"
}
```

Only managers assigned to the shift's team can create assignments.
Assignments can be created only while the schedule is still `DRAFT`.

List all assignments in a schedule:

```bash
curl http://localhost:8080/api/schedules/1/assignments \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
[
  {
    "id": 1,
    "shiftId": 1,
    "employeeId": 2,
    "employeeUsername": "employee1",
    "employeeFullName": "Demo Employee",
    "assignedAt": "2026-07-11T07:17:41.000000Z"
  }
]
```

Only managers assigned to the schedule's team can list assignments for that schedule.

Delete an assignment from a draft schedule:

```bash
curl http://localhost:8080/api/assignments/1/deletion-preview \
  -H "Authorization: Bearer <TOKEN>"

# Review assignment.employeeFullName and shift start/end times before confirming.
curl -X DELETE "http://localhost:8080/api/assignments/1?revision=<REVISION_FROM_PREVIEW>" \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```text
204 No Content
```

Only managers assigned to the assignment's team can delete assignments.

The preview includes `assignment`, `shift`, and `revision`. The revision covers
the assignment, shift, and schedule IDs/versions. Removal keeps the shift and
other assignments; request history involving this assignment blocks removal.
Both individual DELETE APIs require the exact preview revision: missing/malformed
values return `400`, stale state or request history returns `409`, and a removed
record returns `404`. Never automatically fetch a new revision to retry DELETE.
Missing JPA rows, including lazy associations lost during a concurrent deletion,
map to `404` without exposing internal entity details. Other unexpected database
errors still use the normal server-error handler.
Assignments can be deleted only while the schedule is still `DRAFT`.

`AssignmentValidator` currently validates, in order:

1. The employee is an active member of the shift's team.
2. If the shift requires a staffing role, the employee has that role in the shift's team.
3. The employee is not already assigned to the same shift.
4. The shift still has available capacity.
5. The employee has no overlapping availability constraint.
6. The employee has no overlapping assignment in any team.
7. The employee has enough rest before and after the shift.

Scheduling validation failures return a stable error code:

```json
{
  "code": "STAFFING_ROLE_REQUIRED",
  "message": "Employee does not have the staffing role required for this shift"
}
```

## Transfer And Swap Request Endpoints

Create a transfer request for one of the authenticated employee's published assignments:

```bash
curl -X POST http://localhost:8080/api/requests/transfers \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"sourceAssignmentId":1,"targetEmployeeId":3}'
```

Expected response:

```json
{
  "id": 1,
  "type": "TRANSFER",
  "status": "PENDING_EMPLOYEE",
  "requesterId": 2,
  "requesterUsername": "employee1",
  "requesterFullName": "אלון כהן",
  "sourceAssignmentId": 1,
  "sourceShiftId": 1,
  "sourceShiftDescription": "Backend service support",
  "sourceShiftStartTime": "2026-08-04T06:00:00.000000Z",
  "sourceShiftEndTime": "2026-08-04T14:00:00.000000Z",
  "targetEmployeeId": 3,
  "targetEmployeeUsername": "employee2",
  "targetEmployeeFullName": "נועה לוי",
  "targetAssignmentId": null,
  "targetShiftId": null,
  "targetShiftDescription": null,
  "targetShiftStartTime": null,
  "targetShiftEndTime": null,
  "employeeApprovedAt": null,
  "managerApprovedById": null,
  "managerApprovedAt": null,
  "createdAt": "2026-08-04T18:00:00.000000Z",
  "updatedAt": "2026-08-04T18:00:00.000000Z"
}
```

Current transfer request rules:

1. Only an `EMPLOYEE` user can create a transfer request.
2. The source assignment must belong to the authenticated employee.
3. The source assignment must belong to a `PUBLISHED` schedule.
4. The target employee must be a different `EMPLOYEE` user.
5. The target employee must be an active member of the source shift's team.
6. Only one active request can involve the same source assignment.

Create a swap request for one of the authenticated employee's published assignments
and another employee's published assignment in the same team:

```bash
curl -X POST http://localhost:8080/api/requests/swaps \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"sourceAssignmentId":1,"targetAssignmentId":2}'
```

Expected response:

```json
{
  "id": 2,
  "type": "SWAP",
  "status": "PENDING_EMPLOYEE",
  "requesterId": 2,
  "requesterUsername": "employee1",
  "requesterFullName": "אלון כהן",
  "sourceAssignmentId": 1,
  "sourceShiftId": 1,
  "sourceShiftDescription": "Backend service support",
  "sourceShiftStartTime": "2026-08-04T06:00:00.000000Z",
  "sourceShiftEndTime": "2026-08-04T14:00:00.000000Z",
  "targetEmployeeId": 3,
  "targetEmployeeUsername": "employee2",
  "targetEmployeeFullName": "נועה לוי",
  "targetAssignmentId": 2,
  "targetShiftId": 2,
  "targetShiftDescription": "Frontend release support",
  "targetShiftStartTime": "2026-08-05T06:00:00.000000Z",
  "targetShiftEndTime": "2026-08-05T14:00:00.000000Z",
  "employeeApprovedAt": null,
  "managerApprovedById": null,
  "managerApprovedAt": null,
  "createdAt": "2026-08-28T18:00:00.000000Z",
  "updatedAt": "2026-08-28T18:00:00.000000Z"
}
```

Current swap request rules:

1. Only an `EMPLOYEE` user can create a swap request.
2. The source assignment must belong to the authenticated employee.
3. The target assignment must belong to a different employee.
4. Both assignments must belong to `PUBLISHED` schedules.
5. Both assignments must belong to the same team.
6. The target employee must be an active member of the source shift's team.
7. Only one active request can involve either assignment.

List transfer or swap requests created by the authenticated employee:

```bash
curl http://localhost:8080/api/requests/me/outgoing \
  -H "Authorization: Bearer <TOKEN>"
```

List transfer or swap requests targeting the authenticated employee:

```bash
curl http://localhost:8080/api/requests/me/incoming \
  -H "Authorization: Bearer <TOKEN>"
```

List transfer or swap requests waiting for manager approval in teams managed by the authenticated manager:

```bash
curl http://localhost:8080/api/requests/manager/pending \
  -H "Authorization: Bearer <TOKEN>"
```

Request list endpoints return the same response shape as transfer or swap creation.
Employee list endpoints are limited to the authenticated employee, and the manager
list endpoints are limited to teams managed by the authenticated manager. The manager
team request list includes active requests in both `PENDING_EMPLOYEE` and
`PENDING_MANAGER` states, while the pending manager approval endpoint includes only
requests that are ready for manager action.
The response also includes source and, for full swaps, target shift descriptions
and start/end times so clients can identify the concrete shifts involved.

Approve an incoming transfer or swap request as the target employee:

```bash
curl -X POST http://localhost:8080/api/requests/1/employee-approve \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response for a team with `MANAGER` approval policy:

```json
{
  "id": 1,
  "type": "TRANSFER",
  "status": "PENDING_MANAGER",
  "requesterId": 2,
  "requesterUsername": "employee1",
  "requesterFullName": "אלון כהן",
  "sourceAssignmentId": 1,
  "sourceShiftId": 1,
  "sourceShiftDescription": "Backend service support",
  "sourceShiftStartTime": "2026-08-04T06:00:00.000000Z",
  "sourceShiftEndTime": "2026-08-04T14:00:00.000000Z",
  "targetEmployeeId": 3,
  "targetEmployeeUsername": "employee2",
  "targetEmployeeFullName": "נועה לוי",
  "targetAssignmentId": null,
  "targetShiftId": null,
  "targetShiftDescription": null,
  "targetShiftStartTime": null,
  "targetShiftEndTime": null,
  "employeeApprovedAt": "2026-08-05T18:00:00.000000Z",
  "managerApprovedById": null,
  "managerApprovedAt": null,
  "createdAt": "2026-08-04T18:00:00.000000Z",
  "updatedAt": "2026-08-05T18:00:00.000000Z"
}
```

Employee approval rules:

1. Only the target employee can approve the request.
2. Requests can be approved only while their status is `PENDING_EMPLOYEE`.
3. If the team's approval policy is `EMPLOYEE`, the request status becomes `APPROVED` and the backend executes the transfer or swap immediately.
4. If the team's approval policy is `MANAGER`, the request status becomes `PENDING_MANAGER`.
5. For `EMPLOYEE` policy teams, the backend re-runs assignment eligibility checks before changing assignments.

If eligibility fails during an `EMPLOYEE` policy approval, the request becomes
`INVALIDATED` and assignments remain unchanged.

Reject an incoming transfer or swap request as the target employee:

```bash
curl -X POST http://localhost:8080/api/requests/1/employee-reject \
  -H "Authorization: Bearer <TOKEN>"
```

Employee rejection rules:

1. Only the target employee can reject the request.
2. Requests can be rejected only while their status is `PENDING_EMPLOYEE`.
3. Rejection changes the request status to `REJECTED`.
4. Rejection never changes the assignment.

Cancel an active transfer or swap request as the requester:

```bash
curl -X POST http://localhost:8080/api/requests/1/cancel \
  -H "Authorization: Bearer <TOKEN>"
```

Cancellation rules:

1. Only the employee who created the request can cancel it.
2. Requests can be cancelled only while their status is `PENDING_EMPLOYEE` or `PENDING_MANAGER`.
3. Cancellation changes the request status to `CANCELLED`.
4. Cancellation never changes the assignment.

Approve a pending transfer or swap request as a manager:

```bash
curl -X POST http://localhost:8080/api/requests/1/manager-approve \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response when the target employee is still eligible:

```json
{
  "id": 1,
  "type": "TRANSFER",
  "status": "APPROVED",
  "requesterId": 2,
  "requesterUsername": "employee1",
  "requesterFullName": "אלון כהן",
  "sourceAssignmentId": 1,
  "sourceShiftId": 1,
  "sourceShiftDescription": "Backend service support",
  "sourceShiftStartTime": "2026-08-04T06:00:00.000000Z",
  "sourceShiftEndTime": "2026-08-04T14:00:00.000000Z",
  "targetEmployeeId": 3,
  "targetEmployeeUsername": "employee2",
  "targetEmployeeFullName": "נועה לוי",
  "targetAssignmentId": null,
  "targetShiftId": null,
  "targetShiftDescription": null,
  "targetShiftStartTime": null,
  "targetShiftEndTime": null,
  "employeeApprovedAt": "2026-08-05T18:00:00.000000Z",
  "managerApprovedById": 1,
  "managerApprovedAt": "2026-08-05T18:30:00.000000Z",
  "createdAt": "2026-08-04T18:00:00.000000Z",
  "updatedAt": "2026-08-05T18:30:00.000000Z"
}
```

Manager approval rules:

1. Only a `MANAGER` user can approve at this step.
2. The manager must manage the source shift's team.
3. Requests can be manager-approved only while their status is `PENDING_MANAGER`.
4. Before changing assignments, the backend re-runs assignment eligibility checks.
5. A transfer moves the source assignment to the target employee.
6. A swap exchanges the source and target assignments.

If eligibility fails during manager approval, the request becomes `INVALIDATED`
and assignments remain unchanged.

## Availability Constraint Endpoints

Create a personal availability constraint:

```bash
curl -X POST http://localhost:8080/api/availability-constraints \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"startTime":"2026-07-13T06:00:00Z","endTime":"2026-07-13T14:00:00Z","reason":"Doctor appointment"}'
```

Expected response:

```json
{
  "id": 1,
  "employeeId": 2,
  "startTime": "2026-07-13T06:00:00Z",
  "endTime": "2026-07-13T14:00:00Z",
  "reason": "Doctor appointment",
  "createdAt": "2026-07-12T19:25:58.000000Z"
}
```

The authenticated user creates availability constraints only for their own account.
Creating an availability constraint that overlaps one of the authenticated user's existing assignments returns `409 Conflict`.

Availability creation and deletion lock the authenticated employee row before
checking assignments or loading the constraint. Manual/automatic assignment and
transfer/swap execution use the same employee lock. If an assignment commits
first, a competing overlapping constraint returns `409`; if the constraint
commits first, manual assignment returns `409`, automatic assignment skips the
employee, and transfer/swap execution persists `INVALIDATED` without changing
owners. Deletion releases the time range for a waiting assignment after commit.
Two concurrent deletions of the same constraint return `204` then `404`.
Personal constraint listing remains read-only and does not take this write lock.

List personal availability constraints:

```bash
curl http://localhost:8080/api/availability-constraints/me \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
[
  {
    "id": 1,
    "employeeId": 2,
    "startTime": "2026-07-13T06:00:00Z",
    "endTime": "2026-07-13T14:00:00Z",
    "reason": "Doctor appointment",
    "createdAt": "2026-07-12T19:25:58.000000Z"
  }
]
```

Delete a personal availability constraint:

```bash
curl -X DELETE http://localhost:8080/api/availability-constraints/1 \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```text
204 No Content
```

The authenticated user can delete only their own availability constraints.
Deleting a missing constraint, or another user's constraint, returns `404`.

Availability constraints are checked during assignment creation.

## Staffing Roles

A staffing role is a team-specific professional scheduling role, such as `Shift Supervisor` or `Entrance Guard`.
It is different from `ApplicationRole`:

- `ApplicationRole` controls broad system permissions, such as `MANAGER` or `EMPLOYEE`.
- `StaffingRole` describes what work an employee is qualified to cover inside one team.

Current persistence rules:

- Each staffing role belongs to one team.
- Role names are required and trimmed by the domain model.
- A team cannot have two staffing roles with the same name.
- Different teams may use the same staffing role name independently.
- Staffing roles can be connected to team members.
- A team member can receive only staffing roles from the same team.
- The same staffing role cannot be assigned twice to the same team member.

Create a staffing role for a managed team:

```bash
curl -X POST http://localhost:8080/api/teams/1/staffing-roles \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"name":"Shift Supervisor","description":"Can supervise a shift"}'
```

Expected response:

```json
{
  "id": 1,
  "teamId": 1,
  "name": "Shift Supervisor",
  "description": "Can supervise a shift"
}
```

List staffing roles for a managed team:

```bash
curl http://localhost:8080/api/teams/1/staffing-roles \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
[
  {
    "id": 1,
    "teamId": 1,
    "name": "Shift Supervisor",
    "description": "Can supervise a shift"
  }
]
```

Only managers assigned to the requested team can create or list staffing roles.
Creating a duplicate staffing role name in the same team returns `409 Conflict`.

Assign a staffing role to an active employee in a managed team:

```bash
curl -X POST http://localhost:8080/api/teams/1/employees/2/staffing-roles \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"staffingRoleId":1}'
```

Expected response:

```json
{
  "id": 1,
  "teamId": 1,
  "teamMemberId": 1,
  "employeeId": 2,
  "employeeUsername": "employee1",
  "employeeFullName": "Demo Employee",
  "staffingRoleId": 1,
  "staffingRoleName": "Shift Supervisor",
  "assignedAt": "2026-07-18T07:43:40.000000Z"
}
```

List an employee's staffing roles in a managed team:

```bash
curl http://localhost:8080/api/teams/1/employees/2/staffing-roles \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```json
[
  {
    "id": 1,
    "teamId": 1,
    "teamMemberId": 1,
    "employeeId": 2,
    "employeeUsername": "employee1",
    "employeeFullName": "Demo Employee",
    "staffingRoleId": 1,
    "staffingRoleName": "Shift Supervisor",
    "assignedAt": "2026-07-18T07:43:40.000000Z"
  }
]
```

Only managers assigned to the requested team can assign or list employee staffing roles.
The employee must be an active member of the requested team.
The assigned staffing role must belong to the requested team.
Assigning the same staffing role to the same team member twice returns `409 Conflict`.
Assignment creation validates these employee staffing roles when a shift has a `requiredStaffingRoleId`.

## Verification

Run the fast unit suite without Docker:

```bash
mvn test
```

With Docker Desktop running, run the unit suite and PostgreSQL integration tests:

```bash
mvn verify -Ppostgres-it
```

The `postgres-it` profile runs `*IT` classes with Maven Failsafe. Testcontainers
starts a disposable PostgreSQL 16 instance on a random port, applies the real
Flyway migrations, and removes the container afterwards. Outbox dispatch and JMS
consumers are disabled. Demo seeding is disabled except in `DevelopmentDataSeederIT`,
which explicitly tests initialization and restart safety. These tests do not connect to
the development database or require the application servers to be running.
The first run downloads test dependencies and Docker images as needed.

`AssignmentConcurrencyIT` coordinates real overlapping transactions and checks
PostgreSQL lock waits. Its six scenarios cover manual/manual overlap,
minimum rest, manual/automatic overlap, automatic/automatic overlap, same-shift
capacity, and successful non-conflicting assignments. The unit tests also check
the deterministic lock order while preserving chronological automatic assignment.

Schedule writes first lock the owning team, then acquire any needed shift locks
and employee locks, with ascending IDs inside each group. Locks are held until
commit or rollback. Automatic assignment locks all candidate employees before
validation; this deliberately serializes runs that share employees, even across
different teams. Writes to different schedules in the same team also serialize.

`SwapRequestExecutionIT` adds twelve PostgreSQL scenarios: invalid transfer,
invalid second swap leg, successful full swap, successful/invalid employee-only
approval, duplicate manager approvals, overlapping transfers, cross-column active
request creation, legacy competing-request invalidation, cancellation versus
approval, stale ownership after transfer, and transfer versus manual assignment
in another team. Assertions read the committed database state; concurrency tests
also observe an actual PostgreSQL lock wait.

Request creation, approval, rejection, and cancellation first lock the source
team row. This intentionally serializes request writes within one team, including
requests sharing an assignment across the source/target columns. Unrelated teams
do not share that team lock. Final execution additionally locks all affected
shifts and both employees, ordered by ID, so shared employees coordinate with
manual/automatic assignments across teams. Locks last until the enclosing
service transaction completes.

The executor calls the non-transactional `AssignmentValidator` directly inside
that transaction. A caught validation failure can therefore persist
`INVALIDATED`, without an inner service transaction marking the entire operation
rollback-only. Both swap legs are validated before changing either owner.
Repeated approvals or approval after cancellation/invalidation return `409`;
validation failures during execution return the existing `200` response with
status `INVALIDATED`. The API contract and schema are unchanged.

`ScheduleValidationIT` adds eighteen scenarios for shift editing, readiness, and
publication. Negative cases cover availability, cross-schedule overlap, rest on
both sides, increased rest requirements, missing staffing roles, excess capacity,
and inactive membership. They verify stored-field rollback, unchanged assignment
owners, blocked publication with either confirmation value, and no outbox event
on failure. Positive cases cover qualified employees at exact rest/availability
boundaries, full shifts, confirmed open slots, and editing unassigned shifts.

`AvailabilityConcurrencyIT` adds ten scenarios: availability versus manual and
automatic assignment, transfer, and full swap in both commit orders; availability
deletion versus assignment; and duplicate deletion. Every scenario observes a
real PostgreSQL lock wait and checks the committed outcome. The eight availability
unit tests also check lock-before-validation/load ordering and lock-free listing.

Availability writes lock only the employee row, then read assignments or modify
constraints. They do not acquire team/shift write locks afterwards, preserving the
team-then-shifts-then-employees order used by request execution. The lock lasts
until transaction completion; it is not a JVM-only lock.

`ScheduleWorkflowConcurrencyIT` has 29 scenarios. Nine draft operations are
tested against publication in both commit orders: manual assignment, automatic
assignment, shift creation/editing/deletion, assignment deletion, template
generation, draft deletion, and publication itself. Other scenarios cover
reopening versus transfer/full-swap execution, assigned-shift edits versus
availability and cross-team assignment, unconfirmed publication after assignment
removal, assignment after reopening, and two edits submitted with the same
shift version (only the first saves). Tests observe database lock waits and
check committed shifts, owners, schedule status, and publication outbox events.
Four `ScheduleWriteLockTest` unit tests check ordering, employee deduplication,
and missing-entity handling after a wait.

`ScheduleWriteLock` uses the same team row as `SwapRequestLock`. It refreshes
schedule/shift/assignment state loaded before waiting, so draft-only writes do
not proceed after publication. Duplicate publication returns `409` without
another publication event; publication after committed draft deletion returns
`404`. Reopening before request execution results in `INVALIDATED` and unchanged
owners; execution committed first is preserved when reopening the schedule.

Assigned-shift editing and publication lock the assigned employees before
validating availability, overlap, and rest. Editing applies new fields only
after these locks are held. Publication reads assignments once and reuses that
protected list for readiness validation. The read-only readiness endpoint takes
no write lock and is only a current preview, not a reservation or permission to
skip validation when publishing.

`ShiftEditingIT` adds eleven HTTP/JSON checks through MockMvc with real PostgreSQL
transactions: stale edits, create/list/update versions, consecutive and no-op
saves, refresh after a conflict, missing/null/negative versions, deleted shifts,
and authorization. Authentication is supplied by Spring Security's test helper;
these are not login/JWT or browser tests. Unit checks also cover version comparison
after refresh, flush-before-response ordering, and optimistic-conflict error mapping.

This is not a complete concurrency protocol for every API. Client versions now
protect shift edits; draft/template and individual shift/assignment deletions
require a confirmation revision. Existing
template create/delete, slot creation, and generation now coordinate with each
other and with draft writes. Individual slot editing/deletion endpoints are not
implemented yet; future lifecycle controls must join this protocol. Request
refreshes map missing records to `404`, as verified in part 4.3c2 of
`../IMPLEMENTATION_PLAN.md`. Spring/JPA optimistic-lock failures map to
`409 STALE_VERSION`; expected pessimistic-lock/timeout failures map to
`409 CONCURRENT_MODIFICATION`. Unrelated unexpected errors remain `500`.
Future team/member/role writes must join the protocol when implemented. Demo
initialization only writes into an empty database and serializes concurrent
initializers with a transaction-scoped PostgreSQL advisory lock. It never mutates
existing workflow data. The shift version column already
exists; this API change needs no migration or new JMS event type.

`TemplateConcurrencyIT` has twelve PostgreSQL scenarios. They cover duplicate
names, deletion before generation/slot creation/duplicate deletion, generation
before deletion, both slot-creation/generation orders, repeated generation,
removing the last using draft, independent creation in another team, and a real
lock timeout through MockMvc, and stale deletion waiting for a slot addition.
Stored templates, slots, and shifts are checked.
The timeout test returns `409` and preserves the other transaction's generated
shifts; it uses test authentication, not a login/JWT or browser flow. Two new
lock-helper tests verify template refresh ordering and missing-template handling;
four error-handler cases cover Spring/JPA lock exception variants.

`DeletionPreconditionIT` adds fifteen API scenarios against PostgreSQL, using
MockMvc test authentication. It verifies preview counts, child edits/additions/
removals/replacements, publish/reopen changes, fresh deletion, `400` for invalid
revisions, `404` after deletion, authorization, and preservation of request
history. Rejected deletes leave the committed parent/child state unchanged.
Additional workflow/template tests observe actual lock waits before checking
stale revisions. `DeletionRevisionTest` covers stable ordering and fingerprint
identity/version/group boundaries. These are not authenticated browser tests.

## Important Notes

- `/api/health` is public.
- `/api/auth/login` is public.
- All other endpoints are protected by JWT authentication.
- Database tables are created by Flyway migrations under `src/main/resources/db/migration`.
- Hibernate is configured with `ddl-auto: validate`, so it validates that the Java entities match the database schema instead of creating tables automatically.
- The JWT secret in `application.yml` is for local development only and must be replaced before production use.

## Development Seed Data

Demo initialization is **disabled by default** (`app.seed.enabled=false`). For
the first start of a fresh local database, run:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--app.seed.enabled=true
```

Use plain `mvn spring-boot:run` afterwards. Even with the flag enabled, any existing
users, teams, or outbox records cause the entire initializer to skip. All other
business tables depend on users/teams through foreign keys; Flyway's schema
history is not application data. Legacy and partially populated databases are
preserved, not repaired or topped up. No new migration or database reset is needed
for this change, and existing accounts remain usable with their current credentials.

`DevelopmentDataSeeder` creates the complete scenario in one transaction. A
PostgreSQL transaction-level advisory lock serializes simultaneous initializers
before they check for existing data. A failure rolls back all seed rows and
allows a retry. The initializer does not identify or adopt existing schedules by
name/date, reset templates, restore transferred/deleted assignments, or recreate
deleted drafts. Dates are chosen once; restarting in another week does not add
another scenario. Demo credentials must not be used in a public deployment.

Seed users:

| Username | Role     | Password |
| -------- | -------- | -------- |
| manager1 | MANAGER  | password |
| employee1 | EMPLOYEE | password |
| employee2 | EMPLOYEE | password |
| employee3 | EMPLOYEE | password |
| employee4 | EMPLOYEE | password |
| employee5 | EMPLOYEE | password |
| employee6 | EMPLOYEE | password |
| employee7 | EMPLOYEE | password |
| employee8 | EMPLOYEE | password |

These users can be used with `POST /api/auth/login`.

Seed team:

| Team | Swap approval policy | Default rest hours | Time zone |
| ---- | -------------------- | ------------------ | --------- |
| צוות פיתוח | MANAGER | 8 | Asia/Jerusalem |

Additional demo data:

- Staffing roles: `Backend Developer`, `Frontend Developer`, and `QA Engineer`.
- Active staffing-role assignments: employees 1-3 have Backend, 4-6 have Frontend,
  and 7-8 have QA.
- One published schedule for the initialization week with three shifts and three
  assignments. The Frontend shift belongs to `employee4`, matching that employee's
  role; the Backend and transfer-source shifts belong to `employee1`.
- One empty seven-day draft schedule for manual assignment practice.
- One empty 21-day draft schedule for automatic assignment practice.
- One active `כיסוי פיתוח יומי בחירום` template with three eight-hour development coverage slots that repeat every day.
- Eight preloaded schedule-published notifications for active team members.
- One active transfer request from `employee1` to `employee2`.

The manager can generate 21 shifts from the seeded template into the seven-day
draft for manual assignment practice, or generate 63 shifts into the 21-day draft
for automatic assignment. This gives the React UI and Postman collection both a
small workflow and a realistic three-week scheduling scenario after explicit
initialization. The corrected role assignment only applies to new initialization;
existing database records are not rewritten.

Preloaded notifications are inserted directly by the notification service, not
delivered through JMS. The preloaded request likewise does not emit a creation
event. To demonstrate asynchronous delivery, perform a new publication or request
creation through the API/UI and observe the outbox, Artemis, and notifications.

`DevelopmentDataSeederTest` verifies the disabled default. PostgreSQL tests cover
initial creation, repeated runs, completed transfers, deletions, manual schedules
with matching dates, edited data, nonempty partial databases, failure rollback,
and concurrent initializers. The local development database is not used.

## Local Frontend Access

The backend allows browser requests from the local React development server:

```text
http://localhost:5173
http://127.0.0.1:5173
```

This is needed because browser-based requests are subject to CORS checks, unlike
Postman or curl.
