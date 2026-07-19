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

Not implemented yet:

- Schedule list, update, delete, publish, and reopen endpoints.
- Assignment transfer endpoints.
- Automatic assignment.
- Remaining team-scoped authorization for future manager workflows.

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
