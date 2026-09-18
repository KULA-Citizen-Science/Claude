import { describe, it, expect } from "vitest";
import { toTaskDTO, isClassified } from "./task-dto";
import type { TaskNotesTask } from "./runtime-api";

describe("toTaskDTO", () => {
  it("extracts the classifier inputs from a task", () => {
    const task: TaskNotesTask = {
      path: "Tasks/x.md",
      title: "Write the report",
      tags: ["work", "q3"],
      contexts: ["desk"],
      timeEstimate: 90,
      due: "2026-07-20",
      recurrence: "FREQ=WEEKLY",
    };
    expect(toTaskDTO(task)).toEqual({
      title: "Write the report",
      tags: ["work", "q3"],
      contexts: ["desk"],
      timeEstimate: 90,
      due: "2026-07-20",
      recurrence: "FREQ=WEEKLY",
    });
  });

  it("coerces missing / malformed fields to safe defaults", () => {
    const task = { path: "Tasks/y.md", tags: "oops", timeEstimate: "45", due: "" } as unknown as TaskNotesTask;
    expect(toTaskDTO(task)).toEqual({
      title: "",
      tags: [],
      contexts: [],
      timeEstimate: null,
      due: null,
      recurrence: null,
    });
  });
});

describe("isClassified", () => {
  it("true only when a provenance stamp is present", () => {
    expect(isClassified({ path: "a", ef_classified_at: "2026-07-14T00:00:00Z" })).toBe(true);
    expect(isClassified({ path: "a", ef_classified_at: "" })).toBe(false);
    expect(isClassified({ path: "a" })).toBe(false);
  });
});
