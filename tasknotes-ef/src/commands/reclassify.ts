// Batch reclassification (M1.4). The planning step is a pure function so it can
// be unit-tested; the command wrappers add Obsidian I/O (Notice) and the writes.

import { Notice, type Plugin } from "obsidian";
import { classify, type EFClassification } from "../classifier";
import { buildEfPatch, findMissingFields } from "../fields";
import { toTaskDTO, isClassified } from "../gateway/task-dto";
import type { TaskNotesGateway } from "../gateway/tasknotes-gateway";
import type { TaskNotesTask } from "../gateway/runtime-api";

export interface PlannedWrite {
  path: string;
  classification: EFClassification;
  patch: Record<string, unknown>;
}

export interface ReclassifyPlan {
  scanned: number;
  alreadyClassified: string[];
  writes: PlannedWrite[];
}

/**
 * Pure: decide what a reclassify run would write. "Unclassified" == no
 * `ef_classified_at` stamp. Classifies each unclassified task exactly once.
 */
export function planReclassify(tasks: TaskNotesTask[], classifiedAt: string): ReclassifyPlan {
  const alreadyClassified: string[] = [];
  const writes: PlannedWrite[] = [];

  for (const task of tasks) {
    if (isClassified(task)) {
      alreadyClassified.push(task.path);
      continue;
    }
    const classification = classify(toTaskDTO(task));
    writes.push({ path: task.path, classification, patch: buildEfPatch(classification, classifiedAt) });
  }

  return { scanned: tasks.length, alreadyClassified, writes };
}

function describe(w: PlannedWrite): string {
  const c = w.classification;
  const secondary = c.ef_secondary.length ? ` +${c.ef_secondary.join(",")}` : "";
  return `${w.path} → ${c.ef_primary}${secondary} (load ${c.ef_load}, effort ${c.ef_effort}, ${Math.round(c.ef_confidence * 100)}%)`;
}

/** Run a reclassify, either previewing (dry run) or applying the writes. */
export async function runReclassify(
  gateway: TaskNotesGateway,
  opts: { apply: boolean },
): Promise<void> {
  if (!gateway.isAvailable()) {
    new Notice("EF: TaskNotes is not available — cannot reclassify.");
    return;
  }

  // Applying to missing fields would silently create unregistered frontmatter;
  // block it (a dry run is still allowed, since it writes nothing).
  if (opts.apply) {
    const keys = gateway.userFieldKeys();
    const missing = keys ? findMissingFields(keys) : [];
    if (missing.length > 0) {
      new Notice(`EF: ${missing.length} EF field(s) not configured — add them before applying. Dry run still works.`);
      return;
    }
  }

  const tasks = await gateway.listTasks();
  const plan = planReclassify(tasks, new Date().toISOString());

  if (!opts.apply) {
    console.log(
      `[ef-layer] DRY RUN — would classify ${plan.writes.length} of ${plan.scanned} ` +
        `(${plan.alreadyClassified.length} already classified):\n` +
        plan.writes.map(describe).join("\n"),
    );
    new Notice(
      `EF dry run: would classify ${plan.writes.length} of ${plan.scanned} tasks ` +
        `(${plan.alreadyClassified.length} already classified). Details in console.`,
      8000,
    );
    return;
  }

  let written = 0;
  let failed = 0;
  for (const w of plan.writes) {
    const outcome = await gateway.updateTask(w.path, w.patch, "reclassify vault");
    if (outcome.ok) {
      written++;
    } else {
      failed++;
      console.warn("[ef-layer] reclassify write failed:", w.path, outcome);
    }
  }

  new Notice(
    `EF reclassify: wrote ${written}, skipped ${plan.alreadyClassified.length} already-classified` +
      (failed ? `, ${failed} failed (see console)` : "") +
      ".",
    8000,
  );
}

export function registerReclassifyCommands(plugin: Plugin, gateway: TaskNotesGateway): void {
  plugin.addCommand({
    id: "reclassify-vault-dry-run",
    name: "EF: reclassify vault (dry run)",
    callback: () => void runReclassify(gateway, { apply: false }),
  });
  plugin.addCommand({
    id: "reclassify-vault-apply",
    name: "EF: reclassify vault (apply)",
    callback: () => void runReclassify(gateway, { apply: true }),
  });
}
