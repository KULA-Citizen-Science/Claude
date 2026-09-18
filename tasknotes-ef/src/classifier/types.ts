// Pure classifier types. No imports from `obsidian` or TaskNotes — this module
// (and everything it imports) must stay runtime-free so it can be unit-tested in
// isolation and can never reach into task storage.

/** The seven executive-function barrier categories. Order is significant: it is
 *  the deterministic tie-break order when two categories score equally. */
export const EF_CATEGORIES = [
  "initiation", // vague / aversive start; the "just begin" barrier
  "planning", // needs decomposition or sequencing before it can start
  "organization", // needs gathering materials / context / info first
  "focus", // long, sustained-attention demand
  "decision", // ambiguous; requires choosing among options
  "emotional", // anxiety / aversion / conflict-laden
  "routine", // habitual, low-friction, recurring
] as const;

export type Category = (typeof EF_CATEGORIES)[number];

export type SocialDemand = "solo" | "async" | "live";
export type Duration = "none" | "quick" | "medium" | "long";
export type TimePressure = "none" | "soon" | "overdue";

/**
 * Plain input DTO. The three structured fields (`timeEstimate`, `due`,
 * `recurrence`) come straight from TaskNotes and are the ONLY source for the
 * derived signals below — the classifier never re-parses free text to recover
 * duration / time-pressure / routine.
 */
export interface TaskDTO {
  title: string;
  tags: string[];
  contexts: string[];
  timeEstimate?: number | null; // minutes
  due?: string | null; // ISO date or date-time
  recurrence?: string | null; // RRULE or recurrence string
}

/** Signals derived ONLY from structured TaskNotes fields, never from text. */
export interface DerivedSignals {
  duration: Duration;
  timePressure: TimePressure;
  routine: boolean;
}

/** The classifier output, keyed to match the seven `ef_*` TaskNotes fields
 *  (minus the `ef_classified_at` provenance stamp, which the writer adds). */
export interface EFClassification {
  ef_primary: Category;
  ef_secondary: Category[];
  ef_load: 1 | 2 | 3;
  ef_social: SocialDemand;
  ef_effort: 1 | 2 | 3 | 4;
  ef_confidence: number; // 0..1
}
