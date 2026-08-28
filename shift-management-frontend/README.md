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
- Create schedule - manager-only form for creating a draft schedule for a managed team.
- Create shift - manager-only form for adding shifts to a managed draft schedule.
- Assign employee - manager-only form for manually assigning an active team employee to a draft shift.
- Automatic assignment - manager-only action for filling open draft shifts and reviewing the assignment report.
- Shift templates - manager-only workflow for creating templates, adding slots, and generating draft shifts.
- Notifications - authenticated screen section that lists personal notifications, shows unread count, and marks notifications as read.
- Transfer and swap requests - authenticated screen section for outgoing, incoming, and pending manager approval requests.
- Transfer and swap request actions - employee approve/reject, requester cancel, and manager approve.

## Run Locally

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
