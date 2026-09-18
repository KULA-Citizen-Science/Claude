import { describe, it, expect } from "vitest";
import { rankCandidates, firstStep, type PickerTask } from "./select";

const NOW = new Date("2026-07-15T12:00:00Z");

function paths(tasks: PickerTask[], opts = {}): string[] {
  return rankCandidates(tasks, { now: NOW, ...opts }).map((c) => c.path);
}

describe("rankCandidates — ease-first ordering", () => {
  it("surfaces the easiest task first (routine < initiation < emotional)", () => {
    const tasks: PickerTask[] = [
      { path: "emo", title: "Steuererklaerung machen" }, // emotional -> effort 4
      { path: "init", title: "Reisepass" }, // no cue -> initiation -> effort 3
      { path: "routine", title: "Water the plants", recurrence: "FREQ=WEEKLY" }, // effort 1
    ];
    expect(paths(tasks)).toEqual(["routine", "init", "emo"]);
  });

  it("breaks effort ties by urgency (due-today before undated)", () => {
    const tasks: PickerTask[] = [
      { path: "undated", title: "a", ef_effort: 2 },
      { path: "today", title: "a", ef_effort: 2, due: "2026-07-15" },
    ];
    expect(paths(tasks)).toEqual(["today", "undated"]);
  });

  it("honors a manually stored ef_effort over the computed one", () => {
    // "Steuer..." would compute emotional/effort 4, but a stored effort 1 wins.
    const tasks: PickerTask[] = [
      { path: "override", title: "Steuererklaerung machen", ef_effort: 1 },
      { path: "plain", title: "Reisepass" }, // effort 3
    ];
    expect(paths(tasks)).toEqual(["override", "plain"]);
  });
});

describe("rankCandidates — eligibility", () => {
  const base: PickerTask[] = [
    { path: "ok", title: "Reisepass" },
    { path: "done", title: "x", status: "done" },
    { path: "archived", title: "x", archived: true },
    { path: "blocked", title: "x", blockedBy: ["Tasks/other.md"] },
    { path: "deferred", title: "x", scheduled: "2026-08-01" },
  ];

  it("excludes completed, archived, blocked, and future-scheduled tasks", () => {
    expect(paths(base)).toEqual(["ok"]);
  });

  it("keeps an overdue task but drops one scheduled for the future", () => {
    const tasks: PickerTask[] = [
      { path: "overdue", title: "a", ef_effort: 2, due: "2026-07-01" },
      { path: "future", title: "a", ef_effort: 2, scheduled: "2026-09-01" },
    ];
    expect(paths(tasks)).toEqual(["overdue"]);
  });

  it("respects the exclude set (already shown this session)", () => {
    const tasks: PickerTask[] = [
      { path: "a", title: "Reisepass" },
      { path: "b", title: "Reisepass" },
    ];
    expect(paths(tasks, { exclude: new Set(["a"]) })).toEqual(["b"]);
  });

  it("treats custom completed statuses as done", () => {
    const tasks: PickerTask[] = [{ path: "c", title: "x", status: "cancelled" }];
    expect(paths(tasks, { completedStatuses: new Set(["done", "cancelled"]) })).toEqual([]);
  });
});

describe("rankCandidates — time cap", () => {
  const tasks: PickerTask[] = [
    { path: "quick", title: "quick thing", timeEstimate: 10 },
    { path: "med", title: "med thing", timeEstimate: 45 },
    { path: "easyNoEst", title: "Water the plants", recurrence: "FREQ=WEEKLY" }, // effort 1, no est
    { path: "hardNoEst", title: "Reisepass" }, // effort 3, no est
  ];

  it("5m keeps <=15min estimates and effort-1 unestimated tasks", () => {
    expect(paths(tasks, { timeCap: "5m" }).sort()).toEqual(["easyNoEst", "quick"]);
  });

  it("25m keeps <=30min estimates and effort<=2 unestimated tasks", () => {
    const kept = paths(tasks, { timeCap: "25m" }).sort();
    expect(kept).toContain("quick");
    expect(kept).toContain("easyNoEst");
    expect(kept).not.toContain("med"); // 45 > 30
    expect(kept).not.toContain("hardNoEst"); // effort 3 > 2
  });

  it("more keeps everything eligible", () => {
    expect(paths(tasks, { timeCap: "more" }).length).toBe(4);
  });
});

describe("firstStep", () => {
  it("uses the first incomplete subtask when present", () => {
    expect(firstStep("planning", "Draft the outline")).toBe("Start with: “Draft the outline”");
  });
  it("falls back to a barrier-specific nudge", () => {
    expect(firstStep("emotional")).toMatch(/5-minute timer/);
    expect(firstStep("routine", "   ")).toMatch(/just do it/);
  });
});
