import { describe, it, expect } from "vitest";
import {
  classify,
  scoreCategories,
  pickPrimarySecondary,
  computeSocial,
  computeLoad,
  computeEffort,
  computeConfidence,
} from "./classify";
import { deriveSignals } from "./signals";
import type { Category, TaskDTO } from "./types";

const NOW = new Date("2026-07-14T12:00:00Z");

function task(partial: Partial<TaskDTO>): TaskDTO {
  return { title: "", tags: [], contexts: [], ...partial };
}

describe("classify — primary category per barrier type", () => {
  const cases: Array<[string, Partial<TaskDTO>, Category]> = [
    ["initiation (set up)", { title: "Set up the new project" }, "initiation"],
    ["planning (roadmap)", { title: "Plan the Q3 roadmap", timeEstimate: 90 }, "planning"],
    ["organization (organize)", { title: "Organize the receipts folder", timeEstimate: 45 }, "organization"],
    ["focus (write report)", { title: "Write the quarterly report", timeEstimate: 90 }, "focus"],
    ["decision (decide/choose)", { title: "Decide which vendor to choose", timeEstimate: 45 }, "decision"],
    ["emotional (taxes)", { title: "Do my taxes", timeEstimate: 60 }, "emotional"],
    ["routine (recurring chore)", { title: "Water the plants", recurrence: "FREQ=WEEKLY" }, "routine"],
  ];
  it.each(cases)("%s", (_name, partial, expected) => {
    expect(classify(task(partial), NOW).ef_primary).toBe(expected);
  });
});

describe("classify — fallback to initiation when no signal", () => {
  it("empty task -> initiation, empty secondary, low confidence", () => {
    const result = classify(task({ title: "" }), NOW);
    expect(result.ef_primary).toBe("initiation");
    expect(result.ef_secondary).toEqual([]);
    expect(result.ef_confidence).toBeLessThanOrEqual(0.5);
  });
});

describe("scoreCategories — structural weights come from signals, not text", () => {
  it("recurrence boosts routine even without a routine keyword", () => {
    const dto = task({ title: "Ship the release", recurrence: "FREQ=MONTHLY" });
    const scores = scoreCategories(dto, deriveSignals(dto, NOW));
    expect(scores.routine).toBeGreaterThanOrEqual(3);
  });

  it("a long estimate boosts focus", () => {
    const dto = task({ title: "Migrate the database", timeEstimate: 300 });
    const scores = scoreCategories(dto, deriveSignals(dto, NOW));
    expect(scores.focus).toBeGreaterThanOrEqual(2);
  });

  it("each cue-group counts at most once", () => {
    // Three planning synonyms should still only add the group weight (2) once.
    const dto = task({ title: "Plan and outline the roadmap strategy" });
    const scores = scoreCategories(dto, deriveSignals(dto, NOW));
    expect(scores.planning).toBe(2);
  });
});

describe("pickPrimarySecondary", () => {
  it("ties break by category order", () => {
    const scores = { initiation: 2, planning: 2, organization: 0, focus: 0, decision: 0, emotional: 0, routine: 0 };
    const { primary, secondary } = pickPrimarySecondary(scores);
    expect(primary).toBe("initiation"); // earlier in EF_CATEGORIES
    expect(secondary).toContain("planning"); // 2 >= 50% of 2
  });

  it("all-zero -> initiation with empty secondary", () => {
    const scores = { initiation: 0, planning: 0, organization: 0, focus: 0, decision: 0, emotional: 0, routine: 0 };
    expect(pickPrimarySecondary(scores)).toEqual({ primary: "initiation", secondary: [] });
  });

  it("secondary excludes categories below half the top score", () => {
    const scores = { initiation: 0, planning: 6, organization: 2, focus: 1, decision: 0, emotional: 0, routine: 0 };
    const { primary, secondary } = pickPrimarySecondary(scores);
    expect(primary).toBe("planning");
    expect(secondary).not.toContain("focus"); // 1 < 3
    expect(secondary).not.toContain("organization"); // 2 < 3
  });
});

describe("computeSocial — live outranks async outranks solo", () => {
  it.each([
    ["Call the dentist", [], "live"],
    ["Email the team update", [], "async"],
    ["Write the report", [], "solo"],
  ] as Array<[string, string[], string]>)("%s -> %s", (title, contexts, expected) => {
    expect(computeSocial(task({ title, contexts }))).toBe(expected);
  });

  it("uses contexts too", () => {
    expect(computeSocial(task({ title: "Sprint review", contexts: ["meeting"] }))).toBe("live");
    expect(computeSocial(task({ title: "Sprint review", contexts: ["slack"] }))).toBe("async");
  });
});

describe("computeLoad (1..3)", () => {
  it("long duration -> 3", () => {
    expect(computeLoad({ duration: "long", timePressure: "none", routine: false }, {} as any, 0)).toBe(3);
  });
  it("medium + planning present -> 3", () => {
    const scores = { initiation: 0, planning: 2, organization: 0, focus: 0, decision: 0, emotional: 0, routine: 0 };
    expect(computeLoad({ duration: "medium", timePressure: "none", routine: false }, scores, 0)).toBe(3);
  });
  it("quick, no barriers -> 1", () => {
    const scores = { initiation: 0, planning: 0, organization: 0, focus: 0, decision: 0, emotional: 0, routine: 0 };
    expect(computeLoad({ duration: "quick", timePressure: "none", routine: false }, scores, 0)).toBe(1);
  });
});

describe("computeEffort (1..4)", () => {
  it("routine is easiest, floored at 1 even when quick", () => {
    expect(computeEffort("routine", { duration: "quick", timePressure: "none", routine: true })).toBe(1);
  });
  it("emotional is hardest", () => {
    expect(computeEffort("emotional", { duration: "none", timePressure: "none", routine: false })).toBe(4);
  });
  it("initiation + long duration -> 4", () => {
    expect(computeEffort("initiation", { duration: "long", timePressure: "none", routine: false })).toBe(4);
  });
  it("overdue adds dread", () => {
    const base = computeEffort("planning", { duration: "medium", timePressure: "none", routine: false });
    const overdue = computeEffort("planning", { duration: "medium", timePressure: "overdue", routine: false });
    expect(overdue).toBeGreaterThan(base);
  });
});

describe("computeConfidence (0..1)", () => {
  const z = { initiation: 0, planning: 0, organization: 0, focus: 0, decision: 0, emotional: 0, routine: 0 };

  it("no evidence -> 0.2 fallback", () => {
    expect(computeConfidence({ ...z })).toBe(0.2);
  });
  it("stays within bounds", () => {
    const strong = computeConfidence({ ...z, planning: 8 });
    expect(strong).toBeGreaterThan(0.2);
    expect(strong).toBeLessThanOrEqual(1);
  });
  it("a clean, uncontested win beats a contested one", () => {
    const clean = computeConfidence({ ...z, planning: 4 });
    const contested = computeConfidence({ ...z, planning: 4, decision: 4 });
    expect(clean).toBeGreaterThan(contested);
  });
  it("more evidence raises confidence at equal margin", () => {
    const weak = computeConfidence({ ...z, planning: 2 }); // uncontested, little evidence
    const strong = computeConfidence({ ...z, planning: 6 }); // uncontested, more evidence
    expect(strong).toBeGreaterThan(weak);
  });
});

describe("classify — full profile shape on a rich task", () => {
  it("aversive phone call that is overdue", () => {
    const result = classify(
      task({
        title: "Call the dentist about the bill",
        contexts: ["phone"],
        timeEstimate: 20,
        due: "2026-07-01", // overdue relative to NOW
      }),
      NOW,
    );
    expect(result.ef_primary).toBe("emotional");
    expect(result.ef_social).toBe("live");
    expect(result.ef_effort).toBe(4); // emotional caps at 4
    expect(result.ef_load).toBe(1); // quick, no planning/decision
    expect(result.ef_confidence).toBeGreaterThan(0.2);
    expect(Array.isArray(result.ef_secondary)).toBe(true);
  });
});
