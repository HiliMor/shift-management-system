import assert from "node:assert/strict";
import test from "node:test";
import { createServer } from "vite";
import { confirmDeletion } from "./confirmDeletion.js";

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
