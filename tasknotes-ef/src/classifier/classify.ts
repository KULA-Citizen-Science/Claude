// Pure scoring + orchestration. No I/O, no imports from `obsidian` or TaskNotes.
// The pipeline is a series of small pure functions so each is independently
// testable: deriveSignals → scoreCategories → pickPrimarySecondary → compute*.

import {
  EF_CATEGORIES,
  type Category,
  type DerivedSignals,
  type EFClassification,
  type SocialDemand,
  type TaskDTO,
} from "./types";
import { deriveSignals } from "./signals";
import {
  SOCIAL_ASYNC_CONTEXTS,
  SOCIAL_ASYNC_TERMS,
  SOCIAL_LIVE_CONTEXTS,
  SOCIAL_LIVE_TERMS,
  TITLE_CUES,
} from "./lexicon";

function zeroScores(): Record<Category, number> {
  return Object.fromEntries(EF_CATEGORIES.map((c) => [c, 0])) as Record<Category, number>;
}

function clamp(n: number, lo: number, hi: number): number {
  return Math.min(hi, Math.max(lo, n));
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}

/** Lowercased "title tags contexts" blob used for keyword matching. */
function textBlob(dto: TaskDTO): string {
  return [dto.title ?? "", ...(dto.tags ?? []), ...(dto.contexts ?? [])]
    .join(" ")
    .toLowerCase();
}

/**
 * Score each category from keyword cues (text) plus structural weights derived
 * ONLY from signals (never text). Each cue-group counts once.
 */
export function scoreCategories(dto: TaskDTO, signals: DerivedSignals): Record<Category, number> {
  const scores = zeroScores();
  const blob = textBlob(dto);

  for (const cue of TITLE_CUES) {
    if (cue.terms.some((term) => blob.includes(term))) {
      scores[cue.category] += cue.weight;
    }
  }

  // Structural weights — from derived signals, not from text.
  if (signals.routine) scores.routine += 3;
  if (signals.duration === "long") scores.focus += 2;
  if (signals.duration === "none") scores.initiation += 1; // no estimate → vaguer start

  return scores;
}

/**
 * Argmax for the primary; other categories scoring at least half the top score
 * become secondary. Ties break by EF_CATEGORIES order. When nothing scores, we
 * fall back to `initiation` — the safest default for a "help me start" layer.
 */
export function pickPrimarySecondary(scores: Record<Category, number>): {
  primary: Category;
  secondary: Category[];
} {
  let primary: Category = EF_CATEGORIES[0];
  let best = -Infinity;
  for (const c of EF_CATEGORIES) {
    if (scores[c] > best) {
      best = scores[c];
      primary = c;
    }
  }

  if (best <= 0) return { primary: "initiation", secondary: [] };

  const secondary = EF_CATEGORIES.filter(
    (c) => c !== primary && scores[c] > 0 && scores[c] >= best * 0.5,
  ).sort((a, b) => scores[b] - scores[a]);

  return { primary, secondary };
}

/** Social demand from contexts and title verbs. Live outranks async. */
export function computeSocial(dto: TaskDTO): SocialDemand {
  const title = (dto.title ?? "").toLowerCase();
  const contexts = (dto.contexts ?? []).map((c) => c.toLowerCase());

  const titleHas = (terms: string[]) => terms.some((t) => title.includes(t));
  const contextHas = (roots: string[]) =>
    contexts.some((c) => roots.some((r) => c.includes(r)));

  if (titleHas(SOCIAL_LIVE_TERMS) || contextHas(SOCIAL_LIVE_CONTEXTS)) return "live";
  if (titleHas(SOCIAL_ASYNC_TERMS) || contextHas(SOCIAL_ASYNC_CONTEXTS)) return "async";
  return "solo";
}

/** Working-memory load 1..3. */
export function computeLoad(
  signals: DerivedSignals,
  scores: Record<Category, number>,
  secondaryCount: number,
): 1 | 2 | 3 {
  let load = 1;
  if (signals.duration === "medium") load = 2;
  if (signals.duration === "long") load = 3;
  if (scores.planning > 0 || scores.decision > 0) load += 1; // holding options in mind
  if (secondaryCount >= 2) load += 1; // several barriers at once
  return clamp(load, 1, 3) as 1 | 2 | 3;
}

/** Activation energy to begin, 1..4. */
export function computeEffort(primary: Category, signals: DerivedSignals): 1 | 2 | 3 | 4 {
  let effort = 2;
  if (primary === "routine") effort = 1;
  if (primary === "initiation" || primary === "planning" || primary === "decision") effort = 3;
  if (primary === "emotional") effort = 4;

  if (signals.duration === "long") effort += 1;
  if (signals.duration === "quick") effort -= 1;
  if (signals.timePressure === "overdue") effort += 1; // dread accrues on overdue tasks

  return clamp(effort, 1, 4) as 1 | 2 | 3 | 4;
}

/**
 * Confidence 0..1 from how cleanly the top category separates from the rest and
 * how much evidence backs it. Base 0.3 when anything scored, 0.2 for a pure
 * fallback guess, up to 1.0 for a strong unambiguous signal.
 */
export function computeConfidence(scores: Record<Category, number>): number {
  const sorted = EF_CATEGORIES.map((c) => scores[c]).sort((a, b) => b - a);
  const top = sorted[0] ?? 0;
  const runnerUp = sorted[1] ?? 0;

  if (top <= 0) return 0.2;

  const margin = (top - runnerUp) / top; // 0 (tie) .. 1 (uncontested)
  const evidence = Math.min(1, top / 5); // saturates at a score of 5
  return round2(clamp(0.3 + 0.5 * margin + 0.2 * evidence, 0, 1));
}

/** Full classification pipeline. `now` is injectable for deterministic tests. */
export function classify(dto: TaskDTO, now: Date = new Date()): EFClassification {
  const signals = deriveSignals(dto, now);
  const scores = scoreCategories(dto, signals);
  const { primary, secondary } = pickPrimarySecondary(scores);

  return {
    ef_primary: primary,
    ef_secondary: secondary,
    ef_load: computeLoad(signals, scores, secondary.length),
    ef_social: computeSocial(dto),
    ef_effort: computeEffort(primary, signals),
    ef_confidence: computeConfidence(scores),
  };
}
