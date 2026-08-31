import assert from "node:assert/strict";
import test from "node:test";
import { confirmDeletion, deletionErrorMessage } from "./confirmDeletion.js";
import { translations } from "./i18n/translations.js";

test("loads the current preview before asking, then deletes exactly the confirmed revision", async () => {
  const calls = [];
  const result = await confirmDeletion({
    loadPreview: async () => { calls.push("preview"); return { revision: "r1", shiftCount: 3 }; },
    describe: (preview) => `Delete ${preview.shiftCount} shifts?`,
    confirm: (message) => { calls.push(message); return true; },
    remove: async (revision) => calls.push(revision),
  });
  assert.equal(result, true);
  assert.deepEqual(calls, ["preview", "Delete 3 shifts?", "r1"]);
});

test("cancel does not send a delete", async () => {
  assert.equal(await confirmDeletion({
    loadPreview: async () => ({ revision: "r1" }), describe: () => "Delete?",
    confirm: () => false, remove: () => assert.fail("must not delete"),
  }), false);
});

test("a stale conflict is propagated without fetching another revision or retrying", async () => {
  let previews = 0;
  let deletes = 0;
  const conflict = new Error("stale");
  await assert.rejects(confirmDeletion({
    loadPreview: async () => { previews++; return { revision: "old" }; },
    describe: () => "Delete?", confirm: () => true,
    remove: async (revision) => { deletes++; assert.equal(revision, "old"); throw conflict; },
  }), (error) => error === conflict);
  assert.equal(previews, 1);
  assert.equal(deletes, 1);
});

test("failed preview never asks for confirmation or deletes", async () => {
  await assert.rejects(confirmDeletion({
    loadPreview: async () => { throw new Error("forbidden"); },
    describe: () => assert.fail(), confirm: () => assert.fail(), remove: () => assert.fail(),
  }), /forbidden/);
});

test("deletion errors have Hebrew and English messages", () => {
  for (const language of ["he", "en"]) {
    const t = (key) => translations[language][key];
    assert.equal(deletionErrorMessage(new Error("Deletion preview is out of date. Review the current data before deleting."), t), t("staleDeletionPreview"));
    assert.equal(deletionErrorMessage(new Error("Schedules with transfer or swap request history cannot be deleted"), t), t("cannotDeleteRequestHistory"));
    assert.ok(t("cannotDeleteAssignmentHistory"));
    for (const type of ["Shifts", "Assignments"]) {
      assert.equal(deletionErrorMessage(new Error(`${type} with transfer or swap request history cannot be deleted`), t), t("cannotDeleteAssignmentHistory"));
    }
  }
});
