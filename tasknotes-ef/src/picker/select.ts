// "Start something" selection core — pure, no Obsidian / TaskNotes imports.
// Given the current tasks, decide which one to surface to start *now*, biased to
// "ease me in": lowest activation energy first, urgency only as a tiebreak.
//
// Ease leans on effort (computed by the classifier, or a manually-set ef_effort
// override) rather than time estimates, since estimates are usually absent.

import { classify, bucketDuration, bucketTimePressure, EF_CATEGORIES } from "../classifier";
import type { Category, TaskDTO } from "../classifier";

export type TimeCap = "5m" | "25m" | "more";

/** The task fields the picker needs. Mapped from a TaskNotes task by the caller
 *  so this module stays free of any TaskNotes type. */
export interface PickerTask {
  path: string;
  title: string;
  status?: string;
  archived?: boolean;
  due?: string | null;
  scheduled?: string | null;
  blockedBy?: unknown[] | null;
  timeEstimate?: number | null;
  tags?: string[];
  contexts?: string[];
  recurrence?: string | null;
  // Optional stored EF values — a manual override wins over the computed one.
  ef_effort?: unknown;
  ef_primary?: unknown;
}

export interface Candidate {
  path: string;
  title: string;
  effort: 1 | 2 | 3 | 4;
  primary: Category;
  durationLabel: "quick" | "medium" | "long" | "unsized";
  urgency: "overdue" | "soon" | "none";
}

const DURATION_RANK: Record<string, number> = { quick: 0, medium: 1, none: 2, long: 3 };
const URGENCY_RANK: Record<string, number> = { overdue: 0, soon: 1, none: 2 };
const DURATION_LABEL: Record<string, Candidate["durationLabel"]> = {
  quick: "quick",
  medium: "medium",
  long: "long",
  none: "unsized",
};

function toDTO(pt: PickerTask): TaskDTO {
  return {
    title: pt.title ?? "",
    tags: pt.tags ?? [],
    contexts: pt.contexts ?? [],
    timeEstimate: pt.timeEstimate ?? null,
    due: pt.due ?? null,
    recurrence: pt.recurrence ?? null,
  };
}

function isEffort(v: unknown): v is 1 | 2 | 3 | 4 {
  return v === 1 || v === 2 || v === 3 || v === 4;
}

function isCategory(v: unknown): v is Category {
  return typeof v === "string" && (EF_CATEGORIES as readonly string[]).includes(v);
}

function startOfDay(d: Date): number {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}

/** Scheduled strictly after today = deliberately deferred; not a "now" pick. */
function isDeferred(scheduled: string | null | undefined, now: Date): boolean {
  if (!scheduled) return false;
  const t = Date.parse(scheduled);
  if (Number.isNaN(t)) return false;
  return startOfDay(new Date(t)) > startOfDay(now);
}

export interface SelectOptions {
  now?: Date;
  timeCap?: TimeCap;
  completedStatuses?: Set<string>;
  /** Paths to skip this session (already shown / dismissed). */
  exclude?: Set<string>;
}

/** Resolve effort + primary, preferring a manually-stored value over computed. */
function resolveEf(pt: PickerTask): { effort: 1 | 2 | 3 | 4; primary: Category } {
  const c = classify(toDTO(pt));
  return {
    effort: isEffort(pt.ef_effort) ? pt.ef_effort : c.ef_effort,
    primary: isCategory(pt.ef_primary) ? pt.ef_primary : c.ef_primary,
  };
}

function passesTimeCap(cap: TimeCap, timeEstimate: number | null | undefined, effort: number): boolean {
  if (cap === "more") return true;
  const est = typeof timeEstimate === "number" && timeEstimate > 0 ? timeEstimate : null;
  if (cap === "5m") return est !== null ? est <= 15 : effort === 1;
  // "25m" — fits in a focus block
  return est !== null ? est <= 30 : effort <= 2;
}

/**
 * Rank eligible tasks, easiest-to-start first. Eligible = not completed, not
 * archived, not blocked, not deferred to a future scheduled date, not excluded,
 * and within the optional time cap. Order: effort ↑, duration ↑, urgency ↑ (more
 * urgent wins the tie), then title for stability.
 */
export function rankCandidates(tasks: PickerTask[], options: SelectOptions = {}): Candidate[] {
  const now = options.now ?? new Date();
  const cap = options.timeCap ?? "more";
  const completed = options.completedStatuses ?? new Set(["done"]);
  const exclude = options.exclude ?? new Set<string>();

  const scored: Array<{ c: Candidate; keys: [number, number, number, string] }> = [];

  for (const pt of tasks) {
    if (exclude.has(pt.path)) continue;
    if (pt.archived) continue;
    if (pt.status && completed.has(pt.status)) continue;
    if (Array.isArray(pt.blockedBy) && pt.blockedBy.length > 0) continue;
    if (isDeferred(pt.scheduled, now)) continue;

    const { effort, primary } = resolveEf(pt);
    if (!passesTimeCap(cap, pt.timeEstimate, effort)) continue;

    const duration = bucketDuration(pt.timeEstimate);
    const urgency = bucketTimePressure(pt.due, now);

    scored.push({
      c: {
        path: pt.path,
        title: pt.title ?? "",
        effort,
        primary,
        durationLabel: DURATION_LABEL[duration],
        urgency,
      },
      keys: [effort, DURATION_RANK[duration], URGENCY_RANK[urgency], (pt.title ?? "").toLowerCase()],
    });
  }

  scored.sort((a, b) => {
    for (let i = 0; i < 3; i++) {
      const d = (a.keys[i] as number) - (b.keys[i] as number);
      if (d !== 0) return d;
    }
    return (a.keys[3] as string).localeCompare(b.keys[3] as string);
  });

  return scored.map((s) => s.c);
}

// The gentle first move for each barrier type — the whole point is that YOU
// don't have to decide what starting looks like.
const FIRST_STEP: Record<Category, string> = {
  initiation: "Do the smallest physical first action — open the thing. Nothing more.",
  planning: "Write just 3 bullet sub-steps. Don't do them yet — only list them.",
  organization: "Open the one file, link, or email you'll need first.",
  focus: "Open the doc and read only the first line. Put your cursor in.",
  decision: "Name the two options out loud. You don't have to decide yet.",
  emotional: "Set a 5-minute timer. You have permission to stop when it rings.",
  routine: "It's a quick one — just do it now.",
};

/** The suggested first step: the task's first incomplete subtask if there is
 *  one, otherwise a barrier-appropriate nudge. */
export function firstStep(primary: Category, subtaskTitle?: string | null): string {
  if (subtaskTitle && subtaskTitle.trim()) return `Start with: “${subtaskTitle.trim()}”`;
  return FIRST_STEP[primary];
}
