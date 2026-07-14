import { describe, it, expect } from "vitest";
import { bucketDuration, bucketTimePressure, isRoutine, deriveSignals } from "./signals";

describe("bucketDuration", () => {
  const cases: Array<[number | null | undefined, string]> = [
    [undefined, "none"],
    [null, "none"],
    [0, "none"],
    [-10, "none"],
    [NaN, "none"],
    [15, "quick"],
    [29, "quick"],
    [30, "medium"],
    [120, "medium"],
    [121, "long"],
    [600, "long"],
  ];
  it.each(cases)("%s minutes -> %s", (input, expected) => {
    expect(bucketDuration(input)).toBe(expected);
  });
});

describe("bucketTimePressure", () => {
  const now = new Date("2026-07-14T12:00:00Z"); // a Tuesday
  const cases: Array<[string | null | undefined, string]> = [
    [undefined, "none"],
    [null, "none"],
    ["not-a-date", "none"],
    ["2026-07-10", "overdue"],
    ["2026-07-13", "overdue"],
    ["2026-07-14", "soon"], // today
    ["2026-07-15", "soon"], // tomorrow
    ["2026-07-16", "soon"], // within the 2-day window
    ["2026-07-17", "none"], // just past the window
    ["2026-08-01", "none"],
  ];
  it.each(cases)("due %s -> %s", (due, expected) => {
    expect(bucketTimePressure(due, now)).toBe(expected);
  });

  it("compares by calendar day, ignoring time-of-day", () => {
    const lateNow = new Date("2026-07-14T23:59:00Z");
    expect(bucketTimePressure("2026-07-14T00:01:00", lateNow)).toBe("soon");
  });
});

describe("isRoutine", () => {
  it.each([
    [undefined, false],
    [null, false],
    ["", false],
    ["   ", false],
    ["FREQ=WEEKLY", true],
    ["every monday", true],
  ] as Array<[string | null | undefined, boolean]>)("%s -> %s", (input, expected) => {
    expect(isRoutine(input)).toBe(expected);
  });
});

describe("deriveSignals", () => {
  it("reads only the structured fields", () => {
    const now = new Date("2026-07-14T12:00:00Z");
    const signals = deriveSignals(
      {
        // Title text mentions "tomorrow" and "2 hours" — these MUST be ignored;
        // only timeEstimate/due/recurrence drive the signals.
        title: "Finish the essay tomorrow, about 2 hours",
        tags: [],
        contexts: [],
        timeEstimate: 45,
        due: "2026-07-20",
        recurrence: null,
      },
      now,
    );
    expect(signals).toEqual({ duration: "medium", timePressure: "none", routine: false });
  });
});
