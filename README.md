# Shift Management System

A course project for managing employee shifts across teams.

The project is being implemented gradually. The current focus is a working
Spring Boot backend with authentication, teams, schedules, shifts, manual
assignment rules, and availability constraint support.

## Repository Structure

- `shift-management-backend/` - Spring Boot backend.
- `IMPLEMENTATION_PLAN.md` - step-by-step implementation plan and current status.
- `spec-revised.md` - main project specification.

## Current Backend Status

Implemented:

- JWT login and authenticated API access.
- Users, teams, team members, and team managers.
- Draft schedule creation.
- Shift create, list, update, and delete operations.
- Manual assignment create, list, and delete operations.
- Assignment validations for team membership, duplicate assignments, shift capacity, overlap, and minimum rest.
- Availability constraint persistence model.
- Availability constraint create, personal list, and delete operations.
- Availability constraint creation is rejected when it overlaps an existing assignment.
- Assignment creation is rejected when it overlaps an employee availability constraint.

Planned next:

- Schedule publication and reopening.
- Basic React frontend.
- Swap and transfer requests.
- Notifications and JMS integration.

## Backend Documentation

Backend setup, run instructions, and API examples are documented in:

```text
shift-management-backend/README.md
```

## Development Approach

The project is built in small phases. Each phase adds a limited piece of
functionality, tests the business rules, and updates the documentation before
moving to the next feature.
