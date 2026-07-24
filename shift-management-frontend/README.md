# Shift Management Frontend

Minimal React frontend for the shift management system.

For end-to-end local run instructions, see:

```text
../docs/RUN_LOCALLY.md
```

## Current Screens

- Login screen - public screen for signing in with a backend user.
- Published schedules - authenticated screen that lists published schedules visible to the signed-in user.
- Manager actions - placeholder area for the next manager workflows.

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
