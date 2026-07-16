# Current Backend Architecture

This document describes the backend components that currently exist in the project.
It intentionally documents the implemented state only, not future planned features.

## System Context

```mermaid
flowchart LR
    client["API Client<br/>Postman now, React later"]
    security["Spring Security<br/>JWT Authentication Filter"]
    controllers["REST Controllers"]
    services["Application Services<br/>Business Rules"]
    repositories["Spring Data JPA Repositories"]
    database["PostgreSQL Database"]
    flyway["Flyway Migrations"]
    seed["Development Data Seeder"]

    client --> security
    security --> controllers
    controllers --> services
    services --> repositories
    repositories --> database
    flyway --> database
    seed --> repositories
```

## Backend Packages

```mermaid
flowchart TD
    app["ShiftManagementApplication"]

    config["config<br/>SecurityConfig<br/>DevelopmentDataSeeder"]
    health["health<br/>HealthController"]
    auth["auth<br/>Login, JWT, current user"]
    user["user<br/>User and application role"]
    team["team<br/>Team, team members, managers"]
    schedule["schedule<br/>Draft schedule creation"]
    shift["shift<br/>Shift CRUD inside schedules"]
    assignment["assignment<br/>Manual assignment workflow and validations"]
    availability["availability<br/>Employee unavailable time ranges"]
    staffing["staffing<br/>Team staffing roles and member-role links"]

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

    auth --> user
    schedule --> team
    shift --> schedule
    assignment --> shift
    assignment --> team
    assignment --> user
    assignment --> availability
    availability --> user
    availability --> assignment
    staffing --> team
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
For example, `staffing` currently has entities and repositories only, with no REST API yet.

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

    TEAM_MEMBERS ||--o{ TEAM_MEMBER_STAFFING_ROLES : receives
    STAFFING_ROLES ||--o{ TEAM_MEMBER_STAFFING_ROLES : assigned

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
```

## Implemented API Areas

```mermaid
flowchart TD
    api["REST API"]

    healthApi["Health<br/>GET /api/health"]
    authApi["Authentication<br/>POST /api/auth/login<br/>GET /api/auth/me"]
    schedulesApi["Schedules<br/>POST /api/schedules"]
    shiftsApi["Shifts<br/>POST /api/schedules/{scheduleId}/shifts<br/>GET /api/schedules/{scheduleId}/shifts<br/>PUT /api/schedules/{scheduleId}/shifts/{shiftId}<br/>DELETE /api/schedules/{scheduleId}/shifts/{shiftId}"]
    assignmentsApi["Assignments<br/>POST /api/assignments<br/>GET /api/schedules/{scheduleId}/assignments<br/>DELETE /api/assignments/{assignmentId}"]
    availabilityApi["Availability Constraints<br/>POST /api/availability-constraints<br/>GET /api/availability-constraints/me<br/>DELETE /api/availability-constraints/{constraintId}"]

    api --> healthApi
    api --> authApi
    api --> schedulesApi
    api --> shiftsApi
    api --> assignmentsApi
    api --> availabilityApi
```

Staffing roles do not have API endpoints yet.
They currently exist only as persistence components.

## Main Request Flow Examples

### Login

```mermaid
sequenceDiagram
    participant Client
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

### Manual Assignment

```mermaid
sequenceDiagram
    participant Client
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
    AssignmentService->>AssignmentService: validate capacity, availability, overlap, rest
    AssignmentService->>Repositories: save Assignment
    Repositories->>Database: insert assignment
    AssignmentService-->>AssignmentController: AssignmentResponse
    AssignmentController-->>Client: 201 Created
```

### Availability Constraint

```mermaid
sequenceDiagram
    participant Client
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

    v1 --> v2 --> v3 --> v4 --> v5 --> v6 --> v7
```

## Component Responsibilities

| Area | Responsibility |
| --- | --- |
| `auth` | Login, JWT creation, JWT request authentication, current user endpoint. |
| `config` | Security configuration and local development seed data. |
| `health` | Public health check endpoint. |
| `user` | User entity and broad application role such as `MANAGER` or `EMPLOYEE`. |
| `team` | Teams, active team membership, and team managers. |
| `schedule` | Draft schedule creation and schedule lifecycle state fields. |
| `shift` | Shift creation, listing, update, deletion, and schedule-range validation. |
| `assignment` | Manual assignment creation/list/delete and business rule validation. |
| `availability` | Employee unavailable time ranges and conflict checks with assignments. |
| `staffing` | Team-specific professional roles and persistence for assigning those roles to team members. |
| Flyway migrations | Versioned PostgreSQL schema changes. |
| PostgreSQL | Persistent relational storage. |
