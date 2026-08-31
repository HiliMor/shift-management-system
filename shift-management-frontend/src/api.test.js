import assert from "node:assert/strict";
import test from "node:test";
import { createServer } from "vite";
import { confirmDeletion } from "./confirmDeletion.js";

test("employee creation sends credentials in the authenticated POST body, not the URL", async (context) => {
  const server = await createServer({ configFile: false, cacheDir: "node_modules/.vite-tests",
    optimizeDeps: { noDiscovery: true, include: [] },
    server: { middlewareMode: true, hmr: false, ws: false, watch: null } });
  try {
    const { createTeamEmployee } = await server.ssrLoadModule("/src/api.js");
    const employee = { username: "new.employee", password: "Test-only-123", fullName: "New Employee",
      email: null, staffingRoleIds: [4] };
    context.mock.method(globalThis, "fetch", async (url, options) => {
      assert.equal(new URL(url).pathname, "/api/teams/2/employees");
      assert.equal(new URL(url).search, "");
      assert.equal(options.method, "POST");
      assert.equal(options.headers.Authorization, "Bearer test-token");
      assert.deepEqual(JSON.parse(options.body), employee);
      return new Response(JSON.stringify({ id: 7, username: employee.username }), { status: 201 });
    });
    assert.deepEqual(await createTeamEmployee("test-token", 2, employee), { id: 7, username: "new.employee" });
  } finally {
    await server.close();
  }
});

test("assignment removal fetches a noncached preview and sends only the confirmed revision", async (context) => {
  const server = await createServer({
    configFile: false,
    cacheDir: "node_modules/.vite-tests",
    optimizeDeps: { noDiscovery: true, include: [] },
    server: { middlewareMode: true, hmr: false, ws: false, watch: null },
  });
  try {
    const { getAssignmentDeletionPreview, deleteAssignment, ApiError } = await server.ssrLoadModule("/src/api.js");
    const calls = [];
    const preview = {
      revision: "confirmed-revision",
      assignment: { id: 7, employeeFullName: "Current employee" },
      shift: { id: 3 },
    };
    context.mock.method(globalThis, "fetch", async (url, options) => {
      calls.push({ url: new URL(url), options });
      return options.method === "DELETE"
        ? new Response(JSON.stringify({ message: "Stale preview" }), { status: 409 })
        : new Response(JSON.stringify(preview), { status: 200 });
    });
    await assert.rejects(confirmDeletion({
      loadPreview: () => getAssignmentDeletionPreview("test-token", 7),
      describe: (data) => data.assignment.employeeFullName,
      confirm: (message) => { assert.equal(message, "Current employee"); return true; },
      remove: (revision) => deleteAssignment("test-token", 7, revision),
    }), (error) => error instanceof ApiError && error.status === 409);
    assert.equal(calls.length, 2);
    assert.equal(calls[0].url.pathname, "/api/assignments/7/deletion-preview");
    assert.equal(calls[0].options.cache, "no-store");
    assert.equal(calls[1].url.pathname, "/api/assignments/7");
    assert.equal(calls[1].options.method, "DELETE");
    assert.equal(calls[1].url.searchParams.get("revision"), preview.revision);
    assert.ok(calls.every(({ options }) => options.headers.Authorization === "Bearer test-token"));
  } finally {
    await server.close();
  }
});

test("shift editing sends the viewed version and deletion uses the confirmed preview", async (context) => {
  const server = await createServer({ configFile: false, cacheDir: "node_modules/.vite-tests",
    optimizeDeps: { noDiscovery: true, include: [] },
    server: { middlewareMode: true, hmr: false, ws: false, watch: null } });
  try {
    const api = await server.ssrLoadModule("/src/api.js");
    const calls = [];
    context.mock.method(globalThis, "fetch", async (url, options) => {
      calls.push({ url: new URL(url), options });
      if (options.method === "PUT") {
        return new Response(JSON.stringify({ code: "STALE_VERSION", message: "Record changed" }), { status: 409 });
      }
      if (options.method === "DELETE") return new Response(null, { status: 204 });
      return new Response(JSON.stringify({ shift: { id: 5 }, assignmentCount: 2, revision: "revision/+=" }));
    });
    await assert.rejects(api.updateShift("test-token", 2, 5, { version: 3, description: "Changed" }),
      (error) => error.status === 409 && error.body.code === "STALE_VERSION");
    assert.equal(calls.length, 1, "a stale edit must not retry with a newer version");
    assert.equal(calls[0].url.pathname, "/api/schedules/2/shifts/5");
    assert.deepEqual(JSON.parse(calls[0].options.body), { version: 3, description: "Changed" });
    await confirmDeletion({
      loadPreview: () => api.getShiftDeletionPreview("test-token", 2, 5),
      describe: (data) => `Assignments: ${data.assignmentCount}`,
      confirm: (message) => { assert.equal(message, "Assignments: 2"); return true; },
      remove: (revision) => api.deleteShift("test-token", 2, 5, revision),
    });
    assert.equal(calls[1].url.pathname, "/api/schedules/2/shifts/5/deletion-preview");
    assert.equal(calls[1].options.cache, "no-store");
    assert.equal(calls[2].options.method, "DELETE");
    assert.equal(calls[2].url.searchParams.get("revision"), "revision/+=");
    assert.ok(calls.every(({ options }) => options.headers.Authorization === "Bearer test-token"));
  } finally {
    await server.close();
  }
});
