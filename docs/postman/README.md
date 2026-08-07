# Postman Collection

This folder contains a local Postman setup for testing the Shift Management API.

## Files

- `shift-management-api.postman_collection.json` - API requests grouped by feature.
- `local.postman_environment.json` - local development variables.

The environment intentionally does not contain a real token. Login requests save
the returned JWT into `accessToken` automatically.

## Import

1. Open Postman.
2. Import `shift-management-api.postman_collection.json`.
3. Import `local.postman_environment.json`.
4. Select the `Shift Management Local` environment.
5. Make sure the backend is running on `http://localhost:8080`.

If the backend is running on another port, update the environment `baseUrl`.

## Suggested Demo Flow

1. `Health / Health Check`
2. `Auth / Login as Manager`
3. `Teams / List My Managed Teams`
4. `Teams / List Team Employees`
5. `Schedules / Create Draft Schedule`
6. `Shifts / Create Shift`
7. `Assignments / Create Assignment`
8. `Schedules / Publication Readiness`
9. `Schedules / Publish Schedule`
10. `Auth / Login as Employee One`
11. `Transfer Requests / Create Transfer Request`
12. `Auth / Login as Employee Two`
13. `Transfer Requests / Approve Transfer As Target Employee`
14. `Auth / Login as Manager`
15. `Transfer Requests / Approve Transfer As Manager`

Some requests save IDs into the environment automatically, such as `scheduleId`,
`shiftId`, `assignmentId`, and `transferRequestId`.

## Seed Users

When development seed data is enabled, these users are available:

| Username | Password | Role |
| --- | --- | --- |
| `manager1` | `password` | `MANAGER` |
| `employee1` | `password` | `EMPLOYEE` |
| `employee2` | `password` | `EMPLOYEE` |

Do not commit real credentials or exported environments that contain real
tokens.
