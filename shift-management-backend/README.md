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
- Development seed data.
- JWT-based login.
- Authenticated current-user endpoint: `GET /api/auth/me`.
- Managed teams endpoint: `GET /api/teams/me/managed`.
- Managed team employee endpoint: `GET /api/teams/{teamId}/employees`.
- CORS support for the local React development server.
- Initial schedule domain model.
- Schedule creation endpoint: `POST /api/schedules`.
- Initial shift domain model.
- Shift creation endpoint: `POST /api/schedules/{scheduleId}/shifts`.
- Shift list endpoint: `GET /api/schedules/{scheduleId}/shifts`.
- Shift update endpoint: `PUT /api/schedules/{scheduleId}/shifts/{shiftId}`.
- Shift delete endpoint: `DELETE /api/schedules/{scheduleId}/shifts/{shiftId}`.
- Initial assignment domain model.
- Manual assignment endpoint: `POST /api/assignments`.
- Assignment list endpoint: `GET /api/schedules/{scheduleId}/assignments`.
- Assignment delete endpoint: `DELETE /api/assignments/{assignmentId}`.
- Assignment validation for team membership, duplicate assignment, shift capacity, availability constraints, overlap, and minimum rest.
- Initial availability constraint domain model.
- Availability constraint creation endpoint: `POST /api/availability-constraints`.
- Personal availability constraint list endpoint: `GET /api/availability-constraints/me`.
- Availability constraint delete endpoint: `DELETE /api/availability-constraints/{constraintId}`.
- Availability constraint creation is rejected when it overlaps an existing assignment.
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
- Notification creation is idempotent by `eventId` and recipient.
- Transfer request persistence model.
- Transfer request creation endpoint: `POST /api/requests/transfers`.
- Outgoing transfer request list endpoint: `GET /api/requests/me/outgoing`.
- Incoming transfer request list endpoint: `GET /api/requests/me/incoming`.
- Pending manager approval transfer request list endpoint: `GET /api/requests/manager/pending`.
- Target employee approval endpoint: `POST /api/requests/{requestId}/employee-approve`.
- Target employee rejection endpoint: `POST /api/requests/{requestId}/employee-reject`.
- Requester cancellation endpoint: `POST /api/requests/{requestId}/cancel`.
- Transfer execution for teams with `EMPLOYEE` approval policy.
- Manager approval endpoint: `POST /api/requests/{requestId}/manager-approve`.
- Transfer execution for teams with `MANAGER` approval policy.
- Basic business logging for schedule, assignment, transfer request, outbox, and notification workflows.
- Unified JSON error responses for API and security errors.

Not implemented yet:

- Schedule list, update, and delete endpoints.
- Full shift swap endpoints.
- Automatic assignment.
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
- Business validation codes such as `SHIFT_OVERLAP`, `SHIFT_CAPACITY`, `MINIMUM_REST`, `TEAM_MEMBERSHIP`, and `STAFFING_ROLE_REQUIRED`.

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
    "name": "Operations",
    "swapApprovalPolicy": "MANAGER",
    "defaultMinRestHours": 8,
    "timeZone": "Asia/Jerusalem"
  }
]
```

Users who do not manage teams receive an empty list.

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
    "fullName": "Demo Employee One"
  },
  {
    "id": 3,
    "username": "employee2",
    "fullName": "Demo Employee Two"
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
  "teamName": "Operations",
  "startDate": "2026-07-05",
  "endDate": "2026-07-11",
  "status": "DRAFT",
  "publicationNumber": 0,
  "publishedAt": null
}
```

Only managers assigned to the requested team can create schedules for that team.

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
    "teamName": "Operations",
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
    "teamName": "Operations",
    "startDate": "2026-07-12",
    "endDate": "2026-07-18",
    "status": "PUBLISHED",
    "publicationNumber": 1,
    "publishedAt": "2026-07-20T08:59:10.000000Z"
  }
]
```

Users who do not manage teams receive an empty list.

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
  "teamName": "Operations",
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
  "teamName": "Operations",
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
    "teamName": "Operations",
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
    "teamName": "Operations",
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
    "teamName": "Operations",
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
    "message": "The Operations schedule was published.",
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
  "requiredStaffingRoleName": null
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
    "requiredStaffingRoleName": null
  }
]
```

Only managers assigned to the schedule's team can list shifts for now.

Update a shift inside a draft schedule:

```bash
curl -X PUT http://localhost:8080/api/schedules/1/shifts/1 \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"startTime":"2026-07-05T14:00:00Z","endTime":"2026-07-05T22:00:00Z","description":"Evening shift","requiredWorkers":3,"minRestHours":10,"requiredStaffingRoleId":null}'
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
  "requiredStaffingRoleName": null
}
```

Only managers assigned to the schedule's team can update shifts.
Shifts can be updated only while the schedule is still `DRAFT`.
Updated shift dates must stay inside the schedule date range according to the team's time zone.
Updating a shift with `requiredStaffingRoleId: null` clears the professional role requirement.

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
curl -X DELETE http://localhost:8080/api/assignments/1 \
  -H "Authorization: Bearer <TOKEN>"
```

Expected response:

```text
204 No Content
```

Only managers assigned to the assignment's team can delete assignments.
Assignments can be deleted only while the schedule is still `DRAFT`.

The assignment service currently validates, in order:

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

## Transfer Request Endpoints

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
  "requesterFullName": "Demo Employee One",
  "sourceAssignmentId": 1,
  "sourceShiftId": 1,
  "targetEmployeeId": 3,
  "targetEmployeeUsername": "employee2",
  "targetEmployeeFullName": "Demo Employee Two",
  "targetAssignmentId": null,
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
6. Only one active request can exist for the same source assignment.

List transfer requests created by the authenticated employee:

```bash
curl http://localhost:8080/api/requests/me/outgoing \
  -H "Authorization: Bearer <TOKEN>"
```

List transfer requests targeting the authenticated employee:

```bash
curl http://localhost:8080/api/requests/me/incoming \
  -H "Authorization: Bearer <TOKEN>"
```

List transfer requests waiting for manager approval in teams managed by the authenticated manager:

```bash
curl http://localhost:8080/api/requests/manager/pending \
  -H "Authorization: Bearer <TOKEN>"
```

Transfer request list endpoints return the same response shape as transfer creation.
Employee list endpoints are limited to the authenticated employee, and the manager
list endpoint is limited to teams managed by the authenticated manager.

Approve an incoming transfer request as the target employee:

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
  "requesterFullName": "Demo Employee One",
  "sourceAssignmentId": 1,
  "sourceShiftId": 1,
  "targetEmployeeId": 3,
  "targetEmployeeUsername": "employee2",
  "targetEmployeeFullName": "Demo Employee Two",
  "targetAssignmentId": null,
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
3. If the team's approval policy is `EMPLOYEE`, the request status becomes `APPROVED` and the source assignment moves to the target employee.
4. If the team's approval policy is `MANAGER`, the request status becomes `PENDING_MANAGER`.
5. For `EMPLOYEE` policy teams, the backend re-runs transfer eligibility checks before moving the assignment.

If transfer eligibility fails during an `EMPLOYEE` policy approval, the request becomes `INVALIDATED` and the assignment remains unchanged.

Reject an incoming transfer request as the target employee:

```bash
curl -X POST http://localhost:8080/api/requests/1/employee-reject \
  -H "Authorization: Bearer <TOKEN>"
```

Employee rejection rules:

1. Only the target employee can reject the request.
2. Requests can be rejected only while their status is `PENDING_EMPLOYEE`.
3. Rejection changes the request status to `REJECTED`.
4. Rejection never changes the assignment.

Cancel an active transfer request as the requester:

```bash
curl -X POST http://localhost:8080/api/requests/1/cancel \
  -H "Authorization: Bearer <TOKEN>"
```

Cancellation rules:

1. Only the employee who created the request can cancel it.
2. Requests can be cancelled only while their status is `PENDING_EMPLOYEE` or `PENDING_MANAGER`.
3. Cancellation changes the request status to `CANCELLED`.
4. Cancellation never changes the assignment.

Approve a pending transfer request as a manager:

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
  "requesterFullName": "Demo Employee One",
  "sourceAssignmentId": 1,
  "sourceShiftId": 1,
  "targetEmployeeId": 3,
  "targetEmployeeUsername": "employee2",
  "targetEmployeeFullName": "Demo Employee Two",
  "targetAssignmentId": null,
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
4. Before moving the assignment, the backend re-runs transfer eligibility checks for the target employee.

If transfer eligibility fails during manager approval, the request becomes `INVALIDATED` and the assignment remains unchanged.

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

## Important Notes

- `/api/health` is public.
- `/api/auth/login` is public.
- All other endpoints are protected by JWT authentication.
- Database tables are created by Flyway migrations under `src/main/resources/db/migration`.
- Hibernate is configured with `ddl-auto: validate`, so it validates that the Java entities match the database schema instead of creating tables automatically.
- The JWT secret in `application.yml` is for local development only and must be replaced before production use.

## Development Seed Data

When `app.seed.enabled=true`, the application inserts development demo data.
The seeder is idempotent: it reuses existing demo records when possible and creates only missing records.

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

Additional demo data:

- Staffing roles: `Cashier` and `Shift Lead`.
- Active staffing-role assignments for the demo employees.
- One published schedule for the current week with shifts and assignments.
- One draft schedule for the next week so the manager screens have editable data.
- Schedule-published notifications for active team members.
- One active transfer request from `employee1` to `employee2`.

This gives the React UI and Postman collection useful data immediately after local startup.

## Local Frontend Access

The backend allows browser requests from the local React development server:

```text
http://localhost:5173
http://127.0.0.1:5173
```

This is needed because browser-based requests are subject to CORS checks, unlike
Postman or curl.
