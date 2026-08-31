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
- Manager published schedule view - manager-only read-only view of a selected published schedule and its assignments.
- Create schedule - manager-only form for creating a draft schedule for a managed team.
- Delete draft schedule - manager-only action for removing the selected draft and its shifts and assignments.
- Create shift - manager-only form for adding shifts to a managed draft schedule.
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
as well as updating the frontend. Draft, template, and assignment removal require
a preview revision; the shift DELETE API requires one too (shift deletion UI is
still planned separately).
Run `pnpm test` (or `npm test`) for the focused confirmation-flow tests; these
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
