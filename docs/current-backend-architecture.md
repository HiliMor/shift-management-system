# Current Backend Architecture

This document describes the backend components that currently exist in the project
and how the initial React frontend connects to them. It intentionally documents
the implemented state only, not future planned features.

## System Context

```mermaid
flowchart LR
    react["React Frontend<br/>Login, schedules, and manager workflows"]
    apiClient["API Client<br/>Postman or curl"]
    security["Spring Security<br/>JWT Authentication Filter"]
    cors["CORS Configuration<br/>Local frontend access"]
    controllers["REST Controllers"]
    services["Application Services<br/>Business Rules"]
    outbox["Event Outbox<br/>Pending async events"]
    repositories["Spring Data JPA Repositories"]
    database["PostgreSQL Database"]
    flyway["Flyway Migrations"]
    seed["Development Data Seeder"]

    react --> cors
    apiClient --> security
    cors --> security
    security --> controllers
    controllers --> services
    services --> outbox
    services --> repositories
    outbox --> database
    repositories --> database
    flyway --> database
    seed --> repositories
```

## Frontend Integration

The current frontend is intentionally small. It is responsible for:

- Displaying the login form.
- Calling `POST /api/auth/login`.
- Storing the returned JWT in browser local storage.
- Sending the JWT as a bearer token for authenticated API calls.
- Showing a manager or employee workspace based on the authenticated user's role.
- Loading the signed-in user's published schedules from `GET /api/schedules/me/published`.
- Loading a manager's teams from `GET /api/teams/me/managed`.
- Creating draft schedules through `POST /api/schedules`.
- Loading managed draft schedules from `GET /api/schedules/me/managed/drafts`.
- Creating shifts through `POST /api/schedules/{scheduleId}/shifts`.
- Loading draft schedule shifts from `GET /api/schedules/{scheduleId}/shifts`.
- Loading active team employees from `GET /api/teams/{teamId}/employees`.
- Loading draft schedule assignments from `GET /api/schedules/{scheduleId}/assignments`.
- Creating manual assignments through `POST /api/assignments`.

The backend remains the authority for authentication, authorization, validation,
business rules, and persistence.

## Backend Packages

```mermaid
flowchart TD
    app["ShiftManagementApplication"]

    config["config<br/>SecurityConfig<br/>DevelopmentDataSeeder"]
    health["health<br/>HealthController"]
    auth["auth<br/>Login, JWT, current user"]
    user["user<br/>User and application role"]
    team["team<br/>Team, team members, managers, managed team listing, and team employee listing"]
    schedule["schedule<br/>Draft schedule creation, publication, reopening, readiness, and employee published views"]
    shift["shift<br/>Shift CRUD inside schedules"]
    assignment["assignment<br/>Manual assignment workflow and validations"]
    availability["availability<br/>Employee unavailable time ranges"]
    staffing["staffing<br/>Team staffing roles, member-role links, and role assignment API"]
    messaging["messaging<br/>Event outbox for pending asynchronous events"]
    notification["notification<br/>Personal notification model and API"]

    app --> config
    app --> health
    app --> auth
    app --> user
    app --> team
    app --> schedule
    app --> shift
    app --> assignment
    app --> availability
    app --> staffing
    app --> messaging
    app --> notification

    auth --> user
    schedule --> team
    schedule --> shift
    schedule --> assignment
    schedule --> messaging
    shift --> schedule
    assignment --> shift
    assignment --> team
    assignment --> user
    assignment --> availability
    assignment --> staffing
    availability --> user
    availability --> assignment
    staffing --> team
    notification --> user
```

## Layer Pattern

Most feature packages follow this pattern:

```mermaid
flowchart TD
    request["HTTP Request"]
    controller["Controller<br/>Receives REST request"]
    requestDto["Request DTO<br/>Input shape and validation annotations"]
    service["Service<br/>Business rules and authorization checks"]
    repository["Repository<br/>Database access"]
    entity["Entity<br/>Persisted domain state"]
    responseDto["Response DTO<br/>Output shape"]
    response["HTTP Response"]

    request --> controller
    controller --> requestDto
    controller --> service
    service --> repository
    repository --> entity
    service --> responseDto
    responseDto --> response
```

Not every package has every layer yet.
For example, the basic schedule lifecycle, publication readiness report, explicit unfilled-publication confirmation, and employee published schedule views are already implemented.

## Domain Model

```mermaid
erDiagram
    USERS ||--o{ TEAM_MEMBERS : joins
    USERS ||--o{ TEAM_MANAGERS : manages
    TEAMS ||--o{ TEAM_MEMBERS : has
    TEAMS ||--o{ TEAM_MANAGERS : has
    TEAMS ||--o{ SCHEDULES : owns
    TEAMS ||--o{ STAFFING_ROLES : defines

    SCHEDULES ||--o{ SHIFTS : contains
    SHIFTS ||--o{ ASSIGNMENTS : receives
    USERS ||--o{ ASSIGNMENTS : assigned
    USERS ||--o{ AVAILABILITY_CONSTRAINTS : declares
    USERS ||--o{ NOTIFICATIONS : receives

    TEAM_MEMBERS ||--o{ TEAM_MEMBER_STAFFING_ROLES : receives
    STAFFING_ROLES ||--o{ TEAM_MEMBER_STAFFING_ROLES : assigned
    STAFFING_ROLES |o--o{ SHIFTS : may_be_required_by

    USERS {
        bigint id PK
        varchar username
        varchar password_hash
        varchar full_name
        varchar email
        varchar application_role
    }

    TEAMS {
        bigint id PK
        varchar name
        varchar swap_approval_policy
        integer default_min_rest_hours
        varchar time_zone
    }

    TEAM_MEMBERS {
        bigint id PK
        bigint user_id FK
        bigint team_id FK
        timestamptz joined_at
        boolean active
    }

    TEAM_MANAGERS {
        bigint id PK
        bigint manager_id FK
        bigint team_id FK
    }

    SCHEDULES {
        bigint id PK
        bigint team_id FK
        date start_date
        date end_date
        varchar status
        integer publication_number
        timestamptz published_at
    }

    SHIFTS {
        bigint id PK
        bigint schedule_id FK
        timestamptz start_time
        timestamptz end_time
        varchar description
        integer required_workers
        integer min_rest_hours
        bigint required_staffing_role_id FK
    }

    ASSIGNMENTS {
        bigint id PK
        bigint shift_id FK
        bigint employee_id FK
        timestamptz assigned_at
    }

    AVAILABILITY_CONSTRAINTS {
        bigint id PK
        bigint employee_id FK
        timestamptz start_time
        timestamptz end_time
        varchar reason
        timestamptz created_at
    }

    STAFFING_ROLES {
        bigint id PK
        bigint team_id FK
        varchar name
        varchar description
    }

    TEAM_MEMBER_STAFFING_ROLES {
        bigint id PK
        bigint team_member_id FK
        bigint staffing_role_id FK
        timestamptz assigned_at
    }

    NOTIFICATIONS {
        bigint id PK
        uuid event_id
        bigint recipient_id FK
        varchar type
        varchar title
        text message
        varchar related_entity_type
        bigint related_entity_id
        timestamptz created_at
        timestamptz read_at
    }

    EVENT_OUTBOX {
        uuid event_id PK
        varchar event_type
        jsonb payload
        timestamptz created_at
        timestamptz sent_at
        integer attempt_count
    }
```

## Implemented API Areas

```mermaid
flowchart TD
    api["REST API"]

    healthApi["Health<br/>GET /api/health"]
    authApi["Authentication<br/>POST /api/auth/login<br/>GET /api/auth/me"]
    teamsApi["Teams<br/>GET /api/teams/me/managed"]
    schedulesApi["Schedules<br/>POST /api/schedules<br/>GET /api/schedules/me/published<br/>GET /api/schedules/me/published/{scheduleId}<br/>GET /api/schedules/me/managed/drafts<br/>GET /api/schedules/{scheduleId}/publication-readiness<br/>POST /api/schedules/{scheduleId}/publish<br/>POST /api/schedules/{scheduleId}/reopen"]
    shiftsApi["Shifts<br/>POST /api/schedules/{scheduleId}/shifts<br/>GET /api/schedules/{scheduleId}/shifts<br/>PUT /api/schedules/{scheduleId}/shifts/{shiftId}<br/>DELETE /api/schedules/{scheduleId}/shifts/{shiftId}"]
    assignmentsApi["Assignments<br/>POST /api/assignments<br/>GET /api/schedules/{scheduleId}/assignments<br/>DELETE /api/assignments/{assignmentId}"]
    availabilityApi["Availability Constraints<br/>POST /api/availability-constraints<br/>GET /api/availability-constraints/me<br/>DELETE /api/availability-constraints/{constraintId}"]
    staffingApi["Staffing Roles<br/>POST /api/teams/{teamId}/staffing-roles<br/>GET /api/teams/{teamId}/staffing-roles<br/>POST /api/teams/{teamId}/employees/{employeeId}/staffing-roles<br/>GET /api/teams/{teamId}/employees/{employeeId}/staffing-roles"]
    notificationApi["Notifications<br/>GET /api/notifications<br/>GET /api/notifications/unread-count<br/>POST /api/notifications/{notificationId}/read"]

    api --> healthApi
    api --> authApi
    api --> teamsApi
    api --> schedulesApi
    api --> shiftsApi
    api --> assignmentsApi
    api --> availabilityApi
    api --> staffingApi
    api --> notificationApi
```

Assignment creation validates required staffing roles when a shift has a professional role requirement.

## Main Request Flow Examples

### Login

```mermaid
sequenceDiagram
    participant Client as React or API Client
    participant AuthController
    participant AuthService
    participant UserRepository
    participant JwtService

    Client->>AuthController: POST /api/auth/login
    AuthController->>AuthService: login(username, password)
    AuthService->>UserRepository: findByUsername(username)
    AuthService->>AuthService: validate password
    AuthService->>JwtService: generate token
    JwtService-->>AuthService: JWT
    AuthService-->>AuthController: LoginResponse
    AuthController-->>Client: 200 OK
```

### Schedule Publication

```mermaid
sequenceDiagram
    participant Client as React or API Client
    participant Security as JwtAuthenticationFilter
    participant ScheduleController
    participant ScheduleService
    participant ScheduleRepository
    participant EventOutboxService
    participant EventOutboxRepository

    Client->>Security: POST /api/schedules/{scheduleId}/publish with Bearer token
    Security->>ScheduleController: authenticated request
    ScheduleController->>ScheduleService: publishSchedule(username, scheduleId, confirmUnfilled)
    ScheduleService->>ScheduleRepository: findById(scheduleId)
    ScheduleService->>ScheduleService: validate manager and draft status
    ScheduleService->>ScheduleService: require readiness or explicit unfilled confirmation
    ScheduleService->>ScheduleService: mark schedule PUBLISHED
    ScheduleService->>EventOutboxService: createEvent("schedule.published", payload)
    EventOutboxService->>EventOutboxRepository: save pending event
    ScheduleService-->>ScheduleController: ScheduleResponse
    ScheduleController-->>Client: 200 OK
```

### Notification List And Read State

```mermaid
sequenceDiagram
    participant Client as React or API Client
    participant Security as JwtAuthenticationFilter
    participant NotificationController
    participant NotificationService
    participant NotificationRepository

    Client->>Security: GET /api/notifications with Bearer token
    Security->>NotificationController: authenticated request
    NotificationController->>NotificationService: listMyNotifications(username)
    NotificationService->>NotificationRepository: findByRecipient_UsernameOrderByCreatedAtDesc(username)
    NotificationService-->>NotificationController: NotificationResponse list
    NotificationController-->>Client: 200 OK

    Client->>Security: POST /api/notifications/{notificationId}/read with Bearer token
    Security->>NotificationController: authenticated request
    NotificationController->>NotificationService: markMyNotificationRead(username, notificationId)
    NotificationService->>NotificationRepository: findByIdAndRecipient_Username(notificationId, username)
    NotificationService->>NotificationService: set readAt if unread
    NotificationService-->>NotificationController: NotificationResponse
    NotificationController-->>Client: 200 OK
```

### Publication Readiness

```mermaid
sequenceDiagram
    participant Client as React or API Client
    participant Security as JwtAuthenticationFilter
    participant ScheduleController
    participant ScheduleService
    participant ScheduleRepository
    participant ShiftRepository
    participant AssignmentRepository

    Client->>Security: GET /api/schedules/{scheduleId}/publication-readiness with Bearer token
    Security->>ScheduleController: authenticated request
    ScheduleController->>ScheduleService: getPublicationReadiness(username, scheduleId)
    ScheduleService->>ScheduleRepository: findById(scheduleId)
    ScheduleService->>ScheduleService: validate manager access
    ScheduleService->>ShiftRepository: find shifts in schedule order
    ScheduleService->>AssignmentRepository: find assignments for schedule shifts
    ScheduleService->>ScheduleService: calculate required workers, assigned workers, and open slots
    ScheduleService-->>ScheduleController: SchedulePublicationReadinessResponse
    ScheduleController-->>Client: 200 OK
```

### Schedule Reopening

```mermaid
sequenceDiagram
    participant Client as React or API Client
    participant Security as JwtAuthenticationFilter
    participant ScheduleController
    participant ScheduleService
    participant ScheduleRepository

    Client->>Security: POST /api/schedules/{scheduleId}/reopen with Bearer token
    Security->>ScheduleController: authenticated request
    ScheduleController->>ScheduleService: reopenSchedule(username, scheduleId)
    ScheduleService->>ScheduleRepository: findById(scheduleId)
    ScheduleService->>ScheduleService: validate manager and published status
    ScheduleService->>ScheduleService: mark schedule DRAFT
    ScheduleService-->>ScheduleController: ScheduleResponse
    ScheduleController-->>Client: 200 OK
```

### Employee Published Schedule List

```mermaid
sequenceDiagram
    participant Client as React or API Client
    participant Security as JwtAuthenticationFilter
    participant ScheduleController
    participant ScheduleService
    participant UserRepository
    participant TeamMemberRepository
    participant ScheduleRepository

    Client->>Security: GET /api/schedules/me/published with Bearer token
    Security->>ScheduleController: authenticated request
    ScheduleController->>ScheduleService: listPublishedSchedulesForUser(username)
    ScheduleService->>UserRepository: findByUsername(username)
    ScheduleService->>TeamMemberRepository: find active team memberships
    ScheduleService->>ScheduleRepository: find PUBLISHED schedules for active teams
    ScheduleService-->>ScheduleController: ScheduleResponse list
    ScheduleController-->>Client: 200 OK
```

### Employee Published Schedule Details

```mermaid
sequenceDiagram
    participant Client as React or API Client
    participant Security as JwtAuthenticationFilter
    participant ScheduleController
    participant ScheduleService
    participant UserRepository
    participant ScheduleRepository
    participant TeamMemberRepository
    participant ShiftRepository
    participant AssignmentRepository

    Client->>Security: GET /api/schedules/me/published/{scheduleId} with Bearer token
    Security->>ScheduleController: authenticated request
    ScheduleController->>ScheduleService: getPublishedScheduleDetailsForUser(username, scheduleId)
    ScheduleService->>UserRepository: findByUsername(username)
    ScheduleService->>ScheduleRepository: findById(scheduleId)
    ScheduleService->>ScheduleService: require PUBLISHED status
    ScheduleService->>TeamMemberRepository: confirm active team membership
    ScheduleService->>ShiftRepository: find shifts in schedule order
    ScheduleService->>AssignmentRepository: find assignments for schedule shifts
    ScheduleService-->>ScheduleController: PublishedScheduleDetailsResponse
    ScheduleController-->>Client: 200 OK
```

### Manual Assignment

```mermaid
sequenceDiagram
    participant Client as React or API Client
    participant Security as JwtAuthenticationFilter
    participant AssignmentController
    participant AssignmentService
    participant Repositories
    participant Database

    Client->>Security: POST /api/assignments with Bearer token
    Security->>AssignmentController: authenticated request
    AssignmentController->>AssignmentService: createAssignment(username, request)
    AssignmentService->>Repositories: load manager, shift, employee, team membership
    Repositories->>Database: queries
    AssignmentService->>AssignmentService: validate manager, draft schedule, team membership
    AssignmentService->>AssignmentService: validate staffing role, capacity, availability, overlap, rest
    AssignmentService->>Repositories: save Assignment
    Repositories->>Database: insert assignment
    AssignmentService-->>AssignmentController: AssignmentResponse
    AssignmentController-->>Client: 201 Created
```

### Availability Constraint

```mermaid
sequenceDiagram
    participant Client as React or API Client
    participant AvailabilityConstraintController
    participant AvailabilityConstraintService
    participant AssignmentRepository
    participant AvailabilityConstraintRepository

    Client->>AvailabilityConstraintController: POST /api/availability-constraints
    AvailabilityConstraintController->>AvailabilityConstraintService: createConstraint(username, request)
    AvailabilityConstraintService->>AvailabilityConstraintService: validate time range
    AvailabilityConstraintService->>AssignmentRepository: find overlapping assignments
    AssignmentRepository-->>AvailabilityConstraintService: overlaps or empty list
    AvailabilityConstraintService->>AvailabilityConstraintRepository: save constraint
    AvailabilityConstraintRepository-->>AvailabilityConstraintService: saved constraint
    AvailabilityConstraintService-->>AvailabilityConstraintController: AvailabilityConstraintResponse
    AvailabilityConstraintController-->>Client: 201 Created
```

## Database Migration Timeline

```mermaid
flowchart LR
    v1["V1<br/>Users and teams"]
    v2["V2<br/>Schedules"]
    v3["V3<br/>Shifts"]
    v4["V4<br/>Assignments"]
    v5["V5<br/>Availability constraints"]
    v6["V6<br/>Staffing roles"]
    v7["V7<br/>Team member staffing roles"]
    v8["V8<br/>Required staffing role on shifts"]
    v9["V9<br/>Notifications and event outbox"]

    v1 --> v2 --> v3 --> v4 --> v5 --> v6 --> v7 --> v8 --> v9
```

## Component Responsibilities

| Area | Responsibility |
| --- | --- |
| `auth` | Login, JWT creation, JWT request authentication, current user endpoint. |
| `config` | Security configuration and local development seed data. |
| `health` | Public health check endpoint. |
| `user` | User entity and broad application role such as `MANAGER` or `EMPLOYEE`. |
| `team` | Teams, active team membership, team managers, and managed team listing for manager UI. |
| `schedule` | Draft schedule creation, managed draft schedule listing, schedule publication, explicit unfilled-publication confirmation, schedule reopening, publication readiness, employee published schedule list/details, and schedule lifecycle state fields. |
| `shift` | Shift creation, listing, update, deletion, schedule-range validation, and optional required staffing role storage. |
| `assignment` | Manual assignment creation/list/delete and business rule validation, including capacity, availability, overlap, rest, and required staffing roles. |
| `availability` | Employee unavailable time ranges and conflict checks with assignments. |
| `staffing` | Team-specific professional roles, role create/list API, employee role assignment/list API, and persistence for assigning roles to team members. |
| `messaging` | Event outbox persistence and event creation for future JMS delivery. |
| `notification` | Personal notifications, unread count, mark-as-read behavior, and idempotent notification creation. |
| Flyway migrations | Versioned PostgreSQL schema changes. |
| PostgreSQL | Persistent relational storage. |
