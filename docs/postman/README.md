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

Alternative transfer endings:

- `Transfer Requests / Reject Transfer As Target Employee` can be run instead of employee approval.
- `Transfer Requests / Cancel Transfer As Requester` can be run by the requester while the request is still active.

Some requests save IDs into the environment automatically, such as `scheduleId`,
`shiftId`, `assignmentId`, and `transferRequestId`.

## Editing A Shift

`PUT` now requires a non-negative `version` from the shift you read. Re-import
the updated collection; existing requests without this field return `400`.
`Create Shift` saves both `shiftId` and `shiftVersion`. For an existing shift,
set `scheduleId` and `shiftId`, then run `List Schedule Shifts`; it saves only the
selected shift's version. Review its current fields and adapt the example dates,
hours, capacity, rest, and role in `Update Shift` before sending it.

On success, `Update Shift` stores the returned version for the next edit. A
`409` with `code: STALE_VERSION` means another edit was saved first. Reload with
`List Schedule Shifts`, review what changed, and reapply your intended edits.
Do not just replace the version and resend an old body: that can deliberately
overwrite newer data. No pre-request script silently refreshes the version.

To demonstrate the protection on a test draft, note the current `shiftVersion`,
save one edit, then put the noted old number directly in a second edit's `version`
field. Expect `409 STALE_VERSION`; listing the shifts must still show the first
edit. Restore `{{shiftVersion}}` in the body afterwards. A deleted shift returns
`404`; missing/null/negative versions return `400`.

## Confirmed Deletions

Re-import the updated collection and environment (preserve any local tokens
separately). DELETE now requires a revision, not only an ID:

1. Choose `scheduleId` and run **Preview Draft Deletion**, or choose
   `shiftTemplateId` and run **Preview Template Deletion** as the team's manager.
   For a shift choose `scheduleId` and `shiftId`, then **Preview Shift Deletion**.
   For an assignment choose `assignmentId`, then **Preview Assignment Deletion**.
2. Review the identity, dates where applicable, and child counts. A successful
   preview stores the matching `scheduleDeletionRevision`, `templateDeletionRevision`,
   `shiftDeletionRevision`, or `assignmentDeletionRevision`. Assignment previews
   show the employee and shift times, not just a removal count.
3. Run the matching DELETE request without changing the selected ID. Expect `204`.
4. If it returns `409`, review what changed with another explicit preview before
   deciding to delete again. DELETE does not fetch a new revision automatically.

To demonstrate stale confirmation on disposable data: preview a draft, create a
shift inside it, then send DELETE with the saved revision. Expect `409` and verify
the draft and new shift still exist. The analogous template scenario adds a slot
after preview. Same-count edits/replacements also invalidate the revision.
Preview clears its old revision before sending, so a failed preview cannot leave
a previously saved value ready for reuse. Missing/invalid revisions return `400`.
Used templates and drafts/shifts/assignments with request history are not deletable.
Individual shift previews also become stale after an assignment is added or
replaced. Assignment previews become stale after shift edits or publication and
reopening. A repeated successful deletion returns `404`.

## Seed Users

After explicit demo initialization on an empty database, these users are
available (see `../RUN_LOCALLY.md`). Initialization is disabled by default and
skips databases that already contain application data. Importing this collection
or sending a login request does not create accounts:

| Username | Password | Role |
| --- | --- | --- |
| `manager1` | `password` | `MANAGER` |
| `employee1` | `password` | `EMPLOYEE` |
| `employee2` | `password` | `EMPLOYEE` |

Do not commit real credentials or exported environments that contain real
tokens.
