# Implementation Plan - Shift Management System

Last updated: 2026-08-26

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

Source code and technical project material should be written in English:

- File and directory names.
- Code identifiers.
- Code comments.
- README files and developer documentation.
- Commit messages.

Submission documents for the course instructor may be written in Hebrew because
they are part of the academic delivery rather than the source-code convention.
The user guide should be bilingual in Hebrew and English.

Local Hebrew reference materials can stay on the computer. They should be
committed only when they are intentionally turned into official submission
documents.

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
- [x] Create an empty React frontend project.
- [x] Configure local PostgreSQL.
- [x] Configure `application.yml`.
- [x] Verify that the backend starts.
- [x] Verify that the frontend starts.

### Verify

- [ ] Backend runs without errors.
- [x] Frontend runs without errors.
- [x] A simple health endpoint exists: `GET /api/health`.
- [x] The health endpoint can be opened in the browser or tested with Postman.

### Document

- [x] How to run the backend.
- [x] How to run the frontend.
- [x] How to connect to the database.

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
- [x] Return a report before publication.
- [x] Allow publication with unfilled shifts only after explicit confirmation.

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

Phase 7 currently supports the basic schedule lifecycle, publication readiness, explicit unfilled-publication confirmation, and employee-facing published schedule viewing.

Current endpoints:

- `POST /api/schedules/{scheduleId}/publish`
- `POST /api/schedules/{scheduleId}/reopen`
- `GET /api/schedules/me/published`
- `GET /api/schedules/me/published/{scheduleId}`
- `GET /api/schedules/{scheduleId}/publication-readiness`

Rules for publication:

- Only a manager of the schedule's team can publish the schedule.
- Only `DRAFT` schedules can be published.
- Publishing changes the schedule status to `PUBLISHED`.
- Publishing records `publishedAt`.
- Publishing increments `publicationNumber`.
- Publishing without explicit confirmation is allowed only when the publication readiness report has `readyToPublish: true`.
- Publishing with unfilled shifts requires request body `{"confirmUnfilled":true}`.
- Publishing with unfilled shifts without explicit confirmation returns `409 Conflict`.
- Direct shift changes and assignment changes are already blocked once the schedule is `PUBLISHED`.

Rules for publication readiness:

- Only a manager of the schedule's team can view publication readiness.
- The readiness report is read-only.
- The report summarizes total shifts, required workers, assigned workers, and open slots.
- `readyToPublish` is `true` only when the schedule has at least one shift and all shift slots are filled.
- Unfilled shifts are returned with their required worker count, assigned worker count, and open slot count.
- The publish endpoint uses this readiness logic when `confirmUnfilled` is not provided.

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

No remaining items are deferred from the first Phase 7 scope.

## Phase 8 - Basic Frontend

Goal: build a minimal React interface connected to the backend.

### Understand

- How React calls a REST API.
- How login state is stored.
- How employee screens and manager screens are separated.

### Implement

- [x] Login screen.
- [x] Role-based navigation.
- [x] Initial published schedule view.
- [x] Employee published schedule details view.
- [x] Employee availability constraint screen.
- [x] Manager schedule creation screen.
- [x] Manager shift creation screen.
- [x] Simple manual assignment screen.
- [x] Manager publication readiness and publish screen.
- [x] Manager published schedule reopen screen.
- [x] Transfer request list screen.
- [x] Transfer request employee and manager action buttons.
- [x] Display backend errors clearly for login and published schedule loading.
- [x] Display backend errors clearly for published schedule details loading.
- [x] Return expired JWT sessions to the login screen.

### Verify

- [x] Successful login navigates to the correct area.
- [x] Frontend can call the published schedules backend endpoint.
- [x] Employees can view published schedule details in the frontend.
- [x] Employees can create, view, and delete availability constraints in the frontend.
- [x] Managers can create draft schedules from the frontend.
- [x] Managers can create shifts.
- [x] Assignment errors are displayed clearly.
- [x] Managers can check publication readiness before publishing.
- [x] Managers can publish draft schedules from the frontend.
- [x] Managers can reopen published schedules from the frontend.
- [x] Transfer request lists load for employees and managers.
- [x] Transfer request actions refresh the relevant list.
- [x] Expired sessions are cleared and show a clear login message.

### Document

- [x] Existing screens.
- [x] For each screen: who can use it and what it does.

## Phase 9 - Transfer And Swap Requests

Goal: allow employees to transfer a shift or request a shift swap.

### Understand

- The difference between transfer and swap.
- What a state machine is.
- Why all assignment rules must be checked again at final approval time.

### Implement

- [x] Create `SwapRequest`.
- [x] Start with transfer only.
- [x] Add statuses: `PENDING_EMPLOYEE`, `PENDING_MANAGER`, `APPROVED`, `REJECTED`, `CANCELLED`, `INVALIDATED`.
- [x] Allow the target employee to approve.
- [x] Allow the target employee to reject.
- [x] Allow the requester to cancel an active transfer request.
- [x] Allow employees and managers to list relevant transfer requests.
- [x] Run assignment validations before employee-policy final approval.
- [x] Run assignment validations before manager final approval.
- [x] Move the assignment to the target employee after employee-policy transfer approval.
- [x] Move the assignment to the target employee after manager approval.
- [ ] Add full swap.
- [x] Add manager approval according to team policy.

### Verify

- [x] An employee can create a transfer request.
- [x] The target employee can approve it.
- [x] Another employee cannot approve a request not addressed to them.
- [x] An employee-policy transfer request becomes invalidated if assignment is no longer possible.
- [x] A manager-policy transfer request can be approved by a manager of the source team.
- [x] A manager-policy transfer request becomes invalidated if assignment is no longer possible.
- [x] The target employee can reject a pending transfer request.
- [x] The requester can cancel an active transfer request.
- [x] Employees and managers can list only transfer requests relevant to them.
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
- [x] Implement basic automatic assignment.
- [x] Rank employees by fewer assigned hours.
- [x] Return a report of unassigned shifts.

### Verify

- [ ] A template creates shifts on the expected dates.
- [x] Automatic assignment assigns only eligible employees.
- [x] A shift remains unfilled if no employee is eligible.
- [x] The report explains what was not assigned.

### Document

- [x] The automatic assignment algorithm.
- [x] The algorithm limitations.

## Phase 11 - Notifications And JMS

Goal: add internal notifications and satisfy the JMS requirement.

### Understand

- What asynchronous messaging means.
- The difference between saving a notification and sending an event.
- What a transactional outbox is.
- Why JMS does not communicate directly with the browser.

### Implement

- [x] Create `Notification`.
- [x] Create `EventOutbox`.
- [x] Store a pending `schedule.published` outbox event when a schedule is published.
- [x] Add personal notification list, unread count, and mark-as-read backend endpoints.
- [x] Create a basic notification screen.
- [x] Create notifications when a schedule is published.
- [x] Add a dispatcher that sends outbox events to JMS.
- [x] Add a consumer that creates notifications.
- [x] Ensure idempotency with `eventId`.

### Verify

- [x] Publishing a schedule stores a pending `schedule.published` outbox event.
- [x] Publishing a schedule creates a notification.
- [x] A user sees only personal notifications.
- [x] A notification can be marked as read.
- [x] Reprocessing the same event does not create duplicates.

### Document

- [x] Event flow foundation: publish schedule -> event outbox.
- [x] Full event flow: publish schedule -> outbox -> JMS -> notification.
- [x] Existing notification types.

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
- [x] Unified error handling with `ControllerAdvice`.
- [x] Basic logging.
- [x] Seed data for the demo.

### Verify

- [ ] All tests pass.
- [ ] The project can be run on a clean computer using the installation guide.
- [ ] A full demo script exists.
- [ ] All central screens work.

### Document

- [ ] Updated design document.
- [ ] User guide.
- [ ] Installation guide.
- [ ] Explanation of central classes and functions.

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

### 2026-07-22 - Phase 7 Publication Readiness Report

- Added `GET /api/schedules/{scheduleId}/publication-readiness`.
- Added `SchedulePublicationReadinessResponse`.
- Added `SchedulePublicationReadinessShiftResponse`.
- Added `ScheduleService.getPublicationReadiness(...)`.
- The report summarizes total shifts, required workers, assigned workers, and open slots.
- The report lists unfilled shifts with their assigned worker count and remaining open slots.
- `readyToPublish` is `true` only when the schedule has at least one shift and all shift slots are filled.
- The report is manager-only and read-only.
- Kept explicit confirmation for publishing with unfilled shifts as a separate follow-up step.
- Kept this as a read-only API step with no database migration.
- Added service tests for unfilled readiness, ready schedule readiness, and unmanaged-schedule rejection.
- Updated `README.md`, `shift-management-backend/README.md`, `docs/current-backend-architecture.md`, and the Phase 7 plan.
- Verified `mvn -Dtest=ScheduleTest,ScheduleServiceTest test` succeeds outside the Codex sandbox.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL with Flyway schema version `V8`.
- Verified `GET /api/health` returns `UP`.

### 2026-07-23 - Phase 7 Explicit Unfilled Publication Confirmation

- Added `PublishScheduleRequest` with `confirmUnfilled`.
- Updated `POST /api/schedules/{scheduleId}/publish` to accept an optional request body.
- Publishing without a request body still works when the schedule is fully assigned.
- Publishing an unfilled schedule without `confirmUnfilled: true` returns `409 Conflict`.
- Publishing an unfilled schedule with `confirmUnfilled: true` is allowed.
- Reused the publication readiness logic to decide whether a schedule is fully assigned.
- Added service tests for full schedule publication, unconfirmed unfilled publication rejection, and confirmed unfilled publication.
- Updated `README.md`, `shift-management-backend/README.md`, and the Phase 7 plan.
- Verified `mvn -Dtest=ScheduleTest,ScheduleServiceTest test` succeeds outside the Codex sandbox.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL with Flyway schema version `V8`.
- Verified `GET /api/health` returns `UP`.

### 2026-07-24 - Phase 8 Initial React Frontend

- Added the first React frontend under `shift-management-frontend`.
- Added a Vite build setup with `package.json`, `vite.config.js`, and `pnpm-lock.yaml`.
- Added a login screen connected to `POST /api/auth/login`.
- Stored the JWT session in browser local storage.
- Added role-based workspace display for manager and employee users.
- Added an initial published schedule list loaded from `GET /api/schedules/me/published`.
- Added CORS support in the backend for the local React development server.
- Updated project documentation to include frontend status and local frontend access.
- Verified `pnpm build` succeeds.
- Verified `mvn test` succeeds outside the Codex sandbox.
- Verified browser login works for `manager1/password` and `employee1/password`.

### 2026-07-24 - Local Run Documentation

- Added `docs/RUN_LOCALLY.md` as the main concise run guide for presentation and local development.
- Documented required tools, database startup, backend startup, frontend startup, demo users, URLs, and common local issues.
- Updated the architecture document so the system context shows the implemented React frontend instead of describing React as future work.

### 2026-07-27 - Phase 8 Employee Published Schedule Details View

- Added a frontend API call for `GET /api/schedules/me/published/{scheduleId}`.
- Added schedule selection from the published schedules list.
- Added a schedule details panel that shows schedule metadata, shifts, required workers, required staffing role when present, and assigned employees.
- Added loading, empty, and error states for schedule details.
- Kept this step frontend-only because the matching backend endpoint was already implemented in Phase 7.

### 2026-07-27 - Phase 8 Manager Schedule Creation Screen

- Added `GET /api/teams/me/managed` so the frontend can show managed teams instead of requiring manual team IDs.
- Added `TeamResponse`, `TeamService`, and `TeamController`.
- Added focused service tests for managed team listing.
- Added frontend API calls for managed teams and draft schedule creation.
- Replaced the manager placeholder actions with a manager-only create schedule form.
- Added success and error states for schedule creation.
- Verified `pnpm build` succeeds.
- Verified `mvn -Dtest=TeamServiceTest test` succeeds outside the Codex sandbox.

### 2026-07-28 - Phase 8 Manager Shift Creation Screen

- Added `GET /api/schedules/me/managed/drafts` so the frontend can show draft schedules managed by the signed-in manager.
- Added service tests for managed draft schedule listing.
- Added frontend API calls for managed draft schedules, team staffing roles, and shift creation.
- Added a manager-only shift creation form connected to `POST /api/schedules/{scheduleId}/shifts`.
- The shift form supports draft schedule selection, start/end time, description, required workers, minimum rest hours, and optional required staffing role.
- Added success and error states for shift creation.
- Verified `pnpm build` succeeds.
- Verified `mvn -Dtest=ScheduleServiceTest,ShiftServiceTest test` succeeds outside the Codex sandbox.

### 2026-07-29 - Phase 8 Manual Assignment Screen

- Added `GET /api/teams/{teamId}/employees` so the frontend can show active employees for a managed team.
- Added `TeamEmployeeResponse` and service tests for managed team employee listing.
- Added frontend API calls for draft schedule shifts, team employees, schedule assignments, and manual assignment creation.
- Added a manager-only manual assignment form connected to `POST /api/assignments`.
- The assignment form supports draft schedule selection, shift selection, employee selection, current assignment display, success states, and backend validation errors.
- Verified `pnpm build` succeeds.
- Verified `mvn -Dtest=TeamServiceTest,AssignmentServiceTest test` succeeds outside the Codex sandbox.

### 2026-07-30 - Phase 8 Expired Session Handling

- Added structured frontend API errors that include the HTTP status code.
- Added shared frontend handling for `401 Unauthorized` responses.
- Expired JWT sessions now clear local storage, reset authenticated screen state, and return to the login screen.
- The login screen shows `Session expired. Please sign in again.` instead of leaving stale manager or employee data on screen.

### 2026-07-31 - Phase 11 Notification And Event Outbox Foundation

- Added `Notification` and `NotificationType`.
- Added `EventOutbox` for pending asynchronous system events.
- Added Flyway migration `V9__create_notifications_and_event_outbox.sql`.
- Added personal notification APIs for listing notifications, counting unread notifications, and marking a notification as read.
- Added an idempotent notification creation helper based on `eventId` and recipient.
- Added `EventOutboxService` to store structured event payloads as JSON.
- Updated schedule publication so publishing a schedule stores a pending `schedule.published` outbox event.
- Kept JMS dispatcher and consumer for the next Phase 11 step.
- Added unit tests for notifications, event outbox behavior, event creation, and schedule-publication event creation.
- Verified `mvn -Dtest=EventOutboxTest,EventOutboxServiceTest,NotificationTest,NotificationServiceTest,ScheduleServiceTest test` succeeds outside the Codex sandbox.

### 2026-08-02 - Phase 11 JMS Notification Delivery

- Added Spring JMS support with ActiveMQ Artemis.
- Added ActiveMQ Artemis to `compose.yml`.
- Added JMS connection settings and notification queue configuration in `application.yml`.
- Enabled JMS listeners and scheduling in the Spring Boot application.
- Added `OutboxEventDispatcher` to poll unsent `event_outbox` rows and send them to JMS queue `notification.events`.
- Added `OutboxEventMessage` as the message shape sent through JMS.
- Added `NotificationEventConsumer` to consume notification events from JMS.
- Added `SchedulePublishedNotificationService` to create schedule-published notifications for active team members.
- Kept notification creation idempotent by `eventId` and recipient.
- Added tests for dispatcher success/failure behavior, JMS consumer routing, and schedule-published notification creation.
- Verified focused JMS/notification tests succeed outside the Codex sandbox.
- Verified ActiveMQ Artemis starts from Docker Compose.
- Verified the backend starts with PostgreSQL and ActiveMQ Artemis.
- Verified an end-to-end smoke test: publishing a schedule creates an employee notification through outbox -> JMS -> consumer.
- Verified the created notification can be marked as read and the unread count updates.

### 2026-08-03 - Frontend Notification Center

- Added frontend API calls for personal notification list, unread count, and mark-as-read.
- Added an authenticated notification center to the React workspace.
- Added unread count display in the sidebar and notification section.
- Added a manual refresh action so users can reload notifications after asynchronous JMS delivery.
- Added mark-as-read behavior that updates the notification list and unread count in the UI.
- Updated project documentation to reflect that the notification center is implemented.

### 2026-08-04 - Transfer Request MVP

- Added `SwapRequest`, `SwapRequestType`, and `SwapRequestStatus`.
- Added Flyway migration `V10__create_swap_requests.sql`.
- Added `POST /api/requests/transfers` for creating transfer requests.
- Transfer request creation currently supports only the first workflow step: requester creates a `TRANSFER` request in `PENDING_EMPLOYEE`.
- Validation ensures the requester is an employee, owns the source assignment, the source schedule is published, the target is a different employee, the target is an active member of the shift team, and there is no active request for the same source assignment.
- Added focused unit tests for the transfer request model and service.
- Verified focused request tests succeed outside the Codex sandbox.
- Verified the full backend test suite succeeds outside the Codex sandbox.
- Verified Spring Boot starts against PostgreSQL and ActiveMQ Artemis, Flyway migrates the schema to `V10`, and Hibernate validates the mappings.

### 2026-08-05 - Transfer Request Employee Approval

- Added `POST /api/requests/{requestId}/employee-approve`.
- Added domain behavior for target employee approval on `SwapRequest`.
- Target approval sets `employeeApprovedAt` and `updatedAt`.
- If the source team's approval policy is `EMPLOYEE`, the request moves from `PENDING_EMPLOYEE` to `APPROVED`.
- If the source team's approval policy is `MANAGER`, the request moves from `PENDING_EMPLOYEE` to `PENDING_MANAGER`.
- Validation ensures only the target employee can approve the request.
- Re-approval of a request that is no longer `PENDING_EMPLOYEE` returns a conflict.
- Added focused tests for domain status transitions and approval authorization.
- Verified the full backend test suite succeeds outside the Codex sandbox.

### 2026-08-07 - Employee-Policy Transfer Execution

- Added `Assignment.transferTo(...)` so an existing assignment can move to a different employee.
- Added transfer-specific assignment validation that reuses assignment business rules but does not require open shift capacity, because transfer replaces an employee inside an existing assignment slot.
- Updated employee approval so `EMPLOYEE` policy transfers execute immediately after target approval.
- If transfer validation fails during execution, the request becomes `INVALIDATED` and the assignment remains unchanged.
- Added focused tests for successful transfer execution, invalidation on validation failure, and capacity-independent transfer validation.

### 2026-08-07 - Manager-Policy Transfer Execution

- Added `POST /api/requests/{requestId}/manager-approve`.
- Added domain behavior for manager approval on `SwapRequest`.
- Manager approval is allowed only when a request is `PENDING_MANAGER`.
- Validation ensures the authenticated user is a manager of the source shift's team.
- Manager approval re-runs transfer eligibility checks before moving the assignment.
- If transfer validation fails during manager approval, the request becomes `INVALIDATED` and the assignment remains unchanged.
- Added focused tests for manager approval, team-scoped authorization, invalidation on validation failure, and invalid status conflicts.

### 2026-08-07 - Postman API Collection

- Added a Postman collection for the implemented backend API.
- Added a local Postman environment with development variables and empty token storage.
- Login requests save the returned JWT into `accessToken` automatically.
- Added a suggested manual demo flow for health, auth, teams, schedules, shifts, assignments, publication, transfer requests, and manager approval.

### 2026-08-09 - Transfer Request Reject And Cancel

- Added `POST /api/requests/{requestId}/employee-reject`.
- Added `POST /api/requests/{requestId}/cancel`.
- Added domain behavior for target employee rejection and requester cancellation on `SwapRequest`.
- Target rejection is allowed only while a request is `PENDING_EMPLOYEE`.
- Requester cancellation is allowed only while a request is `PENDING_EMPLOYEE` or `PENDING_MANAGER`.
- Rejection and cancellation update request status only; assignments remain unchanged.
- Added focused tests for valid transitions, authorization checks, and invalid status conflicts.

### 2026-08-09 - Basic Business Logging

- Added focused SLF4J logging for schedule creation, publication, and reopening.
- Added focused logging for assignment creation and deletion.
- Added focused logging for transfer request creation, approval, rejection, cancellation, invalidation, and assignment transfer execution.
- Added logging for successful outbox dispatch and schedule-published notification creation.
- Avoided logging passwords, JWT tokens, and full request payloads.

### 2026-08-10 - Unified API Error Responses

- Added `ApiErrorResponse` as the common JSON shape for API errors.
- Added `GlobalExceptionHandler` with `@RestControllerAdvice`.
- Unified handling for `ResponseStatusException`, request body validation errors, malformed JSON, assignment business validation, and unexpected server errors.
- Updated Spring Security authentication and authorization failures to return the same JSON error shape.
- Removed the assignment-specific error handler and response type.
- Added focused tests for the global exception handler.

### 2026-08-10 - Transfer Request List Endpoints

- Added employee outgoing transfer request listing: `GET /api/requests/me/outgoing`.
- Added employee incoming transfer request listing: `GET /api/requests/me/incoming`.
- Added manager pending approval listing: `GET /api/requests/manager/pending`.
- Kept request visibility scoped to the authenticated employee or to teams managed by the authenticated manager.
- Added focused service tests for the new request list workflows.

### 2026-08-11 - Frontend Transfer Request Lists

- Added frontend API calls for outgoing, incoming, and pending-manager transfer request lists.
- Added frontend API calls for target employee approval, target employee rejection, requester cancellation, and manager approval.
- Added an authenticated transfer request screen section.
- Employees can view incoming and outgoing transfer requests.
- Managers can view transfer requests waiting for manager approval.
- Transfer request actions refresh the relevant list after completion.
- Verified the frontend production build succeeds.

### 2026-08-13 - Phase 12 Demo Seed Scenario

- Expanded the development data seeder into an idempotent presentation scenario.
- The seeder now reuses existing demo users and creates missing demo data instead of stopping when users already exist.
- Added demo staffing roles, team-member staffing role assignments, a published schedule, a draft schedule, shifts, assignments, schedule-published notifications, and an active transfer request.
- Added small repository lookup methods needed for idempotent seed creation.
- Updated documentation to describe the new demo data.
- Verified the frontend production build succeeds.
- Verified the full backend test suite succeeds outside the Codex sandbox.

### 2026-08-26 - Reduced-Scope Compliance: Availability UI

- Added an employee-facing availability constraint screen to the React frontend.
- Connected the screen to the existing availability backend APIs for create, personal list, and delete.
- Added a dedicated `useAvailabilityConstraints` hook to keep availability state and API actions outside `App.jsx`.
- Added frontend API helpers for availability constraints.
- Updated README and the implementation plan to include the implemented availability UI.

### 2026-08-26 - Reduced-Scope Compliance: Manager Publication UI

- Added `GET /api/schedules/me/managed/published` so managers can list schedules they already published.
- Added frontend API helpers for publication readiness, publish, reopen, and managed published schedule listing.
- Added a dedicated `useSchedulePublication` hook for publication state and API actions.
- Added a manager publication panel for readiness checks, explicit unfilled-shift confirmation, draft publication, and published schedule reopening.
- Updated README and backend documentation to include the manager publication workflow.

### 2026-08-26 - Reduced-Scope Compliance: Basic Automatic Assignment

- Added `POST /api/schedules/{scheduleId}/auto-assign` for manager-triggered automatic assignment on draft schedules.
- Reused the existing assignment validation rules so automatic assignment respects active team membership, required staffing role, duplicate assignment prevention, availability constraints, shift overlap, and minimum rest.
- Ranked eligible employees by fewer assigned minutes in the selected schedule, then by employee name for deterministic tie-breaking.
- Added a structured report with per-shift created assignments and remaining open slots.
- Added focused service tests for successful least-loaded assignment, unfilled-shift reporting, and published-schedule rejection.
- Updated README and backend documentation to describe the automatic assignment workflow and limitations.

### 2026-08-26 - Refactor: Assignment Validation

- Extracted assignment eligibility checks from `AssignmentService` into `AssignmentValidator`.
- Kept manual assignment, automatic assignment, and transfer-request validation using the same validation rules.
- Reduced `AssignmentService` so it focuses on workflow orchestration, persistence calls, and report creation.
- Updated the architecture document and backend README to reflect the validator responsibility.
