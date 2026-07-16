// Maps a TaskNotes task into the PickerTask the selection core consumes. Pure
// (type-only TaskNotes import), so it stays unit-testable without Obsidian.

import type { TaskNotesTask } from "../gateway/runtime-api";
import type { PickerTask } from "./select";

const ARCHIVE_TAG = "archived"; // per fieldMapping.archiveTag

function strArray(v: unknown): string[] {
  return Array.isArray(v) ? v.filter((x): x is string => typeof x === "string") : [];
}

/** The task's vault-relative path, tolerant of where the runtime puts it. */
export function resolveTaskPath(t: TaskNotesTask): string {
  const rec = t as Record<string, unknown>;
  const file = rec.file as Record<string, unknown> | undefined;
  const candidates = [rec.path, file?.path, rec.filePath, rec.filepath];
  for (const c of candidates) {
    if (typeof c === "string" && c.trim()) return c.trim();
  }
  return "";
}

export function toPickerTask(t: TaskNotesTask): PickerTask {
  const tags = strArray(t.tags);
  return {
    path: t.path,
    title: typeof t.title === "string" ? t.title : "",
    status: typeof t.status === "string" ? t.status : undefined,
    // TaskNotes marks archived tasks with the archive tag (and may also expose a
    // boolean) — treat either as archived.
    archived: t.archived === true || tags.includes(ARCHIVE_TAG),
    due: typeof t.due === "string" ? t.due : null,
    scheduled: typeof t.scheduled === "string" ? t.scheduled : null,
    blockedBy: Array.isArray(t.blockedBy) ? t.blockedBy : null,
    timeEstimate: typeof t.timeEstimate === "number" ? t.timeEstimate : null,
    tags,
    contexts: strArray(t.contexts),
    recurrence: typeof t.recurrence === "string" ? t.recurrence : null,
    ef_effort: t.ef_effort,
    ef_primary: t.ef_primary,
  };
}
