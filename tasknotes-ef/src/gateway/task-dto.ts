// Maps a TaskNotes task object into the plain DTO the pure classifier consumes.
// Kept separate from the classifier so the classifier stays free of any
// TaskNotes type, and separate from the gateway so it needs no obsidian import.

import type { TaskDTO } from "../classifier";
import { EF_CLASSIFIED_AT_KEY } from "../fields";
import type { TaskNotesTask } from "./runtime-api";

function asString(v: unknown): string {
  return typeof v === "string" ? v : "";
}

function asStringArray(v: unknown): string[] {
  return Array.isArray(v) ? v.filter((x): x is string => typeof x === "string") : [];
}

function asNumberOrNull(v: unknown): number | null {
  return typeof v === "number" && Number.isFinite(v) ? v : null;
}

function nonEmptyOrNull(v: unknown): string | null {
  const s = asString(v);
  return s.length > 0 ? s : null;
}

export function toTaskDTO(task: TaskNotesTask): TaskDTO {
  return {
    title: asString(task.title),
    tags: asStringArray(task.tags),
    contexts: asStringArray(task.contexts),
    timeEstimate: asNumberOrNull(task.timeEstimate),
    due: nonEmptyOrNull(task.due),
    recurrence: nonEmptyOrNull(task.recurrence),
  };
}

/** A task is unclassified when it has no provenance stamp. */
export function isClassified(task: TaskNotesTask): boolean {
  return nonEmptyOrNull(task[EF_CLASSIFIED_AT_KEY]) !== null;
}
