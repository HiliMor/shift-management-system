# Shift Management Frontend

Minimal React frontend for the shift management system.

For end-to-end local run instructions, see:

```text
../docs/RUN_LOCALLY.md
```

## Current Screens

- Login screen - public screen for signing in with a backend user.
- Expired session handling - expired JWT sessions are cleared and return to the login screen.
- Published schedules - authenticated screen that lists published schedules visible to the signed-in user.
- Schedule details - authenticated screen section that shows shifts and assignments for a selected published schedule.
- Published schedule viewing - Sunday-first weekly and monthly calendars plus a list. Month navigation stays within the schedule range. Employees can apply `Only my shifts` to all three views; coworkers in those shifts remain visible. Managers retain their read-only published view without the personal filter.
- Calendar dates and shift times use the browser's local timezone, as in the existing weekly view. Overnight shifts appear on their start date. Cross-team access rules have not changed.
- Manager published schedule view - manager-only read-only view of a selected published schedule and its assignments.
- Create schedule - manager-only form for creating a draft schedule for a managed team.
- Team employees - manager-only navigation entry for creating a new employee in a managed team. Name, username, and password are required; email and existing team staffing roles are optional. The new account can log in immediately and appears in the assignment employee list after creation.
- Employee creation clears credentials after success or team changes, blocks repeated submission while saving, and shows duplicate-username errors beside the form. Editing/removing existing members, adding existing accounts, invitations, password resets, and team-join notifications are outside this increment.
- Username format and uniqueness requirements are displayed beside the creation field in Hebrew/English and reused for native validation feedback. Correcting the input or changing team/language clears stale custom validation. Full names may be shared by different employees; login usernames must be unique.
- Delete draft schedule - manager-only action for removing the selected draft and its shifts and assignments.
- Create shift - manager-only form for adding shifts to a managed draft schedule.
- Manage existing shifts - the build step lists the selected draft's shifts with edit and delete actions. Editing covers dates/times, description, capacity, rest, and required role. Cancel discards the local form without a write.
- Shift edits retain the version originally opened; stale changes are rejected. Refresh and reopen the form to review current data before retrying. Deletion confirms the current shift times and assignment count and sends only that preview revision. Existing request history blocks deletion.
- Successful shift changes refresh the shift/assignment data and publication readiness. Switching drafts clears the editor and its feedback; superseded responses cannot update that editor.
- Assign employee - manager-only draft assignment board and form for assigning an active team employee to a draft shift.
- Remove assignment - manager-only action for removing an employee from a draft shift and refreshing the open slot count.
- Assignment removal loads a fresh preview and confirms the current employee and shift times. Cancellation sends no DELETE; changed data or request history prevents removal and is shown without retrying automatically.
- Automatic assignment - manager-only action for filling open draft shifts and reviewing the assignment report.
- Shift templates - manager-only workflow for creating templates, adding slots, and generating draft shifts.
- Delete unused template - manager-only action for removing a template that is not referenced by existing shifts.
- Draft/template deletion first loads an authorized preview, then shows current identity and child counts in a confirmation dialog. A changed revision returns a localized error without automatically retrying deletion. Drafts with request history cannot be deleted.
- Notifications - authenticated top-bar notification center that lists personal notifications, shows unread count, marks notifications as read, and links schedule-publication notifications to the published schedule.
- Transfer and swap requests - authenticated screen section for outgoing, incoming, and pending manager approval requests.
- Transfer and swap request creation - employee-only form based on the selected published schedule.
- Transfer and swap request creation uses an explicit request-type control for choosing transfer or swap.
- Transfer and swap request actions - employee approve/reject, requester cancel, and manager approve.
- Manager schedule workflow - manager-only focused step navigation for draft, build, assign, and publish work.
- Manager workflow keeps one selected draft schedule as the shared context across its steps, so the same draft is used for shift creation, assignment, automatic assignment, template generation, and publication.
- The build step separates template management, template-based shift generation, and single-shift creation.

## Run Locally

After updating from a version without deletion previews, restart the backend
as well as updating the frontend. Draft, template, shift, and assignment removal
require a preview revision. Shift updates require the saved `version` returned
by the current backend.
Restart the backend for the new employee-creation endpoint as well. No database
reset or additional migration is required. Managers supply the initial password
and share it privately; there is no automated credential delivery.
Run `pnpm test` (or `npm test`) for focused confirmation, calendar, filtering,
shift-edit conversion, and employee-creation API-contract tests; these
use Node's built-in runner. The API contract test uses Vite's module loader with
network listeners disabled and a mocked fetch; these do not replace browser testing.

Requirements:

- Node.js 20 or newer.
- pnpm.

Install dependencies:

```bash
pnpm install
```

Run the frontend:

```bash
pnpm dev
```

The frontend expects the backend at:

```text
http://localhost:8080
```

To use a different backend URL:

```bash
VITE_API_BASE_URL=http://localhost:8082 pnpm dev
```

## Frontend Structure

- `App.jsx` composes the main workspace sections and connects page-level hooks.
- `src/hooks/` contains stateful workflow logic, such as published schedule loading,
  manager scheduling, notifications, templates, availability, and transfer/swap requests.
- `src/components/` contains presentational screen sections and manager workflow panels.
- The manager workflow displays the selected draft schedule once and passes that context to the relevant panels.
