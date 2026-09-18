// Derived signals. Each function reads exactly one structured TaskNotes field.
// This is where the "never re-parse text for duration/time_pressure/routine"
// guarantee is enforced structurally: the title/tags/contexts are not even in
// scope here.

import type { Duration, TimePressure, DerivedSignals, TaskDTO } from "./types";

const DAY_MS = 86_400_000;

/** Bucket a time estimate (minutes) into a coarse duration. */
export function bucketDuration(timeEstimate?: number | null): Duration {
  if (timeEstimate == null || !Number.isFinite(timeEstimate) || timeEstimate <= 0) {
    return "none";
  }
  if (timeEstimate < 30) return "quick";
  if (timeEstimate <= 120) return "medium";
  return "long";
}

/** Bucket the (already-structured) due date into time pressure, compared by
 *  calendar day so a date-only `due` is not skewed by time-of-day. */
export function bucketTimePressure(due: string | null | undefined, now: Date): TimePressure {
  if (!due) return "none";
  const parsed = Date.parse(due); // `due` is an ISO string from TaskNotes, not NL text
  if (Number.isNaN(parsed)) return "none";

  const startOfDay = (d: Date) =>
    new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();

  const today = startOfDay(now);
  const dueDay = startOfDay(new Date(parsed));

  if (dueDay < today) return "overdue";
  if (dueDay <= today + 2 * DAY_MS) return "soon"; // due today or within two days
  return "none";
}

/** A non-empty recurrence rule means the task is routine. */
export function isRoutine(recurrence?: string | null): boolean {
  return typeof recurrence === "string" && recurrence.trim().length > 0;
}

export function deriveSignals(dto: TaskDTO, now: Date): DerivedSignals {
  return {
    duration: bucketDuration(dto.timeEstimate),
    timePressure: bucketTimePressure(dto.due, now),
    routine: isRoutine(dto.recurrence),
  };
}
