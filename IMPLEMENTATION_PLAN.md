# Implementation Plan - Shift Management System

Last updated: 2026-07-22

This document is the working implementation plan for the project.  
The goal is to build the system in small, understandable steps, while keeping each part easy to explain during the final presentation.

## Working Sources

- `spec-revised.md` - main specification.
- `spec.md` - older specification, used only for comparison when needed.
- Course instructions, initial design documents, and screen mockups are local reference materials and should not be committed unless converted to English project documentation.

## Work Principles

Each step should follow the same pattern:

1. Understand the concept.
2. Implement one small piece.
3. Verify that it works.
4. Document what was built and how to test it.

Do not move to the next step until the current step can be explained clearly:

- Which classes or files were added.
- What each layer is responsible for.
- How data flows from the UI to the database.
- Which edge cases are handled and which are still open.

## Repository Language Policy

All files committed to Git should be written in English:

- File and directory names.
- Code identifiers.
- Code comments.
- README files and project documentation.
- Commit messages.

Local Hebrew reference materials can stay on the computer, but they should not be committed unless they are converted into English project documentation.

## First Version Goal

The first version should not include the whole system.  
The first working milestone is a small but real backend:

- Login.
- Users and teams.
- Schedule creation.
- Shift creation.
- Manual employee assignment.
- Basic assignment validations: team membership, capacity, overlap, and rest period.

After this version works and is understood, the project can grow into constraints, staffing roles, swaps, notifications, templates, and the full React UI.

## Phase 0 - Project Setup

Goal: create a clean foundation for development.

### Understand

- What the backend is responsible for.
- What the frontend is responsible for.
- What Spring Boot provides.
- What React provides.
- Why PostgreSQL is used.
- Why Git should be used from the beginning.

### Implement

- [ ] Initialize a Git repository.
- [x] Create a basic backend `README.md`.
- [x] Create an empty Spring Boot backend project.
- [ ] Create an empty React frontend project.
- [x] Configure local PostgreSQL.
- [x] Configure `application.yml`.
- [x] Verify that the backend starts.
- [ ] Verify that the frontend starts.

### Verify

- [ ] Backend runs without errors.
- [ ] Frontend runs without errors.
- [x] A simple health endpoint exists: `GET /api/health`.
- [x] The health endpoint can be opened in the browser or tested with Postman.

### Document

- How to run the backend.
- How to run the frontend.
- How to connect to the database.

## Phase 1 - Users, Teams, And Basic Authorization

Goal: build the foundation of the system: users, teams, employee membership, and manager ownership.

### Understand

- The difference between `EMPLOYEE` and `MANAGER`.
- The difference between an application role and a staffing role.
- Many-to-many relationships through join tables.
- What a Spring Data JPA repository does.

### Implement

- [x] Create `User`.
- [x] Create `Team`.
- [x] Create `TeamMember`.
- [x] Create `TeamManager`.
- [x] Create `ApplicationRole`.
- [x] Create repositories for these entities.
- [x] Add initial seed data for development.

### Verify

- [x] Users can be saved.
- [x] Teams can be saved.
- [x] An employee can be assigned to a team.
- [x] A manager can be assigned to a team.
- [ ] Duplicate team membership is blocked.

### Document

- A short relationship diagram.
- Why both `team_members` and `team_managers` are needed.

## Phase 2 - Login And Security

Goal: allow users to log in and make sure each user can access only authorized data.

### Understand

- What JWT is.
- What Spring Security does.
- The difference between authentication and authorization.
- Why controllers should not contain business logic.

### Implement

- [x] Store encrypted passwords.
- [x] Implement `POST /api/auth/login`.
- [x] Return a JWT after successful login.
- [x] Protect every endpoint except login and health.
- [ ] Add basic role checks.
- [x] Add login request and response DTOs.

### Verify

- [x] A valid user receives a token.
- [x] An invalid password returns an error.
- [x] A request without a token is rejected.
- [ ] An employee cannot access manager actions.
- [ ] A manager can access manager actions only for managed teams.

### Document

- The login flow.
- Where the token is stored on the client.
- Which endpoints are public and which are protected.

## Phase 3 - Schedules And Shifts

Goal: allow managers to create schedules and define shifts.

### Understand

- What `Schedule` represents.
- What `Shift` represents.
- The difference between `DRAFT` and `PUBLISHED`.
- Why shifts need full `startTime` and `endTime` values.

### Implement

- [x] Create `Schedule`.
- [x] Create `Shift`.
- [x] Create `ScheduleStatus`.
- [x] Allow managers to create draft schedules.
- [x] Allow managers to list shifts.
- [x] Allow managers to create shifts.
- [x] Allow managers to edit shifts.
- [x] Allow managers to delete shifts.
- [x] Validate that `endTime` is after `startTime`.

### Verify

- [x] A manager can create a schedule for a managed team.
- [x] A manager cannot create a schedule for an unmanaged team.
- [x] A shift cannot end before it starts.
- [x] An employee cannot edit shifts.

### Document

- The `schedules` and `shifts` tables.
- The endpoints added in this phase.

### Phase 3 Design Decisions

`Schedule` represents a team's plan for an inclusive date range.

Initial fields:

- `id`
- `team`
- `startDate`
- `endDate`
- `status`
- `publicationNumber`
- `publishedAt`
- `version`

`ScheduleStatus` values:

- `DRAFT`
- `PUBLISHED`

Rules for this phase:

- New schedules start as `DRAFT`.
- `endDate` must not be before `startDate`.
- Managers may create schedules only for teams they manage.
- Employees should not see draft schedules.
- Publish/reopen behavior is planned for Phase 7, but the fields are included now because they are part of the core schedule lifecycle.

`Shift` represents one actual shift inside a schedule.

Initial fields:

- `id`
- `schedule`
- `startTime`
- `endTime`
- `description`
- `requiredWorkers`
- `minRestHours`
- `version`

Rules for this phase:

- `endTime` must be after `startTime`.
- Shift dates must be inside the schedule date range, using the team's time zone.
- `requiredWorkers` must be positive.
- `minRestHours` must be non-negative.
- Managers may create, update, and delete shifts only inside draft schedules for teams they manage.
- Shift times will be stored as `Instant` values in the backend. The team's `timeZone` will later be used by the frontend for display.

Deferred from Phase 3:

- `templateSlot` reference, because templates are Phase 10.
- `parentShift` reference and split-shift workflow, because split support can be added after basic shift CRUD.
- `requiredStaffingRole`, because staffing roles are Phase 6.
- Assignment counts and capacity indicators, because assignments are Phase 4.
- Publish/reopen endpoints, because the full schedule lifecycle is Phase 7.

## Phase 4 - Manual Assignment

Goal: allow managers to assign employees to shifts with the first business rules.

This is a central phase. Work slowly and keep it easy to explain.

### Understand

- What `Assignment` represents.
- What shift capacity means.
- The time-overlap formula.
- How minimum rest between shifts is calculated.
- Why assignment rules belong in a service, not in a controller.

### Implement

- [x] Create `Assignment`.
- [x] Add a unique constraint on employee and shift.
- [x] Create `AssignmentService`.
- [x] Implement `POST /api/assignments`.
- [x] Validate team membership.
- [x] Validate shift capacity.
- [x] Validate overlapping assignments.
- [x] Validate minimum rest.
- [x] Return a unified error response when validation fails.

### Verify

- [x] A valid assignment is saved.
- [x] A non-team employee cannot be assigned.
- [x] An employee cannot be assigned twice to the same shift.
- [x] An employee cannot be assigned to overlapping shifts.
- [x] An employee cannot be assigned without enough rest.
- [x] The error message is clear.

### Document

- [x] The assignment validation order.
- [x] Examples of successful and failed assignments.
- [x] Where the business logic lives.

### Phase 4 Design Decisions

`Assignment` represents one employee assigned to one shift.

Initial fields:

- `id`
- `shift`
- `employee`
- `assignedAt`
- `version`

Initial endpoint:

- `POST /api/assignments`
- `GET /api/schedules/{scheduleId}/assignments`
- `DELETE /api/assignments/{assignmentId}`

Initial request body:

```json
{
  "shiftId": 1,
  "employeeId": 2
}
```

Rules for this phase:

- Only a manager of the shift's team may create assignments.
- Assignments can be created only while the schedule is `DRAFT`.
- Only a manager of the schedule's team may list assignments for that schedule.
- Assignments can be deleted only while the schedule is `DRAFT`.
- The employee must be an active member of the shift's team.
- The same employee cannot be assigned twice to the same shift.
- A shift cannot exceed `requiredWorkers`.
- Overlap checks use the half-open range rule from the specification.
- Rest checks compare the nearest previous and next assignments across all teams.
- The required rest between adjacent shifts is `max(previous.minRestHours, next.minRestHours)`.

Current assignment validation error shape:

```json
{
  "code": "SHIFT_CAPACITY",
  "message": "Shift has no available assignment slots"
}
```

Stable assignment validation codes added in this phase:

- `SCHEDULE_NOT_DRAFT`
- `TEAM_MEMBERSHIP`
- `DUPLICATE_ASSIGNMENT`
- `SHIFT_CAPACITY`
- `AVAILABILITY_CONFLICT`
- `SHIFT_OVERLAP`
- `MINIMUM_REST`

Deferred from Phase 4:

- Assignment move endpoint.
- Availability constraint checks, because availability is Phase 5.
- Staffing-role checks, because staffing roles are Phase 6.
- Full scheduling concurrency protection, which will be revisited before automatic assignment and swaps.

## Phase 5 - Availability Constraints

Goal: allow employees to declare unavailable time ranges and prevent conflicting assignments.

### Understand

- An availability constraint in this project means unavailability.
- Constraints apply across all teams.
- The backend is the final authority even if the frontend also validates input.

### Implement

- [x] Create `AvailabilityConstraint`.
- [x] Create `AvailabilityConstraintRepository`.
- [x] Add a database migration for availability constraints.
- [x] Allow an employee to create a constraint.
- [x] Allow an employee to view personal constraints.
- [x] Allow an employee to delete personal constraints.
- [x] Block a constraint that overlaps an existing assignment.
- [x] Add constraint validation to assignment creation.

### Verify

- [x] An employee can create a full-day constraint.
- [x] An employee can create a time-range constraint.
- [x] An employee cannot view another employee's constraints.
- [x] An employee cannot create a constraint that overlaps an existing assignment.
- [x] A manager cannot assign an employee during an unavailable time range.
- [x] Invalid time ranges are rejected by the domain model.

### Document

- How a full-day constraint is stored.
- How constraint and shift overlap is checked.

### Phase 5 Design Decisions

An `AvailabilityConstraint` represents employee unavailability.

Initial fields:

- `id`
- `employee`
- `startTime`
- `endTime`
- `reason`
- `createdAt`
- `version`

Rules for the persistence step:

- Time ranges are stored as `Instant` values, matching shifts and assignments.
- `endTime` must be after `startTime`.
- `reason` is optional and limited to 500 characters.
- Full-day constraints will be stored as the start of the local day through the start of the next local day.
- Constraint overlap uses the same half-open range formula as assignment overlap.

Initial endpoints:

- `POST /api/availability-constraints`
- `GET /api/availability-constraints/me`
- `DELETE /api/availability-constraints/{constraintId}`

Rules for the first API step:

- A user creates availability constraints only for the authenticated account.
- A user lists only the authenticated account's own constraints.
- A user deletes only the authenticated account's own constraints.
- Deleting another user's constraint returns `404 Not Found`.
- Invalid time ranges return `400 Bad Request`.
- Creating a constraint that overlaps one of the authenticated user's existing assignments returns `409 Conflict`.
- Creating an assignment that overlaps one of the employee's availability constraints returns `409 Conflict` with code `AVAILABILITY_CONFLICT`.

## Phase 6 - Staffing Roles

Goal: support professional scheduling roles such as shift supervisor or entrance guard.

### Understand

- A staffing role is not the same as `MANAGER` or `EMPLOYEE`.
- A staffing role belongs to one team.
- An employee can have a staffing role in one team but not another.

### Implement

- [x] Create `StaffingRole`.
- [x] Connect team members to staffing roles.
- [x] Allow managers to create team staffing roles.
- [x] Allow managers to assign staffing roles to employees.
- [x] Add an optional required staffing role to shifts.
- [x] Add staffing-role validation to assignment creation.

### Verify

- [x] A manager can create a staffing role for a managed team.
- [x] A manager can assign a staffing role to an active team member.
- [x] A manager cannot assign a role from another team to an employee.
- [x] An employee with the required role can be assigned.
- [x] An employee without the required role is rejected.
- [x] A role from another team does not count.

### Document

- The difference between application roles and staffing roles.
- Example staffing roles in the system.

### Phase 6 Design Decisions

`StaffingRole` represents a team-specific professional role used for scheduling.

It is different from `ApplicationRole`:

- `ApplicationRole` controls broad system permissions, such as `MANAGER` or `EMPLOYEE`.
- `StaffingRole` describes what work an employee is qualified to cover inside one team, such as `Shift Supervisor` or `Entrance Guard`.

Initial fields:

- `id`
- `team`
- `name`
- `description`
- `version`

`TeamMemberStaffingRole` represents one staffing role assigned to one team member.

Initial fields:

- `id`
- `teamMember`
- `staffingRole`
- `assignedAt`
- `version`

Initial persistence rules:

- A staffing role belongs to exactly one team.
- The role name is required and trimmed by the domain model.
- The same team cannot have two staffing roles with the same name.
- Different teams may use the same role name independently.
- A team member can be connected to staffing roles only from the same team.
- The same staffing role cannot be assigned twice to the same team member.
- A shift can optionally require one staffing role.
- A shift without a required staffing role remains a general shift.
- A required staffing role on a shift must belong to the schedule's team.
- Assignment creation validates required staffing roles only when a shift has a professional role requirement.
- A role assignment from another team does not satisfy a shift requirement.

Staffing role management endpoints:

- `POST /api/teams/{teamId}/staffing-roles`
- `GET /api/teams/{teamId}/staffing-roles`

Rules for the first staffing role API step:

- Only a manager of the requested team can create or list staffing roles.
- Creating a duplicate staffing role name in the same team returns `409 Conflict`.
- Role names are trimmed before duplicate checking and saving.

Employee staffing role endpoints:

- `POST /api/teams/{teamId}/employees/{employeeId}/staffing-roles`
- `GET /api/teams/{teamId}/employees/{employeeId}/staffing-roles`

Rules for employee staffing role assignment:

- Only a manager of the requested team can assign or list an employee's staffing roles for that team.
- The employee must be an active member of the requested team.
- The staffing role must belong to the requested team.
- Assigning the same staffing role to the same team member twice returns `409 Conflict`.

Rules for required staffing roles on shifts:

- `requiredStaffingRoleId` is optional in shift create/update requests.
- A missing or `null` `requiredStaffingRoleId` means the shift has no professional role requirement.
- A non-null `requiredStaffingRoleId` must point to a staffing role from the schedule's team.
- Shift responses include `requiredStaffingRoleId` and `requiredStaffingRoleName`.

Rules for assignment validation against staffing roles:

- A shift without a required staffing role keeps the existing general assignment behavior.
- A shift with a required staffing role can receive only employees who have that role in the shift's team.
- Missing required staffing roles return `409 Conflict` with code `STAFFING_ROLE_REQUIRED`.
- The staffing-role check runs after active team membership validation and before duplicate assignment, capacity, availability, overlap, and rest checks.

## Phase 7 - Schedule Publication

Goal: turn a draft schedule into an official schedule visible to employees.

### Understand

- The schedule lifecycle.
- Why employees cannot see drafts.
- Why editing a published schedule requires reopening it.

### Implement

- [x] Add `publish`.
- [x] Add `reopen`.
- [x] Add `publicationNumber`.
- [x] Block direct shift and assignment edits after publication.
- [x] Allow employees to list published schedules for active teams.
- [x] Allow employees to view published schedule details with shifts and assignments.
- [ ] Return a report before publication.
- [ ] Allow publication with unfilled shifts only after explicit confirmation.

### Verify

- [x] A new schedule starts as `DRAFT`.
- [x] Publishing changes a schedule from `DRAFT` to `PUBLISHED`.
- [x] Reopening changes a schedule from `PUBLISHED` to `DRAFT`.
- [x] After publication, employees can list the schedule.
- [x] Employees can view published schedule details and shifts.
- [x] After publication, managers cannot edit shifts or assignments until reopening.
- [x] Republishing increments `publicationNumber`.

### Document

- A short state diagram for `Schedule`.
- Which actions are allowed in each state.

### Phase 7 Design Decisions

Phase 7 currently supports the basic schedule lifecycle and employee-facing published schedule viewing.

Current endpoints:

- `POST /api/schedules/{scheduleId}/publish`
- `POST /api/schedules/{scheduleId}/reopen`
- `GET /api/schedules/me/published`
- `GET /api/schedules/me/published/{scheduleId}`

Rules for publication:

- Only a manager of the schedule's team can publish the schedule.
- Only `DRAFT` schedules can be published.
- Publishing changes the schedule status to `PUBLISHED`.
- Publishing records `publishedAt`.
- Publishing increments `publicationNumber`.
- Direct shift changes and assignment changes are already blocked once the schedule is `PUBLISHED`.

Rules for reopening:

- Only a manager of the schedule's team can reopen the schedule.
- Only `PUBLISHED` schedules can be reopened.
- Reopening changes the schedule status back to `DRAFT`.
- Reopening does not increment `publicationNumber`.
- Reopening does not clear `publishedAt`, so the latest publication timestamp remains visible.
- Publishing a reopened schedule again increments `publicationNumber`.

Rules for the employee published schedule list:

- The authenticated user sees only schedules with status `PUBLISHED`.
- The authenticated user sees only schedules for teams where they are an active team member.
- Users with no active team memberships receive an empty list.
- Draft schedules are not returned.
- The list endpoint returns schedule headers only.

Rules for the employee published schedule details:

- The authenticated user can open only schedules with status `PUBLISHED`.
- The authenticated user can open only schedules from teams where they are an active team member.
- Draft schedules and schedules from unrelated teams return `404 Not Found`.
- The details response includes the schedule header, shifts, and published assignment information for each shift.

Deferred from the first Phase 7 steps:

- Publication readiness report.
- Explicit confirmation for publishing with unfilled shifts.

## Phase 8 - Basic Frontend

Goal: build a minimal React interface connected to the backend.

### Understand

- How React calls a REST API.
- How login state is stored.
- How employee screens and manager screens are separated.

### Implement

- [ ] Login screen.
- [ ] Role-based navigation.
- [ ] Schedule view.
- [ ] Manager schedule creation screen.
- [ ] Manager shift creation screen.
- [ ] Simple manual assignment screen.
- [ ] Display backend errors clearly.

### Verify

- [ ] Successful login navigates to the correct area.
- [ ] Frontend can call the published schedules backend endpoint.
- [ ] Employees can view published schedule details in the frontend.
- [ ] Managers can create shifts.
- [ ] Assignment errors are displayed clearly.

### Document

- Existing screens.
- For each screen: who can use it and what it does.

## Phase 9 - Transfer And Swap Requests

Goal: allow employees to transfer a shift or request a shift swap.

### Understand

- The difference between transfer and swap.
- What a state machine is.
- Why all assignment rules must be checked again at final approval time.

### Implement

- [ ] Create `SwapRequest`.
- [ ] Start with transfer only.
- [ ] Add statuses: `PENDING_EMPLOYEE`, `APPROVED`, `REJECTED`, `CANCELLED`, `INVALIDATED`.
- [ ] Allow the target employee to approve or reject.
- [ ] Run assignment validations before final approval.
- [ ] Move the assignment to the target employee after transfer approval.
- [ ] Add full swap.
- [ ] Add manager approval according to team policy.

### Verify

- [ ] An employee can create a transfer request.
- [ ] The target employee can approve it.
- [ ] Another employee cannot approve a request not addressed to them.
- [ ] A request becomes invalidated if assignment is no longer possible.
- [ ] A swap exchanges two assignments.

### Document

- Request statuses and transitions.
- Transfer and swap examples.

## Phase 10 - Templates And Automatic Assignment

Goal: generate shifts from templates and offer automatic assignment.

### Understand

- What a shift template is.
- What a template slot is.
- How template slots generate shifts over a date range.
- Why automatic assignment does not need to find a globally optimal solution.

### Implement

- [ ] Create `ShiftTemplate`.
- [ ] Create `TemplateSlot`.
- [ ] Allow managers to create templates.
- [ ] Allow managers to create slots.
- [ ] Generate shifts from a template.
- [ ] Implement basic automatic assignment.
- [ ] Rank employees by fewer assigned hours.
- [ ] Return a report of unassigned shifts.

### Verify

- [ ] A template creates shifts on the expected dates.
- [ ] Automatic assignment assigns only eligible employees.
- [ ] A shift remains unfilled if no employee is eligible.
- [ ] The report explains what was not assigned.

### Document

- The automatic assignment algorithm.
- The algorithm limitations.

## Phase 11 - Notifications And JMS

Goal: add internal notifications and satisfy the JMS requirement.

### Understand

- What asynchronous messaging means.
- The difference between saving a notification and sending an event.
- What a transactional outbox is.
- Why JMS does not communicate directly with the browser.

### Implement

- [ ] Create `Notification`.
- [ ] Create a basic notification screen.
- [ ] Create direct notifications when a schedule is published.
- [ ] Create `NotificationOutbox`.
- [ ] Add a dispatcher that sends outbox events to JMS.
- [ ] Add a consumer that creates notifications.
- [ ] Ensure idempotency with `eventId`.

### Verify

- [ ] Publishing a schedule creates a notification.
- [ ] A user sees only personal notifications.
- [ ] A notification can be marked as read.
- [ ] Reprocessing the same event does not create duplicates.

### Document

- Event flow: publish schedule -> outbox -> JMS -> notification.
- Existing notification types.

## Phase 12 - Testing, Hardening, And Submission

Goal: make the project ready for submission and presentation.

### Understand

- The difference between unit tests and integration tests.
- Why concurrency tests matter in this project.
- How to explain the code during the presentation.

### Implement

- [ ] Unit tests for assignment rules.
- [ ] Unit tests for overlap and rest calculations.
- [ ] Repository integration tests.
- [ ] Basic security tests.
- [ ] Unified error handling with `ControllerAdvice`.
- [ ] Basic logging.
- [ ] Seed data for the demo.

### Verify

- [ ] All tests pass.
- [ ] The project can be run on a clean computer using the installation guide.
- [ ] A full demo script exists.
- [ ] All central screens work.

### Document

- Updated design document.
- User guide.
- Installation guide.
- Explanation of central classes and functions.

## Progress Log Template

Use this template at the end of every completed step:

```text
Date:
Phase:
Implemented:
Main files/classes:
How it was tested:
Still open:
```

## Progress Log

### 2026-06-26

- Created the initial implementation plan.
- Decided to start with a small MVP: login, teams, schedules, shifts, and manual assignment.
- `spec-revised.md` will be the main specification source.
- Created the initial backend in `shift-management-backend`.
- Added `pom.xml`, main application class, `SecurityConfig`, `HealthController`, `application.yml`, and `compose.yml`.
- The backend has not been executed yet because Java, Maven, Gradle, and Docker are not currently available in the terminal.

### 2026-06-27

- Added Phase 1 domain model: `User`, `Team`, `TeamMember`, and `TeamManager`.
- Added enums: `ApplicationRole` and `SwapApprovalPolicy`.
- Added repositories for users, teams, team members, and team managers.
- Added Flyway migration `V1__create_users_and_teams.sql`.
- Changed Hibernate from schema creation to schema validation with `ddl-auto: validate`.
- Added development seed data for `manager1`, `employee1`, `employee2`, and the `Operations` team.
- Verified `mvn test` succeeds.
- Verified the Spring Boot app starts on port `8081` when `8080` is busy.
- Verified `GET /api/health` returns `UP`.
- Verified seed users exist in PostgreSQL.

### 2026-06-27 - Phase 2 Start

- Added JWT dependencies with JJWT.
- Added JWT configuration under `app.jwt`.
- Added `JwtService` for token creation and parsing.
- Added `JwtAuthenticationFilter` to read bearer tokens.
- Replaced temporary HTTP Basic security with stateless JWT security.
- Added `AuthController`, `AuthService`, `LoginRequest`, and `LoginResponse`.
- Added `POST /api/auth/login`.
- Added `GET /api/auth/me` for checking the current authenticated user.
- Verified `mvn test` succeeds.
- Verified `GET /api/health` is public.
- Verified `GET /api/auth/me` returns `401` without a token.
- Verified invalid login returns `401`.
- Verified `manager1/password` can log in and access `/api/auth/me`.
- Verified `employee1/password` can log in and returns role `EMPLOYEE`.
- Role-specific manager endpoint checks are still open because manager endpoints do not exist yet.

### 2026-06-30 - Phase 3 Planning

- Reviewed Phase 2 status and confirmed that role-specific checks remain open until manager endpoints exist.
- Reviewed schedule lifecycle, `schedules` table, `shifts` table, and schedule/shift API requirements from `spec-revised.md`.
- Added Phase 3 design decisions for `Schedule`, `Shift`, and `ScheduleStatus`.
- Decided to keep Phase 3 focused on basic schedules and shift CRUD.
- Deferred template slots, split shifts, staffing-role requirements, assignment capacity indicators, and publish/reopen workflows to later phases.

### 2026-06-30 - Phase 3 Start

- Added `ScheduleStatus` with `DRAFT` and `PUBLISHED`.
- Added `Schedule` entity linked to `Team`.
- Added `ScheduleRepository`.
- Added Flyway migration `V2__create_schedules.sql`.
- Added a focused unit test for the default schedule state and date-range validation.
- Verified `mvn test` succeeds.
- Verified the Spring Boot app starts on port `8081`.
- Verified Flyway migrated the database to version 2 and created the `schedules` table.
- Verified `GET /api/health` returns `UP` after the migration.
- Kept schedule creation endpoints for the next Phase 3 step.

### 2026-07-05 - Phase 3 Schedule API

- Added `CreateScheduleRequest`.
- Added `ScheduleResponse`.
- Added `ScheduleService`.
- Added `ScheduleController`.
- Added `POST /api/schedules` for creating draft schedules.
- Added team-manager authorization for schedule creation.
- Allowed Spring Boot's technical `/error` path through security so business errors return the correct status codes.
- Added service tests for successful creation, unmanaged-team rejection, and invalid date-range rejection.
- Verified `mvn test` succeeds.
- Verified the Spring Boot app starts on port `8081`.
- Verified `POST /api/schedules` returns `201` for `manager1`.
- Verified `POST /api/schedules` returns `403` for `employee1`.
- Verified invalid schedule dates return `400`.

### 2026-07-05 - Phase 3 Shift Model

- Added `Shift` entity linked to `Schedule`.
- Added `ShiftRepository`.
- Added Flyway migration `V3__create_shifts.sql`.
- Added validation rules for shift time range, required workers, and minimum rest hours.
- Added unit tests for valid shift creation and invalid shift rules.
- Verified `mvn test` succeeds.
- Verified the Spring Boot app starts on port `8081`.
- Verified Flyway migrated the database to version 3 and created the `shifts` table.
- Kept shift creation endpoints for the next Phase 3 step.

### 2026-07-05 - Phase 3 Shift API

- Added `CreateShiftRequest`.
- Added `ShiftResponse`.
- Added `ShiftService`.
- Added `ShiftController`.
- Added `POST /api/schedules/{scheduleId}/shifts` for creating shifts inside draft schedules.
- Added `GET /api/schedules/{scheduleId}/shifts` for listing shifts in a schedule.
- Added `PUT /api/schedules/{scheduleId}/shifts/{shiftId}` for updating shifts inside draft schedules.
- Added `DELETE /api/schedules/{scheduleId}/shifts/{shiftId}` for deleting shifts inside draft schedules.
- Added team-manager authorization for shift creation.
- Added team-manager authorization for shift listing.
- Added team-manager authorization for shift updates.
- Added team-manager authorization for shift deletion.
- Added validation that shift `endTime` must be after `startTime`.
- Added validation that shift dates must be inside the schedule date range using the team's time zone.
- Added validation that shifts can be created, updated, and deleted only while the schedule is `DRAFT`.
- Added service tests for successful creation, managed schedule listing, successful update, successful deletion, unmanaged-schedule rejection, published-schedule rejection, wrong-schedule rejection, and invalid time-range rejection.
- Verified `mvn test` succeeds.
- Verified the Spring Boot app starts on port `8081`.
- Verified `POST /api/schedules/{scheduleId}/shifts` returns `201` for `manager1`.
- Verified `POST /api/schedules/{scheduleId}/shifts` returns `403` for `employee1`.
- Verified invalid shift times return `400`.
- Verified `GET /api/schedules/{scheduleId}/shifts` returns the created shift for `manager1`.
- Verified `GET /api/schedules/{scheduleId}/shifts` returns `403` for `employee1`.
- Verified `PUT /api/schedules/{scheduleId}/shifts/{shiftId}` returns updated shift details for `manager1`.
- Verified `PUT /api/schedules/{scheduleId}/shifts/{shiftId}` returns `403` for `employee1`.
- Verified invalid shift update times return `400`.
- Verified `DELETE /api/schedules/{scheduleId}/shifts/{shiftId}` returns `204` for `manager1`.
- Verified `DELETE /api/schedules/{scheduleId}/shifts/{shiftId}` returns `403` for `employee1`.
- Verified deleted shifts no longer appear in `GET /api/schedules/{scheduleId}/shifts`.

### 2026-07-08 - Phase 3 Shift Date-Range Validation

- Added validation that created and updated shifts must fit inside the schedule date range.
- The validation uses the team's `timeZone` when converting shift `Instant` values to local dates.
- Treated `endTime` as the exclusive end of the shift, so a shift may end exactly at midnight after the schedule's last date.
- Added tests for shifts before the schedule, after the schedule, and the midnight boundary case.
- Verified `mvn test` succeeds.
- Verified the Spring Boot app starts on port `8081`.
- Verified valid shift creation returns `201`.
- Verified shift creation before the schedule range returns `400`.
- Verified shift update after the schedule range returns `400`.

### 2026-07-06 - Shift Package Organization

- Moved shift-related classes from `schedule` to a dedicated `shift` package.
- Moved shift-related tests from `schedule` to a dedicated `shift` test package.
- Kept existing shift endpoints and behavior unchanged.
- Verified `mvn test` succeeds.
- Verified the Spring Boot app starts on port `8081`.
- Verified schedule creation, shift creation, and shift listing still work after the package move.

### 2026-07-11 - Phase 4 Manual Assignment

- Added `Assignment` entity linked to `Shift` and `User`.
- Added Flyway migration `V4__create_assignments.sql`.
- Added `AssignmentRepository`.
- Added `CreateAssignmentRequest` and `AssignmentResponse`.
- Added `AssignmentService`.
- Added `AssignmentController`.
- Added `POST /api/assignments` for creating manual assignments.
- Added stable assignment validation error responses with `code` and `message`.
- Added team-manager authorization for assignment creation.
- Added validation that assignments can be created only while the schedule is `DRAFT`.
- Added validation that the assigned employee is an active member of the shift's team.
- Added validation that the same employee cannot be assigned twice to the same shift.
- Added validation that a shift cannot exceed `requiredWorkers`.
- Added overlap validation across the employee's assignments in all teams.
- Added minimum-rest validation against the nearest previous and next assignments.
- Added service tests for successful assignment creation, unmanaged-team rejection, published-schedule rejection, team-membership rejection, duplicate assignment rejection, capacity rejection, overlap rejection, and minimum-rest rejection.
- Verified `mvn test` succeeds.

### 2026-07-11 - Phase 4 Assignment Workflow Completion

- Added root `README.md` for the repository.
- Added `GET /api/schedules/{scheduleId}/assignments` for listing assignments in a schedule.
- Added `DELETE /api/assignments/{assignmentId}` for deleting assignments from draft schedules.
- Added team-manager authorization for assignment listing and deletion.
- Added validation that assignments can be deleted only while the schedule is `DRAFT`.
- Added service tests for managed schedule listing, unmanaged schedule listing rejection, successful deletion, unmanaged deletion rejection, and published-schedule deletion rejection.
- Updated `README.md`, `shift-management-backend/README.md`, and the Phase 4 plan to match the current assignment workflow.
- Verified `mvn test` succeeds.

### 2026-07-12 - Phase 5 Availability Constraint Persistence

- Started Phase 5 with a small persistence-only step.
- Added `AvailabilityConstraint` entity linked to `User`.
- Added `AvailabilityConstraintRepository`.
- Added Flyway migration `V5__create_availability_constraints.sql`.
- Stored availability constraint time ranges as `Instant` values.
- Added optional `reason` and required `createdAt` fields.
- Added validation that constraint `endTime` must be after `startTime`.
- Added repository methods for personal constraint listing and future overlap checks.
- Added entity tests for valid constraint creation, invalid time range rejection, and required employee validation.
- Updated `README.md`, `shift-management-backend/README.md`, and the Phase 5 plan to show that only the persistence model has been implemented so far.
- Verified `mvn test` succeeds.

### 2026-07-12 - Phase 5 Availability Constraint API Start

- Added `CreateAvailabilityConstraintRequest`.
- Added `AvailabilityConstraintResponse`.
- Added `AvailabilityConstraintService`.
- Added `AvailabilityConstraintController`.
- Added `POST /api/availability-constraints` for creating a personal availability constraint.
- Added `GET /api/availability-constraints/me` for listing the authenticated user's constraints.
- Added service validation that constraint `endTime` must be after `startTime`.
- Added service tests for successful personal constraint creation, invalid time-range rejection, and personal constraint listing.
- Updated `README.md`, `shift-management-backend/README.md`, and the Phase 5 plan to show that create/list are implemented while delete and assignment validation are still open.
- Verified `mvn test` succeeds.

### 2026-07-14 - Phase 5 Availability Constraint Delete

- Added `DELETE /api/availability-constraints/{constraintId}` for deleting a personal availability constraint.
- Added service ownership validation so users can delete only their own constraints.
- Returned `404 Not Found` when a constraint does not exist or belongs to another user.
- Added service tests for successful deletion, missing constraint rejection, and other-user constraint rejection.
- Updated `README.md`, `shift-management-backend/README.md`, and the Phase 5 plan to show that personal create, list, and delete are implemented.
- Verified `mvn test` succeeds.

### 2026-07-15 - Phase 5 Availability Constraint Assignment Overlap

- Added validation that a user cannot create an availability constraint overlapping one of their existing assignments.
- Reused the assignment overlap query based on the half-open range rule.
- Returned `409 Conflict` when a new availability constraint conflicts with an existing assignment.
- Added a service test for rejecting availability constraints that overlap existing assignments.
- Updated `README.md`, `shift-management-backend/README.md`, and the Phase 5 plan to show that this direction of availability validation is implemented.
- Verified `mvn -Dtest=AvailabilityConstraintServiceTest test` succeeds outside the Codex sandbox.

### 2026-07-15 - Phase 5 Assignment Availability Validation

- Added validation that a manager cannot assign an employee to a shift overlapping the employee's availability constraints.
- Added stable assignment validation code `AVAILABILITY_CONFLICT`.
- Kept the validation in `AssignmentService`, before assignment overlap and minimum-rest checks.
- Added a service test for rejecting assignment creation when the employee is unavailable.
- Updated `README.md`, `shift-management-backend/README.md`, and the Phase 5 plan to show that assignment validation against availability constraints is implemented.
- Verified `mvn -Dtest=AssignmentServiceTest test` succeeds outside the Codex sandbox.

### 2026-07-16 - Phase 6 Staffing Role Persistence

- Added a permanent service test that verifies full-day availability constraints can be created.
- Started Phase 6 with a persistence-only step.
- Added `StaffingRole` entity linked to `Team`.
- Added `StaffingRoleRepository`.
- Added Flyway migration `V6__create_staffing_roles.sql`.
- Added role name trimming and blank-name validation in the domain model.
- Added a unique database constraint for role names inside the same team.
- Added entity tests for valid role creation, name trimming, blank-name rejection, and required team validation.
- Updated `README.md`, `shift-management-backend/README.md`, and the Phase 6 plan to show that only the staffing role persistence model has been implemented so far.
- Verified `mvn -Dtest=StaffingRoleTest,AvailabilityConstraintServiceTest test` succeeds outside the Codex sandbox.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL and Flyway migrates the schema to `V6`.

### 2026-07-16 - Phase 6 Team Member Staffing Role Persistence

- Added `TeamMemberStaffingRole` entity linking `TeamMember` and `StaffingRole`.
- Added `TeamMemberStaffingRoleRepository`.
- Added Flyway migration `V7__create_team_member_staffing_roles.sql`.
- Added `assignedAt` to record when a staffing role was assigned to a team member.
- Added domain validation that a team member can receive only staffing roles from the same team.
- Added a unique database constraint so the same staffing role cannot be assigned twice to the same team member.
- Added entity tests for valid role assignment, cross-team rejection, and required fields.
- Updated `README.md`, `shift-management-backend/README.md`, and the Phase 6 plan to show that role assignment persistence is implemented but API endpoints are still deferred.
- Verified `mvn -Dtest=StaffingRoleTest,TeamMemberStaffingRoleTest test` succeeds.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL and Flyway migrates the schema to `V7`.

### 2026-07-17 - Phase 6 Staffing Role API Start

- Added `CreateStaffingRoleRequest`.
- Added `StaffingRoleResponse`.
- Added `StaffingRoleService`.
- Added `StaffingRoleController`.
- Added `POST /api/teams/{teamId}/staffing-roles` for creating team staffing roles.
- Added `GET /api/teams/{teamId}/staffing-roles` for listing team staffing roles.
- Added manager authorization so only managers of the requested team can create or list staffing roles.
- Added duplicate-name validation for staffing roles in the same team.
- Trimmed role names before duplicate checking and saving.
- Added service tests for successful creation, unmanaged-team rejection, missing-team rejection, duplicate-name rejection, successful list, and unmanaged-list rejection.
- Updated `README.md`, `shift-management-backend/README.md`, `docs/current-backend-architecture.md`, and the Phase 6 plan to show that create/list API endpoints are implemented.
- Verified `mvn -Dtest=StaffingRoleTest,TeamMemberStaffingRoleTest,StaffingRoleServiceTest test` succeeds outside the Codex sandbox.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL and `GET /api/teams/1/staffing-roles` returns `200 OK` for `manager1`.

### 2026-07-18 - Phase 6 Optional Required Staffing Role On Shifts

- Added nullable `requiredStaffingRole` relation to `Shift`.
- Added `requiredStaffingRoleId` to `CreateShiftRequest` and `UpdateShiftRequest`.
- Kept shift create/update requests backward compatible by allowing missing or `null` required staffing role values.
- Added `requiredStaffingRoleId` and `requiredStaffingRoleName` to `ShiftResponse`.
- Added Flyway migration `V8__add_required_staffing_role_to_shifts.sql`.
- Added service validation that a required staffing role must exist and belong to the schedule's team.
- Added tests for shifts without role requirements, shifts with role requirements, missing role rejection, cross-team role rejection, and updating a shift role requirement.
- Updated `README.md`, `shift-management-backend/README.md`, `docs/current-backend-architecture.md`, and the Phase 6 plan.
- Verified `mvn -Dtest=ShiftTest,ShiftServiceTest test` succeeds outside the Codex sandbox.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL and Flyway migrates the schema to `V8`.

### 2026-07-18 - Phase 6 Employee Staffing Role API

- Added `AssignStaffingRoleRequest`.
- Added `TeamMemberStaffingRoleResponse`.
- Added `TeamMemberStaffingRoleService`.
- Added `TeamMemberStaffingRoleController`.
- Added `POST /api/teams/{teamId}/employees/{employeeId}/staffing-roles` for assigning a team staffing role to an active team member.
- Added `GET /api/teams/{teamId}/employees/{employeeId}/staffing-roles` for listing an employee's staffing roles in a team.
- Added manager authorization so only managers of the requested team can assign or list employee staffing roles.
- Added validation that the employee must be an active member of the requested team.
- Added validation that the assigned staffing role must belong to the requested team.
- Added duplicate-assignment validation for employee staffing roles.
- Added service tests for successful assignment, unmanaged-team rejection, missing active team member rejection, cross-team role rejection, duplicate role rejection, successful listing, and unmanaged-list rejection.
- Updated `README.md`, `shift-management-backend/README.md`, `docs/current-backend-architecture.md`, and the Phase 6 plan.
- Verified `mvn -Dtest=StaffingRoleTest,StaffingRoleServiceTest,TeamMemberStaffingRoleTest,TeamMemberStaffingRoleServiceTest test` succeeds outside the Codex sandbox.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL with Flyway schema version `V8`.
- Verified `GET /api/teams/1/employees/2/staffing-roles` returns `200 OK` for `manager1`.

### 2026-07-19 - Phase 6 Assignment Validation Against Required Staffing Roles

- Added assignment validation that checks a shift's optional required staffing role.
- Kept shifts without a required staffing role backward compatible with the existing assignment behavior.
- Added `STAFFING_ROLE_REQUIRED` as a stable assignment validation code.
- Added a Spring Data repository query that checks whether an employee has a required staffing role inside the shift's team.
- Added service tests for assigning an employee who has the required role and rejecting an employee who does not have the required role in the shift's team.
- Updated `README.md`, `shift-management-backend/README.md`, `docs/current-backend-architecture.md`, and the Phase 6 plan.
- Verified `mvn -Dtest=AssignmentServiceTest,TeamMemberStaffingRoleServiceTest test` succeeds outside the Codex sandbox.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL with Flyway schema version `V8`.
- Verified `GET /api/health` returns `UP`.

### 2026-07-20 - Phase 7 Schedule Publish Endpoint

- Started Phase 7 with a focused publish-only step.
- Added `Schedule.publish(...)` as the domain operation that changes a schedule from `DRAFT` to `PUBLISHED`.
- Publishing records `publishedAt` and increments `publicationNumber`.
- Added `ScheduleService.publishSchedule(...)` with manager authorization and draft-status validation.
- Added `POST /api/schedules/{scheduleId}/publish`.
- Added entity tests for publishing a draft schedule and rejecting repeated publication.
- Added service tests for successful publication, missing schedule rejection, unmanaged schedule rejection, and already-published schedule rejection.
- Updated `README.md`, `shift-management-backend/README.md`, `docs/current-backend-architecture.md`, and the Phase 7 plan.
- Verified `mvn -Dtest=ScheduleTest,ScheduleServiceTest test` succeeds outside the Codex sandbox.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL with Flyway schema version `V8`.
- Verified `GET /api/health` returns `UP`.

### 2026-07-21 - Phase 7 Schedule Reopen Endpoint

- Added `Schedule.reopen(...)` as the domain operation that changes a schedule from `PUBLISHED` back to `DRAFT`.
- Added `ScheduleService.reopenSchedule(...)` with manager authorization and published-status validation.
- Added `POST /api/schedules/{scheduleId}/reopen`.
- Kept `publishedAt` and `publicationNumber` unchanged when reopening, so the latest publication remains visible.
- Verified that publishing a reopened schedule again increments `publicationNumber`.
- Added entity tests for reopening, rejecting reopen on draft schedules, and publishing a reopened schedule again.
- Added service tests for successful reopening, missing schedule rejection, unmanaged schedule rejection, and draft-schedule reopen rejection.
- Updated `README.md`, `shift-management-backend/README.md`, `docs/current-backend-architecture.md`, and the Phase 7 plan.
- Verified `mvn -Dtest=ScheduleTest,ScheduleServiceTest test` succeeds outside the Codex sandbox.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL with Flyway schema version `V8`.
- Verified `GET /api/health` returns `UP`.

### 2026-07-22 - Phase 7 Employee Published Schedule List

- Added `GET /api/schedules/me/published`.
- Added a schedule repository query for published schedules across the authenticated user's active team memberships.
- Added `ScheduleService.listPublishedSchedulesForUser(...)`.
- The endpoint returns only `PUBLISHED` schedules.
- Draft schedules are not returned.
- Users with no active team memberships receive an empty list.
- Kept this as a read-only API step with no database migration.
- Added service tests for successful employee listing, empty active-team membership, and missing user rejection.
- Updated `README.md`, `shift-management-backend/README.md`, `docs/current-backend-architecture.md`, and the Phase 7 plan.
- Verified `mvn -Dtest=ScheduleTest,ScheduleServiceTest test` succeeds outside the Codex sandbox.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL with Flyway schema version `V8`.
- Verified `GET /api/health` returns `UP`.

### 2026-07-22 - Phase 7 Employee Published Schedule Details

- Added `GET /api/schedules/me/published/{scheduleId}`.
- Added `PublishedScheduleDetailsResponse`.
- Added `PublishedShiftResponse`.
- Added `PublishedAssignmentResponse`.
- Added `ScheduleService.getPublishedScheduleDetailsForUser(...)`.
- The endpoint returns one published schedule with its shifts and shift assignments.
- The endpoint returns `404 Not Found` for draft schedules.
- The endpoint returns `404 Not Found` for schedules outside the authenticated user's active team memberships.
- Kept this as a read-only API step with no database migration.
- Added service tests for successful details viewing, draft-schedule rejection, and unrelated-team rejection.
- Updated `README.md`, `shift-management-backend/README.md`, `docs/current-backend-architecture.md`, and the Phase 7 plan.
- Verified `mvn -Dtest=ScheduleTest,ScheduleServiceTest test` succeeds outside the Codex sandbox.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL with Flyway schema version `V8`.
- Verified `GET /api/health` returns `UP`.
