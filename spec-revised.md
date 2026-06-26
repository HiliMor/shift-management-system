# Shift Management System - Revised Specification

## 1. Overview

A full-stack web application for managing employee shifts in organizations
containing multiple teams.

The system has two application roles:

- `EMPLOYEE`
- `MANAGER`

An employee may belong to multiple teams, and a manager may manage multiple
teams. A manager may edit data only for teams assigned to that manager.

The system supports:

- Shift and schedule management
- Manual, automatic, and recurring assignments
- Availability constraints
- Staffing-role requirements
- Shift swaps and shift transfers
- Schedule publication and reopening
- Asynchronous in-application notifications
- Concurrency protection for scheduling and swap operations

---

## 2. Terminology

### Application role

The authorization role of a user: `EMPLOYEE` or `MANAGER`.

### Staffing role

A team-specific professional qualification used for scheduling, for example:

- Shift supervisor
- Entrance guard
- On-call developer

Staffing roles are separate from application authorization roles.

### Employee note

Free text written by a manager about an employee for manual scheduling
considerations. Employee notes are never exposed to employees.

### Shift note

Free text written by a manager about a shift. Shift notes are visible only to
managers who manage the shift's team and are never exposed to employees.

### Schedule

A team's shift plan for a defined date range. A schedule can be a draft or
published.

### Swap

Two employees exchange two existing assignments.

### Transfer

One employee gives an assignment to another employee who is not required to
offer another assignment in return.

---

## 3. User Capabilities

### 3.1 Employee

An employee can:

1. View a published weekly or monthly schedule for any team to which the
   employee belongs.
2. View published schedules of other teams in read-only mode for cross-team
   coordination.
3. Filter the schedule to show only personal assignments.
4. Submit an unavailability constraint for a full day or a specific time
   range, with an optional reason.
5. View and delete personal availability constraints.
6. Request a full shift swap with another employee.
7. Request a transfer of a shift to another employee.
8. Approve or reject an incoming swap or transfer request.
9. View the status and history of personal requests.
10. View in-application notifications and mark them as read.

Employees cannot view:

- Employee notes
- Shift notes
- Any draft schedule
- Manager-only assignment information

### 3.2 Manager

A manager can perform the following operations for managed teams only:

1. Create, edit, and delete schedules and shifts.
2. Split a shift into sub-shifts.
3. Assign employees manually.
4. Assign employees automatically according to availability, staffing roles,
   workload, overlap, rest, and capacity rules.
5. Define a recurring assignment of an employee to a template slot for a
   specified date range.
6. Create free-text private notes about employees.
7. Create free-text manager-only notes about shifts.
8. Create and manage shift templates.
9. Create and assign team-specific staffing roles.
10. Add and remove employees from a team.
11. Configure the team's swap approval policy.
12. Approve or reject requests that require manager approval.
13. Publish, reopen, edit, and republish a schedule.
14. View assignment validation and auto-assignment reports.

---

## 4. Team Membership and Authorization

### 4.1 Membership rules

- An employee may be a member of multiple teams.
- A manager may manage multiple teams.
- Availability constraints apply to the employee across all teams.
- Overlap and rest-period checks consider assignments from all teams.

### 4.2 Authorization rules

- Every API request except login must be authenticated.
- Employees have read-only access to published schedules.
- Employees can modify only their own constraints, requests, and notification
  read status.
- Managers can modify only teams listed in `team_managers`.
- Employee and shift notes are never returned from employee-facing endpoints.
- Employee notes are visible only to the manager who authored them.
- Shift notes are visible to all managers assigned to the shift's team.

---

## 5. Schedule Lifecycle

Each schedule belongs to one team and covers one date range.

### 5.1 States

- `DRAFT`: managers may edit shifts and assignments.
- `PUBLISHED`: employees can treat the schedule as final. Direct manager edits
  are blocked until the schedule is reopened.

### 5.2 Transitions

1. A newly created schedule starts as `DRAFT`.
2. A manager publishes the schedule.
3. Publishing changes its state to `PUBLISHED`, increments
   `publication_number`, and creates a notification event.
4. A manager may reopen a published schedule.
5. Reopening changes the state to `DRAFT`.
6. Republishing increments `publication_number` again and sends a new
   notification to all current team members.

Approved swaps and transfers may update assignments in a published schedule
without reopening it. The affected employees receive separate notifications.

Before publication, the system reports:

- Unfilled shifts
- Assignments with unresolved validation errors

A manager may publish a schedule with unfilled positions only after explicit
confirmation. A schedule containing an invalid assignment cannot be published.

---

## 6. Shift Templates

A shift template belongs to one team and defines a reusable repeating pattern.

A template contains:

- Name
- Description
- Pattern length in days
- Default minimum rest hours
- One or more template slots

Each template slot defines:

- Day offset within the pattern
- Start time
- Duration
- Number of required employees
- Optional required staffing role

Examples:

- Routine on-call template: one employee for 24 hours
- Emergency template: three eight-hour shifts per day
- Facility-specific templates with different staffing requirements

Managers can generate shifts from a selected template for a given schedule and
date range. Generated shifts retain a reference to their source template and
slot.

---

## 7. Manager Notes

### 7.1 Employee notes

- A manager may attach any number of free-text notes to an employee who is a
  member of a managed team.
- The note records its author and creation time.
- Only the authoring manager can view, update, or delete the note.
- Notes are advisory and are not interpreted by the automatic assignment
  algorithm.
- Notes are never exposed to employees.

Examples include experience, communication style, or other manual scheduling
considerations.

### 7.2 Shift notes

- A manager may attach any number of free-text notes to a shift.
- Shift notes are visible to managers of that team only.
- Shift notes are advisory and are not interpreted by the automatic assignment
  algorithm.
- Shift notes are never exposed to employees.

Staffing roles, rather than free text, must be used for requirements that the
automatic assignment algorithm is expected to enforce.

---

## 8. Assignment Rules

Every manual, automatic, recurring, swap, and transfer operation must enforce
the same assignment validations.

### 8.1 Validation order

1. **Team membership**
   - The employee must be a current member of the shift's team.
2. **Shift capacity**
   - The number of active assignments must remain less than or equal to
     `required_workers`.
3. **Staffing-role match**
   - If the shift requires a staffing role, the employee must hold it for that
     team.
4. **Availability**
   - No unavailable time range may overlap the shift.
5. **Overlap**
   - The employee must not have another active assignment whose time range
     overlaps the shift, including assignments in other teams.
6. **Minimum rest**
   - The gap before and after the new assignment must satisfy the minimum rest
     requirement.

The operation stops on the first failure and returns a stable error code and a
human-readable message.

### 8.2 Time overlap formula

Two half-open ranges overlap when:

```text
existing.start_time < new.end_time
AND existing.end_time > new.start_time
```

The end time itself is not considered part of a shift, so a shift ending at
10:00 does not overlap a shift starting at 10:00.

### 8.3 Minimum rest

Each shift stores `min_rest_hours`. For generated shifts, the value is copied
from the template and may be overridden by a manager.

For two adjacent shifts, the required gap is:

```text
max(previous_shift.min_rest_hours, next_shift.min_rest_hours)
```

This rule remains deterministic when an employee works for multiple teams with
different templates.

### 8.4 Time validity

- `end_time` must be later than `start_time`.
- A constraint must have a valid non-empty time range.
- A full-day constraint is stored using the start and end of the relevant local
  day.
- All backend timestamps are stored with an explicit time zone or as UTC.
- The application displays times in the organization's configured time zone.

### 8.5 Constraint submission

An employee cannot submit an unavailability constraint that overlaps one of
the employee's existing assignments. The employee must first arrange a swap,
transfer, or manager-approved assignment removal. This prevents a newly
submitted constraint from silently making a draft or published schedule
invalid.

---

## 9. Assignment Modes

### 9.1 Manual assignment

1. The manager selects an employee and a shift.
2. The server runs all validations in one transaction.
3. The assignment is saved only if all validations pass.
4. A validation failure returns HTTP `409 Conflict` for a scheduling conflict
   or HTTP `400 Bad Request` for malformed input.

### 9.2 Automatic assignment

The manager selects a schedule or a set of unfilled shifts.

The algorithm:

1. Sorts shifts by start time and then by shift ID.
2. Processes each unfilled position separately.
3. Finds team members who pass every assignment validation.
4. Ranks eligible employees by the fewest assigned hours in the schedule's
   date range across all teams.
5. Uses employee ID as a deterministic tie-breaker.
6. Assigns the highest-ranked employee.
7. Leaves a position unfilled if there is no eligible employee.

The result contains:

- Number of assignments created
- List of unfilled positions
- Reason summary for rejected candidates

Automatic assignment is transactional per assignment rather than for the
entire schedule, so one unfillable shift does not roll back all successful
assignments.

### 9.3 Recurring assignment

A recurring assignment rule links:

- One employee
- One template slot
- A start date
- An end date

When shifts are generated, the system attempts to assign that employee to every
generated occurrence matching the slot.

Each occurrence is validated independently. A conflicting occurrence remains
unassigned and is added to a recurring-assignment report. The system never
creates an invalid assignment merely because it is part of a recurring series.

Editing or deleting a recurring rule affects future generated occurrences only.
Existing assignments remain unchanged unless the manager explicitly requests
their removal.

---

## 10. Swap and Transfer Workflow

### 10.1 Request types

#### Full swap

Employee A proposes exchanging assignment A with assignment B belonging to
employee B.

#### Transfer

Employee A proposes transferring assignment A to employee B without receiving
another assignment.

Both request types are limited to one team:

- The target employee must be a current member of the source shift's team.
- For a full swap, both assignments must belong to schedules of the same team.

### 10.2 Approval policy

The source shift's team has one of two policies:

- `EMPLOYEE`: approval by the target employee is sufficient.
- `MANAGER`: the target employee must approve first, followed by a manager of
  the source shift's team.

The requester cannot approve the request on behalf of the target employee.

### 10.3 Request states

- `PENDING_EMPLOYEE`
- `PENDING_MANAGER`
- `APPROVED`
- `REJECTED`
- `CANCELLED`
- `INVALIDATED`

### 10.4 State transitions

For an `EMPLOYEE` approval policy:

```text
PENDING_EMPLOYEE -> APPROVED
PENDING_EMPLOYEE -> REJECTED
PENDING_EMPLOYEE -> CANCELLED
```

For a `MANAGER` approval policy:

```text
PENDING_EMPLOYEE -> PENDING_MANAGER -> APPROVED
PENDING_EMPLOYEE -> REJECTED
PENDING_MANAGER  -> REJECTED
PENDING_EMPLOYEE or PENDING_MANAGER -> CANCELLED
```

### 10.5 Execution rules

Before final approval, the server re-runs all assignment validations using the
current database state.

For a swap:

- Employee A must be eligible for assignment B.
- Employee B must be eligible for assignment A.
- Validation temporarily excludes the two assignments being exchanged.

For a transfer:

- Employee B must be eligible for assignment A.

If any validation fails, the request is changed to `INVALIDATED` and no
assignment is modified.

After a successful execution:

- Other pending requests involving either changed assignment are invalidated.
- The affected employees receive notifications.
- The schedule remains published if it was already published.

---

## 11. Concurrency and Transaction Rules

`@Version` alone does not protect rules that depend on multiple rows. The
system therefore combines optimistic locking, pessimistic locking, and database
constraints.

### 11.1 Optimistic locking

`@Version` is used on:

- `Schedule`
- `Shift`
- `Assignment`
- `SwapRequest`
- `RecurringAssignmentRule`

It detects stale updates to an existing entity and returns HTTP `409 Conflict`.

### 11.2 Assignment locking protocol

Before creating, deleting, swapping, or transferring assignments, the service
opens one database transaction and locks:

1. All involved `User` rows using `PESSIMISTIC_WRITE`, ordered by user ID.
2. All involved `Shift` rows using `PESSIMISTIC_WRITE`, ordered by shift ID.
3. Existing involved `Assignment` rows.

After acquiring the locks, the service repeats capacity, availability,
staffing-role, overlap, and rest validations and only then writes the change.

Locking the employee serializes concurrent assignment changes involving that
employee, including assignments in different teams. Locking the shift prevents
two managers from exceeding its required capacity.

### 11.3 Database constraints

The database includes:

- Unique `(user_id, shift_id)` on active assignments
- Unique `(user_id, team_id)` on team membership
- Unique `(manager_id, team_id)` on manager-team assignment
- Check constraint `shift.end_time > shift.start_time`
- Check constraint `constraint.end_time > constraint.start_time`
- Check constraint `required_workers > 0`
- Foreign keys for all relationships

Database constraints are the final protection against exact duplicates, while
the transactional locking protocol protects overlap, rest, and capacity rules.

### 11.4 Deadlock prevention

Whenever multiple users, shifts, or assignments are locked, IDs are sorted and
locked in ascending order.

### 11.5 Conflict response

A lock timeout, optimistic-lock failure, or newly detected scheduling conflict
returns HTTP `409 Conflict` with a stable error code. The frontend refreshes the
affected data and asks the manager to retry.

---

## 12. Notifications and Messaging

### 12.1 Selected technology

The project uses:

- Jakarta Messaging (JMS API)
- Spring JMS
- Apache ActiveMQ Artemis as the message broker

RabbitMQ is not used. It is an alternative broker commonly accessed through
AMQP and Spring AMQP. Using both ActiveMQ Artemis and RabbitMQ would duplicate
the messaging infrastructure without serving a separate project requirement.

### 12.2 Notification channel

The required notification channel is an in-application notification center.

Each notification contains:

- Recipient
- Type
- Title
- Message
- Related entity type and ID
- Creation time
- Read time

The React application displays unread notifications and periodically refreshes
them through the REST API.

Email delivery may be added later as another consumer, but it is outside the
required project scope.

### 12.3 Notification events

Events are created for:

- Schedule published or republished
- Employee added to a team
- Incoming swap or transfer request
- Request moved to manager approval
- Request approved, rejected, cancelled, or invalidated
- Recurring assignment occurrence rejected because of a conflict

### 12.4 Reliable event flow

The application uses a transactional outbox:

1. The business transaction updates domain data and inserts an
   `notification_outbox` row in the same database transaction.
2. An outbox dispatcher sends unsent events to the durable JMS queue
   `notification.events`.
3. A JMS consumer creates rows in `notifications`.
4. The consumer uses the unique `event_id` to process messages idempotently.
5. A message is acknowledged only after notification records are committed.
6. Failed messages are retried and eventually moved to a dead-letter queue.

This design prevents a successful publication or request approval from losing
its notification because the broker was temporarily unavailable.

### 12.5 Delivery semantics

Processing is at least once. Duplicate delivery is harmless because
`notifications.event_id` is unique and the consumer is idempotent.

JMS is used for asynchronous backend processing. It does not communicate
directly with the React browser.

---

## 13. Database Model

### `users`

| Column             | Type           | Notes                 |
| ------------------ | -------------- | --------------------- |
| `id`               | BIGINT PK      |                       |
| `username`         | VARCHAR UNIQUE |                       |
| `password_hash`    | VARCHAR        | BCrypt or Argon2      |
| `full_name`        | VARCHAR        |                       |
| `email`            | VARCHAR        | Optional              |
| `application_role` | ENUM           | `EMPLOYEE`, `MANAGER` |
| `version`          | BIGINT         | Optimistic locking    |

### `teams`

| Column                   | Type      | Notes                 |
| ------------------------ | --------- | --------------------- |
| `id`                     | BIGINT PK |                       |
| `name`                   | VARCHAR   |                       |
| `swap_approval_policy`   | ENUM      | `EMPLOYEE`, `MANAGER` |
| `default_min_rest_hours` | INT       | Non-negative          |
| `time_zone`              | VARCHAR   | IANA zone ID          |

### `team_members`

| Column      | Type      | Notes |
| ----------- | --------- | ----- |
| `id`        | BIGINT PK |       |
| `user_id`   | BIGINT FK |       |
| `team_id`   | BIGINT FK |       |
| `joined_at` | TIMESTAMP |       |
| `active`    | BOOLEAN   |       |

Unique constraint: `(user_id, team_id)`.

### `team_managers`

| Column       | Type      | Notes                     |
| ------------ | --------- | ------------------------- |
| `id`         | BIGINT PK |                           |
| `manager_id` | BIGINT FK | References a manager user |
| `team_id`    | BIGINT FK |                           |

Unique constraint: `(manager_id, team_id)`.

### `staffing_roles`

| Column    | Type      | Notes       |
| --------- | --------- | ----------- |
| `id`      | BIGINT PK |             |
| `team_id` | BIGINT FK | Team-scoped |
| `name`    | VARCHAR   |             |

Unique constraint: `(team_id, name)`.

### `employee_staffing_roles`

| Column             | Type      | Notes |
| ------------------ | --------- | ----- |
| `team_member_id`   | BIGINT FK |       |
| `staffing_role_id` | BIGINT FK |       |

### `employee_notes`

| Column              | Type      | Notes                         |
| ------------------- | --------- | ----------------------------- |
| `id`                | BIGINT PK |                               |
| `employee_id`       | BIGINT FK |                               |
| `team_id`           | BIGINT FK | Authorization context         |
| `author_manager_id` | BIGINT FK | Only this manager may read it |
| `text`              | TEXT      |                               |
| `created_at`        | TIMESTAMP |                               |
| `updated_at`        | TIMESTAMP |                               |

### `schedules`

| Column               | Type      | Notes                |
| -------------------- | --------- | -------------------- |
| `id`                 | BIGINT PK |                      |
| `team_id`            | BIGINT FK |                      |
| `start_date`         | DATE      |                      |
| `end_date`           | DATE      | Inclusive            |
| `status`             | ENUM      | `DRAFT`, `PUBLISHED` |
| `publication_number` | INT       | Starts at zero       |
| `published_at`       | TIMESTAMP | Nullable             |
| `version`            | BIGINT    | Optimistic locking   |

### `shift_templates`

| Column                   | Type      | Notes        |
| ------------------------ | --------- | ------------ |
| `id`                     | BIGINT PK |              |
| `team_id`                | BIGINT FK |              |
| `name`                   | VARCHAR   |              |
| `description`            | VARCHAR   | Optional     |
| `cycle_days`             | INT       | Positive     |
| `default_min_rest_hours` | INT       | Non-negative |
| `active`                 | BOOLEAN   |              |

### `template_slots`

| Column                      | Type      | Notes                          |
| --------------------------- | --------- | ------------------------------ |
| `id`                        | BIGINT PK |                                |
| `template_id`               | BIGINT FK |                                |
| `day_offset`                | INT       | `0 <= day_offset < cycle_days` |
| `start_time`                | TIME      |                                |
| `duration_minutes`          | INT       | Positive                       |
| `required_workers`          | INT       | Positive                       |
| `required_staffing_role_id` | BIGINT FK | Nullable                       |

### `shifts`

| Column                      | Type      | Notes                      |
| --------------------------- | --------- | -------------------------- |
| `id`                        | BIGINT PK |                            |
| `schedule_id`               | BIGINT FK |                            |
| `template_slot_id`          | BIGINT FK | Nullable                   |
| `parent_shift_id`           | BIGINT FK | Nullable, for split shifts |
| `start_time`                | TIMESTAMP |                            |
| `end_time`                  | TIMESTAMP |                            |
| `description`               | VARCHAR   |                            |
| `required_workers`          | INT       | Positive                   |
| `required_staffing_role_id` | BIGINT FK | Nullable                   |
| `min_rest_hours`            | INT       | Non-negative               |
| `version`                   | BIGINT    | Optimistic locking         |

### `shift_notes`

| Column              | Type      | Notes |
| ------------------- | --------- | ----- |
| `id`                | BIGINT PK |       |
| `shift_id`          | BIGINT FK |       |
| `author_manager_id` | BIGINT FK |       |
| `text`              | TEXT      |       |
| `created_at`        | TIMESTAMP |       |
| `updated_at`        | TIMESTAMP |       |

### `assignments`

| Column       | Type      | Notes                  |
| ------------ | --------- | ---------------------- |
| `id`         | BIGINT PK |                        |
| `user_id`    | BIGINT FK |                        |
| `shift_id`   | BIGINT FK |                        |
| `created_by` | BIGINT FK | Manager or system user |
| `created_at` | TIMESTAMP |                        |
| `version`    | BIGINT    | Optimistic locking     |

### `recurring_assignment_rules`

| Column             | Type      | Notes              |
| ------------------ | --------- | ------------------ |
| `id`               | BIGINT PK |                    |
| `user_id`          | BIGINT FK |                    |
| `template_slot_id` | BIGINT FK |                    |
| `start_date`       | DATE      |                    |
| `end_date`         | DATE      |                    |
| `active`           | BOOLEAN   |                    |
| `version`          | BIGINT    | Optimistic locking |

### `availability_constraints`

| Column       | Type      | Notes                |
| ------------ | --------- | -------------------- |
| `id`         | BIGINT PK |                      |
| `user_id`    | BIGINT FK |                      |
| `start_time` | TIMESTAMP |                      |
| `end_time`   | TIMESTAMP |                      |
| `full_day`   | BOOLEAN   |                      |
| `reason`     | TEXT      | Optional and private |
| `created_at` | TIMESTAMP |                      |

All rows represent unavailability; an unnecessary `is_available` flag is not
stored.

### `swap_requests`

| Column                 | Type      | Notes                    |
| ---------------------- | --------- | ------------------------ |
| `id`                   | BIGINT PK |                          |
| `type`                 | ENUM      | `SWAP`, `TRANSFER`       |
| `requester_id`         | BIGINT FK |                          |
| `source_assignment_id` | BIGINT FK |                          |
| `target_employee_id`   | BIGINT FK |                          |
| `target_assignment_id` | BIGINT FK | Required only for `SWAP` |
| `status`               | ENUM      | Workflow state           |
| `employee_approved_at` | TIMESTAMP | Nullable                 |
| `manager_approved_by`  | BIGINT FK | Nullable                 |
| `manager_approved_at`  | TIMESTAMP | Nullable                 |
| `created_at`           | TIMESTAMP |                          |
| `updated_at`           | TIMESTAMP |                          |
| `version`              | BIGINT    | Optimistic locking       |

### `notification_outbox`

| Column          | Type      | Notes           |
| --------------- | --------- | --------------- |
| `event_id`      | UUID PK   | Idempotency key |
| `event_type`    | VARCHAR   |                 |
| `payload`       | JSONB     |                 |
| `created_at`    | TIMESTAMP |                 |
| `sent_at`       | TIMESTAMP | Nullable        |
| `attempt_count` | INT       |                 |

### `notifications`

| Column                | Type      | Notes                 |
| --------------------- | --------- | --------------------- |
| `id`                  | BIGINT PK |                       |
| `event_id`            | UUID      | Unique with recipient |
| `recipient_id`        | BIGINT FK |                       |
| `type`                | VARCHAR   |                       |
| `title`               | VARCHAR   |                       |
| `message`             | TEXT      |                       |
| `related_entity_type` | VARCHAR   | Nullable              |
| `related_entity_id`   | BIGINT    | Nullable              |
| `created_at`          | TIMESTAMP |                       |
| `read_at`             | TIMESTAMP | Nullable              |

Unique constraint: `(event_id, recipient_id)`.

---

## 14. REST API

### Authentication

| Method | Endpoint          | Access | Description                 |
| ------ | ----------------- | ------ | --------------------------- |
| `POST` | `/api/auth/login` | Public | Authenticate and return JWT |

### Schedules and shifts

| Method   | Endpoint                        | Access  | Description            |
| -------- | ------------------------------- | ------- | ---------------------- |
| `GET`    | `/api/schedules`                | Both    | List visible schedules |
| `POST`   | `/api/teams/{teamId}/schedules` | Manager | Create draft           |
| `POST`   | `/api/schedules/{id}/publish`   | Manager | Publish or republish   |
| `POST`   | `/api/schedules/{id}/reopen`    | Manager | Reopen for editing     |
| `POST`   | `/api/schedules/{id}/shifts`    | Manager | Create shift           |
| `PUT`    | `/api/shifts/{id}`              | Manager | Update shift           |
| `DELETE` | `/api/shifts/{id}`              | Manager | Delete shift           |
| `POST`   | `/api/shifts/{id}/split`        | Manager | Split shift            |

### Assignments

| Method   | Endpoint                                | Access  | Description                    |
| -------- | --------------------------------------- | ------- | ------------------------------ |
| `POST`   | `/api/assignments`                      | Manager | Manual assignment              |
| `DELETE` | `/api/assignments/{id}`                 | Manager | Remove assignment              |
| `POST`   | `/api/schedules/{id}/auto-assign`       | Manager | Auto-assign selected shifts    |
| `GET`    | `/api/schedules/{id}/assignment-report` | Manager | Validation and capacity report |

### Recurring assignments

| Method   | Endpoint                          | Access  | Description        |
| -------- | --------------------------------- | ------- | ------------------ |
| `GET`    | `/api/recurring-assignments`      | Manager | List rules         |
| `POST`   | `/api/recurring-assignments`      | Manager | Create rule        |
| `PUT`    | `/api/recurring-assignments/{id}` | Manager | Update future rule |
| `DELETE` | `/api/recurring-assignments/{id}` | Manager | Disable rule       |

### Constraints

| Method   | Endpoint                | Access   | Description           |
| -------- | ----------------------- | -------- | --------------------- |
| `GET`    | `/api/constraints`      | Employee | View own history      |
| `POST`   | `/api/constraints`      | Employee | Submit unavailability |
| `DELETE` | `/api/constraints/{id}` | Employee | Delete own constraint |

### Swaps and transfers

| Method | Endpoint                              | Access   | Description              |
| ------ | ------------------------------------- | -------- | ------------------------ |
| `GET`  | `/api/requests`                       | Both     | List authorized requests |
| `POST` | `/api/requests/swaps`                 | Employee | Request full swap        |
| `POST` | `/api/requests/transfers`             | Employee | Request transfer         |
| `POST` | `/api/requests/{id}/employee-approve` | Employee | Target approval          |
| `POST` | `/api/requests/{id}/manager-approve`  | Manager  | Manager approval         |
| `POST` | `/api/requests/{id}/reject`           | Both     | Authorized rejection     |
| `POST` | `/api/requests/{id}/cancel`           | Employee | Requester cancellation   |

### Teams, members, and staffing roles

| Method   | Endpoint                                                   | Access  | Description        |
| -------- | ---------------------------------------------------------- | ------- | ------------------ |
| `GET`    | `/api/teams`                                               | Both    | List visible teams |
| `GET`    | `/api/teams/{id}/members`                                  | Both    | Read team members  |
| `POST`   | `/api/teams/{id}/members`                                  | Manager | Add employee       |
| `DELETE` | `/api/teams/{id}/members/{userId}`                         | Manager | Remove employee    |
| `PUT`    | `/api/teams/{id}/settings`                                 | Manager | Update team policy |
| `GET`    | `/api/teams/{id}/staffing-roles`                           | Manager | List roles         |
| `POST`   | `/api/teams/{id}/staffing-roles`                           | Manager | Create role        |
| `POST`   | `/api/teams/{id}/members/{userId}/staffing-roles`          | Manager | Assign role        |
| `DELETE` | `/api/teams/{id}/members/{userId}/staffing-roles/{roleId}` | Manager | Remove role        |

### Templates

| Method   | Endpoint                        | Access  | Description              |
| -------- | ------------------------------- | ------- | ------------------------ |
| `GET`    | `/api/teams/{teamId}/templates` | Manager | List templates           |
| `POST`   | `/api/teams/{teamId}/templates` | Manager | Create template          |
| `PUT`    | `/api/templates/{id}`           | Manager | Update template          |
| `DELETE` | `/api/templates/{id}`           | Manager | Delete template          |
| `POST`   | `/api/templates/{id}/slots`     | Manager | Add slot                 |
| `PUT`    | `/api/template-slots/{id}`      | Manager | Update slot              |
| `DELETE` | `/api/template-slots/{id}`      | Manager | Delete slot              |
| `POST`   | `/api/templates/{id}/generate`  | Manager | Generate schedule shifts |

### Notes

| Method   | Endpoint                                       | Access  | Description              |
| -------- | ---------------------------------------------- | ------- | ------------------------ |
| `GET`    | `/api/teams/{teamId}/employees/{userId}/notes` | Manager | Own employee notes       |
| `POST`   | `/api/teams/{teamId}/employees/{userId}/notes` | Manager | Add employee note        |
| `PUT`    | `/api/employee-notes/{id}`                     | Manager | Update own note          |
| `DELETE` | `/api/employee-notes/{id}`                     | Manager | Delete own note          |
| `GET`    | `/api/shifts/{shiftId}/notes`                  | Manager | Team-manager shift notes |
| `POST`   | `/api/shifts/{shiftId}/notes`                  | Manager | Add shift note           |
| `PUT`    | `/api/shift-notes/{id}`                        | Manager | Update own note          |
| `DELETE` | `/api/shift-notes/{id}`                        | Manager | Delete own note          |

### Notifications

| Method | Endpoint                          | Access | Description                     |
| ------ | --------------------------------- | ------ | ------------------------------- |
| `GET`  | `/api/notifications`              | Both   | List own notifications          |
| `GET`  | `/api/notifications/unread-count` | Both   | Get unread count                |
| `POST` | `/api/notifications/{id}/read`    | Both   | Mark own notification read      |
| `POST` | `/api/notifications/read-all`     | Both   | Mark all own notifications read |

---

## 15. Frontend Screens

### Login

- Username and password
- Role-aware navigation after successful login

### Schedule calendar

- Week and month views
- Team selector
- "My shifts only" filter
- Read-only indicator for other teams
- Draft/published status for authorized users
- Shift details without manager-only notes for employees

### Employee constraints

- Full-day or time-range form
- Optional reason
- Personal history and delete action

### Requests

- Create swap or transfer request
- Incoming employee approvals
- Manager approval queue
- Status and history

### Notification center

- Unread badge
- Notification list
- Mark one or all as read
- Link to the related schedule or request

### Manager schedule editor

- Create or reopen schedule
- Create, edit, delete, and split shifts
- Manual drag-and-drop or list-based assignment
- Automatic assignment
- Capacity and validation indicators
- Publish and republish actions

### Template and recurring assignment editor

- Template list and slot editor
- Calendar preview
- Generate shifts for date range
- Define recurring employee assignments
- View generation conflict report

### Team management

- Add and remove members
- Configure swap approval policy and default rest period
- Manage staffing roles
- Add private employee notes

---

## 16. Technology Stack

| Layer          | Technology                              |
| -------------- | --------------------------------------- |
| Frontend       | React                                   |
| Backend        | Java, Spring Boot                       |
| REST           | Spring Web                              |
| Validation     | Jakarta Validation                      |
| Security       | Spring Security, JWT                    |
| Persistence    | Spring Data JPA, Hibernate              |
| Database       | PostgreSQL                              |
| Messaging API  | Jakarta Messaging (JMS), Spring JMS     |
| Message broker | Apache ActiveMQ Artemis                 |
| Testing        | JUnit, Spring Boot Test, Testcontainers |

The broker should run as a separate local service or container. An embedded
broker may be used for focused integration tests.

---

## 17. Error Model

All error responses use:

```json
{
  "code": "ASSIGNMENT_OVERLAP",
  "message": "The employee already has an overlapping assignment",
  "details": {},
  "timestamp": "..."
}
```

Important status mappings:

| Condition                          | HTTP status        |
| ---------------------------------- | ------------------ |
| Invalid request format             | `400 Bad Request`  |
| Missing or invalid JWT             | `401 Unauthorized` |
| Insufficient role or team access   | `403 Forbidden`    |
| Entity not found                   | `404 Not Found`    |
| Scheduling or concurrency conflict | `409 Conflict`     |

---

## 18. Testing Requirements

### Unit tests

- Overlap boundary conditions
- Rest-period calculations
- Staffing-role validation
- Auto-assignment ranking
- Swap and transfer state transitions
- Notification payload creation

### Database integration tests

- JPA relationships and constraints
- Cross-team overlap checks
- Previous and next assignment queries using `Pageable` or derived
  `findFirst...` methods rather than JPQL `LIMIT`
- Transactional outbox persistence
- Idempotent notification consumer

### Concurrency integration tests

- Two managers assigning the same employee to overlapping shifts
- Two managers filling the final position of the same shift
- Two requests attempting to transfer or swap the same assignment
- Concurrent publish and reopen attempts

These tests must use a real PostgreSQL instance through Testcontainers because
in-memory databases do not reproduce PostgreSQL locking behavior accurately.

### Security integration tests

- Employee cannot access notes
- Manager cannot edit an unmanaged team
- Employee cannot approve a request addressed to another employee
- Manager cannot approve before required employee approval
- Other-team schedules remain read-only

### End-to-end tests

- Create schedule, assign employees, publish, and receive notifications
- Reopen, edit, republish, and receive a second publication notification
- Complete an employee-approved transfer
- Complete an employee-and-manager-approved swap

---

## 19. Recommended Implementation Order

### Phase 1 - Core domain and security

- Users, teams, memberships, and manager assignments
- JWT authentication and team-scoped authorization
- Schedules, shifts, and assignments
- Manual assignment validations

### Phase 2 - Scheduling workflows

- Availability constraints
- Staffing roles
- Templates and shift generation
- Automatic assignment
- Recurring assignments
- Employee and shift notes

### Phase 3 - Swap and transfer workflows

- Request state machine
- Employee and manager approvals
- Atomic execution and invalidation
- Concurrency tests

### Phase 4 - Publication and notifications

- Schedule lifecycle
- Outbox table and dispatcher
- ActiveMQ Artemis
- JMS consumer and internal notifications

### Phase 5 - React application

- Authentication and navigation
- Employee schedule, constraints, requests, and notifications
- Manager schedule editor, templates, team management, and notes

### Phase 6 - Hardening

- Full integration and end-to-end tests
- Error handling
- Audit and logging
- UI validation and conflict refresh behavior

---

## 20. Explicit Non-Goals

The initial project does not include:

- Payroll calculation
- Attendance or clock-in tracking
- SMS or mobile push notifications
- Email delivery, although the messaging design allows it to be added later
- Automatic interpretation of free-text employee or shift notes
- Optimization algorithms that guarantee a globally optimal schedule
- Editing another team's schedules without manager authorization
