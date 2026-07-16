import { describe, it, expect } from "vitest";
import { toPickerTask } from "./map";
import type { TaskNotesTask } from "../gateway/runtime-api";

describe("toPickerTask", () => {
  it("carries the fields the picker needs, including stored EF overrides", () => {
    const t: TaskNotesTask = {
      path: "Tasks/x.md",
      title: "Reisepass",
      status: "open",
      due: "2026-07-20",
      scheduled: null,
      blockedBy: [{ uid: "Tasks/y.md", reltype: "FINISHTOSTART" }],
      timeEstimate: 30,
      tags: ["task"],
      contexts: ["desk"],
      recurrence: null,
      ef_effort: 2,
      ef_primary: "organization",
    };
    expect(toPickerTask(t)).toEqual({
      path: "Tasks/x.md",
      title: "Reisepass",
      status: "open",
      archived: false,
      due: "2026-07-20",
      scheduled: null,
      blockedBy: [{ uid: "Tasks/y.md", reltype: "FINISHTOSTART" }],
      timeEstimate: 30,
      tags: ["task"],
      contexts: ["desk"],
      recurrence: null,
      ef_effort: 2,
      ef_primary: "organization",
    });
  });

  it("detects archived via the archive tag", () => {
    expect(toPickerTask({ path: "a", title: "x", tags: ["task", "archived"] }).archived).toBe(true);
  });

  it("coerces missing/malformed fields safely", () => {
    const t = { path: "b", tags: "nope", timeEstimate: "20" } as unknown as TaskNotesTask;
    const pt = toPickerTask(t);
    expect(pt.title).toBe("");
    expect(pt.tags).toEqual([]);
    expect(pt.timeEstimate).toBeNull();
    expect(pt.blockedBy).toBeNull();
  });
});
