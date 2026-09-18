import { describe, it, expect } from "vitest";
import { toPickerTask, resolveTaskPath } from "./map";
import type { TaskNotesTask } from "../gateway/runtime-api";

describe("resolveTaskPath", () => {
  it("reads path, or file.path / filePath fallbacks, or empty", () => {
    expect(resolveTaskPath({ path: "Tasks/a.md" } as TaskNotesTask)).toBe("Tasks/a.md");
    expect(resolveTaskPath({ file: { path: "Tasks/b.md" } } as unknown as TaskNotesTask)).toBe("Tasks/b.md");
    expect(resolveTaskPath({ filePath: "Tasks/c.md" } as unknown as TaskNotesTask)).toBe("Tasks/c.md");
    expect(resolveTaskPath({} as TaskNotesTask)).toBe("");
  });
});

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
