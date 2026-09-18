import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { EF_TRIAGE_BASE, EF_TRIAGE_VIEW_PATH } from "./ef-triage-base";

describe("ef-triage.base embedded copy", () => {
  it("matches the shipped views/ef-triage.base byte-for-byte", () => {
    const shipped = readFileSync(
      fileURLToPath(new URL("../../views/ef-triage.base", import.meta.url)),
      "utf8",
    );
    expect(EF_TRIAGE_BASE).toBe(shipped);
  });

  it("is a tasknotesTaskList grouped by ef_primary, filtered to open tasks", () => {
    expect(EF_TRIAGE_BASE).toContain("type: tasknotesTaskList");
    expect(EF_TRIAGE_BASE).toContain("property: note.ef_primary");
    expect(EF_TRIAGE_BASE).toContain('status != "done"');
    expect(EF_TRIAGE_VIEW_PATH).toBe("TaskNotes/Views/ef-triage.base");
  });
});
