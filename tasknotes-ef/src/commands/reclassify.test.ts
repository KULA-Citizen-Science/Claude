import { describe, it, expect } from "vitest";
import { planReclassify } from "./reclassify";
import { EF_CLASSIFIED_AT_KEY } from "../fields";
import type { TaskNotesTask } from "../gateway/runtime-api";

const AT = "2026-07-14T12:00:00.000Z";

describe("planReclassify", () => {
  it("skips already-classified tasks and plans writes for the rest", () => {
    const tasks: TaskNotesTask[] = [
      { path: "Tasks/done.md", title: "Water the plants", recurrence: "FREQ=WEEKLY", [EF_CLASSIFIED_AT_KEY]: AT },
      { path: "Tasks/new1.md", title: "Do my taxes", timeEstimate: 60 },
      { path: "Tasks/new2.md", title: "Plan the roadmap", timeEstimate: 90 },
    ];

    const plan = planReclassify(tasks, AT);

    expect(plan.scanned).toBe(3);
    expect(plan.alreadyClassified).toEqual(["Tasks/done.md"]);
    expect(plan.writes.map((w) => w.path)).toEqual(["Tasks/new1.md", "Tasks/new2.md"]);
    expect(plan.writes[0].classification.ef_primary).toBe("emotional");
    expect(plan.writes[1].classification.ef_primary).toBe("planning");
    // each planned patch carries the provenance stamp
    expect(plan.writes[0].patch[EF_CLASSIFIED_AT_KEY]).toBe(AT);
  });

  it("returns an empty plan for an empty vault", () => {
    expect(planReclassify([], AT)).toEqual({ scanned: 0, alreadyClassified: [], writes: [] });
  });
});
