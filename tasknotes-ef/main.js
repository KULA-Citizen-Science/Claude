"use strict";
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

// src/main.ts
var main_exports = {};
__export(main_exports, {
  default: () => EFPlugin
});
module.exports = __toCommonJS(main_exports);
var import_obsidian4 = require("obsidian");

// src/classifier/types.ts
var EF_CATEGORIES = [
  "initiation",
  // vague / aversive start; the "just begin" barrier
  "planning",
  // needs decomposition or sequencing before it can start
  "organization",
  // needs gathering materials / context / info first
  "focus",
  // long, sustained-attention demand
  "decision",
  // ambiguous; requires choosing among options
  "emotional",
  // anxiety / aversion / conflict-laden
  "routine"
  // habitual, low-friction, recurring
];

// src/classifier/signals.ts
var DAY_MS = 864e5;
function bucketDuration(timeEstimate) {
  if (timeEstimate == null || !Number.isFinite(timeEstimate) || timeEstimate <= 0) {
    return "none";
  }
  if (timeEstimate < 30) return "quick";
  if (timeEstimate <= 120) return "medium";
  return "long";
}
function bucketTimePressure(due, now) {
  if (!due) return "none";
  const parsed = Date.parse(due);
  if (Number.isNaN(parsed)) return "none";
  const startOfDay2 = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const today = startOfDay2(now);
  const dueDay = startOfDay2(new Date(parsed));
  if (dueDay < today) return "overdue";
  if (dueDay <= today + 2 * DAY_MS) return "soon";
  return "none";
}
function isRoutine(recurrence) {
  return typeof recurrence === "string" && recurrence.trim().length > 0;
}
function deriveSignals(dto, now) {
  return {
    duration: bucketDuration(dto.timeEstimate),
    timePressure: bucketTimePressure(dto.due, now),
    routine: isRoutine(dto.recurrence)
  };
}

// src/classifier/lexicon.ts
var TITLE_CUES = [
  {
    category: "planning",
    weight: 2,
    terms: [
      // en
      "plan",
      "outline",
      "roadmap",
      "strategy",
      "break down",
      "breakdown",
      "figure out",
      "scope",
      "sketch out",
      "design a",
      // de
      "planen",
      "\xFCberleg",
      "ueberleg",
      "konzept",
      "entwurf",
      "strukturier",
      "vorbereiten",
      "ausarbeiten",
      "erarbeiten"
    ]
  },
  {
    category: "decision",
    weight: 2,
    terms: [
      // en
      "decide",
      "choose",
      "pick ",
      "select",
      "evaluate",
      "compare",
      "which ",
      "whether",
      "option",
      "vs ",
      // de
      "entscheiden",
      "ausw\xE4hlen",
      "auswaehlen",
      "w\xE4hlen",
      "waehlen",
      "vergleichen",
      " ob "
    ]
  },
  {
    category: "organization",
    weight: 2,
    terms: [
      // en
      "organize",
      "organise",
      "sort",
      "file ",
      "tidy",
      "clean up",
      "gather",
      "collect",
      "look up",
      "research",
      "compile",
      "catalog",
      "find ",
      // de
      "organisier",
      "sortier",
      "einsammeln",
      "sammeln",
      "suchen",
      "nachsehen",
      "nachschauen",
      "nachschlagen",
      "pr\xFCfen",
      "pruefen",
      "liste",
      "raussuchen",
      "mitnehmen",
      "einpacken",
      "packen"
    ]
  },
  {
    category: "focus",
    weight: 2,
    terms: [
      // en
      "write",
      "draft",
      "read ",
      "review",
      "study",
      "analyze",
      "analyse",
      "implement",
      "edit ",
      "report",
      "chapter",
      "essay",
      // de
      "schreiben",
      "lesen",
      "bericht",
      "nachbereiten",
      "posten",
      "blogpost",
      "notizen"
    ]
  },
  {
    category: "initiation",
    weight: 2,
    terms: [
      // en
      "start",
      "begin",
      "set up",
      "kick off",
      "kickoff",
      "initiate",
      "get going",
      "create a",
      "reserve",
      "booking",
      // de
      "anfangen",
      "beginnen",
      "starten",
      "loslegen",
      "einrichten",
      "erstellen",
      "besorgen",
      "buchen"
    ]
  },
  {
    category: "emotional",
    weight: 3,
    terms: [
      // en
      "taxes",
      "dentist",
      "doctor",
      "apolog",
      "confront",
      "difficult conversation",
      "cancel",
      "complaint",
      "insurance",
      "argument",
      "chase up",
      "overdue bill",
      "reimburse",
      "invoice",
      "refund",
      // de
      "steuer",
      "arzt",
      "rechnung",
      "abrechnung",
      "versicherung",
      "k\xFCndigen",
      "kuendigen",
      "mahnung",
      "ausgleichen",
      "erstattung"
    ]
  },
  {
    category: "routine",
    weight: 2,
    terms: [
      // en
      "water the",
      "take out",
      "laundry",
      "dishes",
      "standup",
      "check email",
      "backup",
      "chores",
      "refill",
      "restock",
      // de
      "einkaufen",
      "putzen",
      "w\xE4sche",
      "waesche",
      "m\xFCll",
      "muell",
      "nachf\xFCllen",
      "nachfuellen",
      "gie\xDFen",
      "giessen"
    ]
  }
];
var SOCIAL_LIVE_TERMS = [
  // en
  "call",
  "meet",
  "meeting",
  "interview",
  "standup",
  "sync",
  "1:1",
  "one-on-one",
  "zoom",
  "present to",
  "catch up with",
  // de
  "anrufen",
  "anruf",
  "telefon",
  "treffen",
  "zusammensetzen",
  "besprechen",
  "gespr\xE4ch",
  "gespraech"
];
var SOCIAL_ASYNC_TERMS = [
  // en
  "email",
  "reply",
  "message",
  "text ",
  "dm ",
  "slack",
  "respond",
  "write to",
  "send ",
  // de
  "e-mail",
  "mailen",
  "senden",
  "schicken",
  "antworten",
  "nachricht",
  "posten"
];
var SOCIAL_LIVE_CONTEXTS = ["phone", "call", "meeting", "zoom", "office", "telefon"];
var SOCIAL_ASYNC_CONTEXTS = ["email", "slack", "online", "mail"];

// src/classifier/classify.ts
function zeroScores() {
  return Object.fromEntries(EF_CATEGORIES.map((c) => [c, 0]));
}
function clamp(n, lo, hi) {
  return Math.min(hi, Math.max(lo, n));
}
function round2(n) {
  return Math.round(n * 100) / 100;
}
function textBlob(dto) {
  return [dto.title ?? "", ...dto.tags ?? [], ...dto.contexts ?? []].join(" ").toLowerCase();
}
function scoreCategories(dto, signals) {
  const scores = zeroScores();
  const blob = textBlob(dto);
  for (const cue of TITLE_CUES) {
    if (cue.terms.some((term) => blob.includes(term))) {
      scores[cue.category] += cue.weight;
    }
  }
  if (signals.routine) scores.routine += 3;
  if (signals.duration === "long") scores.focus += 2;
  if (signals.duration === "none") scores.initiation += 1;
  return scores;
}
function pickPrimarySecondary(scores) {
  let primary = EF_CATEGORIES[0];
  let best = -Infinity;
  for (const c of EF_CATEGORIES) {
    if (scores[c] > best) {
      best = scores[c];
      primary = c;
    }
  }
  if (best <= 0) return { primary: "initiation", secondary: [] };
  const secondary = EF_CATEGORIES.filter(
    (c) => c !== primary && scores[c] > 0 && scores[c] >= best * 0.5
  ).sort((a, b) => scores[b] - scores[a]);
  return { primary, secondary };
}
function computeSocial(dto) {
  const title = (dto.title ?? "").toLowerCase();
  const contexts = (dto.contexts ?? []).map((c) => c.toLowerCase());
  const titleHas = (terms) => terms.some((t) => title.includes(t));
  const contextHas = (roots) => contexts.some((c) => roots.some((r) => c.includes(r)));
  if (titleHas(SOCIAL_LIVE_TERMS) || contextHas(SOCIAL_LIVE_CONTEXTS)) return "live";
  if (titleHas(SOCIAL_ASYNC_TERMS) || contextHas(SOCIAL_ASYNC_CONTEXTS)) return "async";
  return "solo";
}
function computeLoad(signals, scores, secondaryCount) {
  let load = 1;
  if (signals.duration === "medium") load = 2;
  if (signals.duration === "long") load = 3;
  if (scores.planning > 0 || scores.decision > 0) load += 1;
  if (secondaryCount >= 2) load += 1;
  return clamp(load, 1, 3);
}
function computeEffort(primary, signals) {
  let effort = 2;
  if (primary === "routine") effort = 1;
  if (primary === "initiation" || primary === "planning" || primary === "decision") effort = 3;
  if (primary === "emotional") effort = 4;
  if (signals.duration === "long") effort += 1;
  if (signals.duration === "quick") effort -= 1;
  if (signals.timePressure === "overdue") effort += 1;
  return clamp(effort, 1, 4);
}
function computeConfidence(scores) {
  const sorted = EF_CATEGORIES.map((c) => scores[c]).sort((a, b) => b - a);
  const top = sorted[0] ?? 0;
  const runnerUp = sorted[1] ?? 0;
  if (top <= 0) return 0.2;
  const margin = (top - runnerUp) / top;
  const evidence = Math.min(1, top / 6);
  return round2(clamp(0.2 + 0.8 * evidence * (0.5 + 0.5 * margin), 0, 1));
}
function classify(dto, now = /* @__PURE__ */ new Date()) {
  const signals = deriveSignals(dto, now);
  const scores = scoreCategories(dto, signals);
  const { primary, secondary } = pickPrimarySecondary(scores);
  return {
    ef_primary: primary,
    ef_secondary: secondary,
    ef_load: computeLoad(signals, scores, secondary.length),
    ef_social: computeSocial(dto),
    ef_effort: computeEffort(primary, signals),
    ef_confidence: computeConfidence(scores)
  };
}

// src/fields.ts
var EF_FIELDS = [
  { key: "ef_primary", displayName: "EF Primary", type: "Text", description: "Dominant EF barrier to starting" },
  { key: "ef_secondary", displayName: "EF Secondary", type: "List", description: "Other EF barriers present" },
  { key: "ef_load", displayName: "EF Load", type: "Number", description: "Working-memory / cognitive load (1-3)" },
  { key: "ef_social", displayName: "EF Social", type: "Text", description: "Social demand: solo | async | live" },
  { key: "ef_effort", displayName: "EF Effort", type: "Number", description: "Activation energy to begin (1-4)" },
  { key: "ef_confidence", displayName: "EF Confidence", type: "Number", description: "Classifier confidence (0-1)" },
  { key: "ef_classified_at", displayName: "EF Classified At", type: "Text", description: "Provenance stamp (ISO); presence = classified" }
];
var EF_CLASSIFIED_AT_KEY = "ef_classified_at";
var EF_FIELD_KEYS = EF_FIELDS.map((f) => f.key);
function findMissingFields(present) {
  return EF_FIELDS.filter((f) => !present.has(f.key));
}
function buildEfPatch(classification, classifiedAt) {
  return {
    ef_primary: classification.ef_primary,
    ef_secondary: classification.ef_secondary,
    ef_load: classification.ef_load,
    ef_social: classification.ef_social,
    ef_effort: classification.ef_effort,
    ef_confidence: classification.ef_confidence,
    [EF_CLASSIFIED_AT_KEY]: classifiedAt
  };
}

// src/gateway/tasknotes-gateway.ts
var MUTATION_SOURCE = "ef-layer";
function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
function randomId() {
  const c = globalThis.crypto;
  return c?.randomUUID ? c.randomUUID() : `ef-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
var TaskNotesGateway = class {
  constructor(app) {
    this.app = app;
  }
  plugin() {
    const plugins = this.app.plugins;
    return plugins?.getPlugin("tasknotes") ?? null;
  }
  /** Resolve the runtime API with the version gate applied. Null when TaskNotes
   *  is missing, not yet loaded, or a different major API version. */
  resolve() {
    const api = this.plugin()?.api;
    if (!api || api.apiVersion !== 1) return null;
    return api;
  }
  isAvailable() {
    return this.resolve() !== null;
  }
  /** Resolve and require a capability in one step. */
  apiWith(capability) {
    const api = this.resolve();
    if (!api || !api.hasCapability(capability)) return null;
    return api;
  }
  /**
   * Wait until TaskNotes is present and its runtime reports ready. Handles the
   * common case where TaskNotes loads after us, by polling for the API up to
   * `timeoutMs`, then awaiting `lifecycle.ready()`. Returns false on timeout.
   */
  async whenReady(timeoutMs = 15e3, pollMs = 250) {
    const deadline = Date.now() + timeoutMs;
    let api = this.resolve();
    while (!api && Date.now() < deadline) {
      await sleep(pollMs);
      api = this.resolve();
    }
    if (!api) return false;
    try {
      await api.lifecycle.ready();
      return true;
    } catch {
      return false;
    }
  }
  context(reason) {
    return { source: MUTATION_SOURCE, correlationId: randomId(), reason };
  }
  /** The configured user-field property keys, or null if the catalog is
   *  unreadable (TaskNotes absent / capability missing). */
  userFieldKeys() {
    const api = this.apiWith("catalog.read");
    if (!api) return null;
    let defs;
    try {
      defs = api.catalog.userFields() ?? [];
    } catch {
      return null;
    }
    return new Set(defs.map((f) => f.key).filter((k) => Boolean(k)));
  }
  /**
   * Subscribe to `task.created`, skipping events caused by our own writes.
   * Returns an EventRef to hand to Plugin.registerEvent, or null if the events
   * capability is unavailable.
   */
  onTaskCreated(handler) {
    const api = this.apiWith("tasks.events");
    if (!api) return null;
    return api.events.on("task.created", (event) => {
      if (event.source === MUTATION_SOURCE) return;
      const task = event.task ?? event.after;
      if (task) handler(task, event);
    });
  }
  /** Write an EF patch through the update service, with mutation context and
   *  typed-error handling. */
  async updateTask(path, patch, reason) {
    const api = this.apiWith("tasks.write");
    if (!api) return { ok: false, reason: "no-capability" };
    const result = await api.errors.toResult(
      () => api.tasks.update(path, patch, this.context(reason))
    );
    if (result.ok) return { ok: true, task: result.value };
    return { ok: false, reason: "error", code: result.error.code, message: result.error.message };
  }
  /** Set a task's status through the update service, with mutation context. */
  async setStatus(path, status, reason) {
    const api = this.apiWith("tasks.write");
    if (!api) return { ok: false, reason: "no-capability" };
    const result = await api.errors.toResult(
      () => api.tasks.setStatus(path, status, this.context(reason))
    );
    if (result.ok) return { ok: true, task: result.value };
    return { ok: false, reason: "error", code: result.error.code, message: result.error.message };
  }
  /** The set of status values TaskNotes treats as completed (defaults to
   *  {"done"} when the catalog is unreadable). */
  completedStatuses() {
    const api = this.apiWith("catalog.read");
    if (!api) return /* @__PURE__ */ new Set(["done"]);
    try {
      const set = /* @__PURE__ */ new Set();
      for (const s of api.catalog.statuses() ?? []) {
        if (s?.isCompleted && typeof s.value === "string") set.add(s.value);
      }
      return set.size ? set : /* @__PURE__ */ new Set(["done"]);
    } catch {
      return /* @__PURE__ */ new Set(["done"]);
    }
  }
  /** Title of the task's first not-completed subtask, or null. Used to suggest a
   *  concrete first step. */
  async firstIncompleteSubtask(path) {
    const api = this.apiWith("relationships.read");
    if (!api) return null;
    const completed = this.completedStatuses();
    try {
      for (const sub of await api.relationships.subtasks(path) ?? []) {
        if (!sub) continue;
        if (typeof sub.status === "string" && completed.has(sub.status)) continue;
        if (typeof sub.title === "string" && sub.title.trim()) return sub.title;
      }
      return null;
    } catch {
      return null;
    }
  }
  /** All tasks in scope, preferring the stable query API and falling back to
   *  tasks.list(). Returns [] when nothing is readable. */
  async listTasks() {
    const q = this.apiWith("query.tasks");
    if (q) {
      try {
        const res = await q.query.tasks({ scope: { includeArchived: false } });
        return res.tasks ?? [];
      } catch {
      }
    }
    const api = this.apiWith("tasks.read");
    if (!api) return [];
    try {
      return await api.tasks.list() ?? [];
    } catch {
      return [];
    }
  }
};

// src/gateway/task-dto.ts
function asString(v) {
  return typeof v === "string" ? v : "";
}
function asStringArray(v) {
  return Array.isArray(v) ? v.filter((x) => typeof x === "string") : [];
}
function asNumberOrNull(v) {
  return typeof v === "number" && Number.isFinite(v) ? v : null;
}
function nonEmptyOrNull(v) {
  const s = asString(v);
  return s.length > 0 ? s : null;
}
function toTaskDTO(task) {
  return {
    title: asString(task.title),
    tags: asStringArray(task.tags),
    contexts: asStringArray(task.contexts),
    timeEstimate: asNumberOrNull(task.timeEstimate),
    due: nonEmptyOrNull(task.due),
    recurrence: nonEmptyOrNull(task.recurrence)
  };
}
function isClassified(task) {
  return nonEmptyOrNull(task[EF_CLASSIFIED_AT_KEY]) !== null;
}

// src/commands/reclassify.ts
var import_obsidian = require("obsidian");
function planReclassify(tasks, classifiedAt) {
  const alreadyClassified = [];
  const writes = [];
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
function describe(w) {
  const c = w.classification;
  const secondary = c.ef_secondary.length ? ` +${c.ef_secondary.join(",")}` : "";
  return `${w.path} \u2192 ${c.ef_primary}${secondary} (load ${c.ef_load}, effort ${c.ef_effort}, ${Math.round(c.ef_confidence * 100)}%)`;
}
async function runReclassify(gateway, opts) {
  if (!gateway.isAvailable()) {
    new import_obsidian.Notice("EF: TaskNotes is not available \u2014 cannot reclassify.");
    return;
  }
  if (opts.apply) {
    const keys = gateway.userFieldKeys();
    const missing = keys ? findMissingFields(keys) : [];
    if (missing.length > 0) {
      new import_obsidian.Notice(`EF: ${missing.length} EF field(s) not configured \u2014 add them before applying. Dry run still works.`);
      return;
    }
  }
  const tasks = await gateway.listTasks();
  const plan = planReclassify(tasks, (/* @__PURE__ */ new Date()).toISOString());
  if (!opts.apply) {
    console.log(
      `[ef-layer] DRY RUN \u2014 would classify ${plan.writes.length} of ${plan.scanned} (${plan.alreadyClassified.length} already classified):
` + plan.writes.map(describe).join("\n")
    );
    new import_obsidian.Notice(
      `EF dry run: would classify ${plan.writes.length} of ${plan.scanned} tasks (${plan.alreadyClassified.length} already classified). Details in console.`,
      8e3
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
  new import_obsidian.Notice(
    `EF reclassify: wrote ${written}, skipped ${plan.alreadyClassified.length} already-classified` + (failed ? `, ${failed} failed (see console)` : "") + ".",
    8e3
  );
}
function registerReclassifyCommands(plugin, gateway) {
  plugin.addCommand({
    id: "reclassify-vault-dry-run",
    name: "EF: reclassify vault (dry run)",
    callback: () => void runReclassify(gateway, { apply: false })
  });
  plugin.addCommand({
    id: "reclassify-vault-apply",
    name: "EF: reclassify vault (apply)",
    callback: () => void runReclassify(gateway, { apply: true })
  });
}

// src/commands/install-view.ts
var import_obsidian2 = require("obsidian");

// src/views/ef-triage-base.ts
var EF_TRIAGE_VIEW_PATH = "TaskNotes/Views/ef-triage.base";
var EF_TRIAGE_BASE = `# EF Triage \u2014 open tasks grouped by their primary executive-function barrier.
# Shipped by the TaskNotes EF Layer companion plugin. No custom view code: this
# is a standard TaskNotes Bases (tasknotesTaskList) view. Within each group,
# lowest-effort tasks come first, so the easiest thing to start is on top.
#
# Assumes tag-based task identification (#task). If your vault identifies tasks
# by a property instead, replace the file.hasTag("task") line accordingly.

views:
  - type: tasknotesTaskList
    name: "EF Triage"
    filters:
      and:
        - file.hasTag("task")
        - status != "done"
    groupBy:
      property: note.ef_primary
      direction: ASC
    order:
      - note.ef_primary
      - note.ef_effort
      - note.ef_load
      - note.ef_social
      - note.status
      - note.priority
      - note.due
      - file.tasks
    sort:
      - column: ef_effort
        direction: ASC
      - column: ef_confidence
        direction: DESC
`;

// src/commands/install-view.ts
async function ensureFolder(plugin, folderPath) {
  const parts = folderPath.split("/");
  let current = "";
  for (const part of parts) {
    current = current ? `${current}/${part}` : part;
    if (!await plugin.app.vault.adapter.exists(current)) {
      await plugin.app.vault.createFolder(current);
    }
  }
}
async function installTriageView(plugin) {
  const path = (0, import_obsidian2.normalizePath)(EF_TRIAGE_VIEW_PATH);
  if (await plugin.app.vault.adapter.exists(path)) {
    new import_obsidian2.Notice(`EF: ${EF_TRIAGE_VIEW_PATH} already exists \u2014 not overwriting.`);
    return;
  }
  try {
    await ensureFolder(plugin, "TaskNotes/Views");
    await plugin.app.vault.create(path, EF_TRIAGE_BASE);
    new import_obsidian2.Notice(`EF: installed ${EF_TRIAGE_VIEW_PATH}. Enable the Bases core plugin, then open it.`);
  } catch (e) {
    console.error("[ef-layer] failed to install triage view:", e);
    new import_obsidian2.Notice("EF: failed to install the triage view \u2014 see console.");
  }
}
function registerInstallViewCommand(plugin) {
  plugin.addCommand({
    id: "install-triage-view",
    name: "EF: install triage view",
    callback: () => void installTriageView(plugin)
  });
}

// src/picker/start-modal.ts
var import_obsidian3 = require("obsidian");

// src/picker/select.ts
var DURATION_RANK = { quick: 0, medium: 1, none: 2, long: 3 };
var URGENCY_RANK = { overdue: 0, soon: 1, none: 2 };
var DURATION_LABEL = {
  quick: "quick",
  medium: "medium",
  long: "long",
  none: "unsized"
};
function toDTO(pt) {
  return {
    title: pt.title ?? "",
    tags: pt.tags ?? [],
    contexts: pt.contexts ?? [],
    timeEstimate: pt.timeEstimate ?? null,
    due: pt.due ?? null,
    recurrence: pt.recurrence ?? null
  };
}
function isEffort(v) {
  return v === 1 || v === 2 || v === 3 || v === 4;
}
function isCategory(v) {
  return typeof v === "string" && EF_CATEGORIES.includes(v);
}
function startOfDay(d) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}
function isDeferred(scheduled, now) {
  if (!scheduled) return false;
  const t = Date.parse(scheduled);
  if (Number.isNaN(t)) return false;
  return startOfDay(new Date(t)) > startOfDay(now);
}
function resolveEf(pt) {
  const c = classify(toDTO(pt));
  return {
    effort: isEffort(pt.ef_effort) ? pt.ef_effort : c.ef_effort,
    primary: isCategory(pt.ef_primary) ? pt.ef_primary : c.ef_primary
  };
}
function passesTimeCap(cap, timeEstimate, effort) {
  if (cap === "more") return true;
  const est = typeof timeEstimate === "number" && timeEstimate > 0 ? timeEstimate : null;
  if (cap === "5m") return est !== null ? est <= 15 : effort === 1;
  return est !== null ? est <= 30 : effort <= 2;
}
function rankCandidates(tasks, options = {}) {
  const now = options.now ?? /* @__PURE__ */ new Date();
  const cap = options.timeCap ?? "more";
  const completed = options.completedStatuses ?? /* @__PURE__ */ new Set(["done"]);
  const exclude = options.exclude ?? /* @__PURE__ */ new Set();
  const scored = [];
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
        urgency
      },
      keys: [effort, DURATION_RANK[duration], URGENCY_RANK[urgency], (pt.title ?? "").toLowerCase()]
    });
  }
  scored.sort((a, b) => {
    for (let i = 0; i < 3; i++) {
      const d = a.keys[i] - b.keys[i];
      if (d !== 0) return d;
    }
    return a.keys[3].localeCompare(b.keys[3]);
  });
  return scored.map((s) => s.c);
}
var FIRST_STEP = {
  initiation: "Do the smallest physical first action \u2014 open the thing. Nothing more.",
  planning: "Write just 3 bullet sub-steps. Don't do them yet \u2014 only list them.",
  organization: "Open the one file, link, or email you'll need first.",
  focus: "Open the doc and read only the first line. Put your cursor in.",
  decision: "Name the two options out loud. You don't have to decide yet.",
  emotional: "Set a 5-minute timer. You have permission to stop when it rings.",
  routine: "It's a quick one \u2014 just do it now."
};
function firstStep(primary, subtaskTitle) {
  if (subtaskTitle && subtaskTitle.trim()) return `Start with: \u201C${subtaskTitle.trim()}\u201D`;
  return FIRST_STEP[primary];
}

// src/picker/map.ts
var ARCHIVE_TAG = "archived";
function strArray(v) {
  return Array.isArray(v) ? v.filter((x) => typeof x === "string") : [];
}
function toPickerTask(t) {
  const tags = strArray(t.tags);
  return {
    path: t.path,
    title: typeof t.title === "string" ? t.title : "",
    status: typeof t.status === "string" ? t.status : void 0,
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
    ef_primary: t.ef_primary
  };
}

// src/picker/start-modal.ts
var IN_PROGRESS_STATUS = "in-progress";
var EFFORT_LABEL = { 1: "very easy", 2: "easy", 3: "moderate", 4: "heavy" };
var SIZE_LABEL = {
  quick: "quick",
  medium: "medium",
  long: "long",
  unsized: null
};
var URGENCY_LABEL = {
  overdue: "\u26A0 overdue",
  soon: "due soon",
  none: null
};
var STYLE_ID = "ef-start-styles";
var STYLES = `
.ef-start-modal .ef-filters { display:flex; gap:6px; margin-bottom:14px; }
.ef-start-modal .ef-filter { font-size:0.8em; padding:3px 10px; border-radius:12px; cursor:pointer;
  background: var(--background-modifier-border); border:none; color: var(--text-normal); }
.ef-start-modal .ef-filter.is-active { background: var(--interactive-accent); color: var(--text-on-accent); }
.ef-start-modal .ef-pick-title { font-size:1.3em; font-weight:600; cursor:pointer; }
.ef-start-modal .ef-pick-title:hover { text-decoration:underline; }
.ef-start-modal .ef-chips { display:flex; gap:6px; flex-wrap:wrap; margin:8px 0 0; }
.ef-start-modal .ef-chip { font-size:0.78em; padding:2px 8px; border-radius:10px;
  background: var(--background-modifier-border); }
.ef-start-modal .ef-firststep { margin:14px 0; padding:10px 12px; border-left:3px solid var(--interactive-accent);
  background: var(--background-secondary); border-radius:4px; }
.ef-start-modal .ef-firststep-label { font-size:0.72em; text-transform:uppercase; opacity:0.7; letter-spacing:0.05em; }
.ef-start-modal .ef-firststep-text { margin-top:3px; }
.ef-start-modal .ef-actions { display:flex; gap:8px; margin-top:16px; flex-wrap:wrap; }
.ef-start-modal .ef-msg { opacity:0.75; padding:8px 0; }
`;
var TIME_CAPS = [
  { cap: "5m", label: "5 min" },
  { cap: "25m", label: "25 min" },
  { cap: "more", label: "more" }
];
var StartSomethingModal = class extends import_obsidian3.Modal {
  constructor(app, gateway) {
    super(app);
    this.timeCap = "more";
    this.exclude = /* @__PURE__ */ new Set();
    this.ranked = [];
    this.completed = /* @__PURE__ */ new Set(["done"]);
    this.gateway = gateway;
  }
  async onOpen() {
    injectStyles();
    this.titleEl.setText("Start something");
    this.modalEl.addClass("ef-start-modal");
    if (!this.gateway.isAvailable()) {
      this.renderMessage("TaskNotes isn't ready \u2014 can't pick a task right now.");
      return;
    }
    await this.reload();
  }
  onClose() {
    this.contentEl.empty();
  }
  /** Refetch tasks and re-rank (used on open and when the size filter changes). */
  async reload() {
    this.renderMessage("Finding the easiest thing to start\u2026");
    this.completed = this.gateway.completedStatuses();
    const tasks = (await this.gateway.listTasks()).map(toPickerTask);
    this.ranked = rankCandidates(tasks, {
      timeCap: this.timeCap,
      completedStatuses: this.completed,
      exclude: this.exclude
    });
    this.render();
  }
  render() {
    const { contentEl } = this;
    contentEl.empty();
    this.renderFilters(contentEl);
    const pick = this.ranked[0];
    if (!pick) {
      this.renderEmpty(contentEl);
      return;
    }
    this.renderPick(contentEl, pick);
  }
  renderFilters(parent) {
    const row = parent.createDiv({ cls: "ef-filters" });
    for (const { cap, label } of TIME_CAPS) {
      const btn = row.createEl("button", { text: label, cls: "ef-filter" });
      if (cap === this.timeCap) btn.addClass("is-active");
      btn.addEventListener("click", () => {
        if (this.timeCap !== cap) {
          this.timeCap = cap;
          void this.reload();
        }
      });
    }
  }
  renderPick(parent, pick) {
    const title = parent.createDiv({ cls: "ef-pick-title", text: pick.title || "(untitled task)" });
    title.addEventListener("click", () => this.openTask(pick.path));
    const chips = parent.createDiv({ cls: "ef-chips" });
    const labels = [
      EFFORT_LABEL[pick.effort],
      SIZE_LABEL[pick.durationLabel],
      URGENCY_LABEL[pick.urgency],
      pick.primary
    ].filter((x) => Boolean(x));
    for (const l of labels) chips.createSpan({ cls: "ef-chip", text: l });
    const step = parent.createDiv({ cls: "ef-firststep" });
    step.createDiv({ cls: "ef-firststep-label", text: "First move" });
    const stepText = step.createDiv({ cls: "ef-firststep-text", text: firstStep(pick.primary) });
    void this.gateway.firstIncompleteSubtask(pick.path).then((sub) => {
      if (sub && this.ranked[0]?.path === pick.path) stepText.setText(firstStep(pick.primary, sub));
    });
    const actions = parent.createDiv({ cls: "ef-actions" });
    this.button(actions, "play", "Start", "cta", () => this.startTask(pick.path));
    this.button(actions, "file", "Open", "", () => this.openTask(pick.path));
    this.button(actions, "dice", "Show another", "", () => this.showAnother(pick.path));
  }
  renderEmpty(parent) {
    const msg = this.timeCap === "more" ? "Nothing to start right now \u2014 everything's done, blocked, or parked for later. \u{1F389}" : "Nothing that small right now. Try a longer window.";
    parent.createDiv({ cls: "ef-msg", text: msg });
  }
  renderMessage(text) {
    const { contentEl } = this;
    contentEl.empty();
    contentEl.createDiv({ cls: "ef-msg", text });
  }
  button(parent, icon, label, cls, onClick) {
    const btn = parent.createEl("button", { cls });
    (0, import_obsidian3.setIcon)(btn.createSpan(), icon);
    btn.createSpan({ text: ` ${label}` });
    btn.addEventListener("click", onClick);
  }
  showAnother(currentPath) {
    this.exclude.add(currentPath);
    this.ranked = this.ranked.filter((c) => !this.exclude.has(c.path));
    this.render();
  }
  openTask(path) {
    void this.app.workspace.openLinkText(path, "", false);
    this.close();
  }
  async startTask(path) {
    const outcome = await this.gateway.setStatus(path, IN_PROGRESS_STATUS, "started from picker");
    if (!outcome.ok) new import_obsidian3.Notice("Couldn't mark it in-progress \u2014 opening it anyway.");
    this.openTask(path);
  }
};
function injectStyles() {
  if (document.getElementById(STYLE_ID)) return;
  const el = document.createElement("style");
  el.id = STYLE_ID;
  el.textContent = STYLES;
  document.head.appendChild(el);
}
function registerStartCommand(plugin, gateway) {
  plugin.addCommand({
    id: "start-something",
    name: "EF: Start something",
    callback: () => new StartSomethingModal(plugin.app, gateway).open()
  });
  plugin.addRibbonIcon(
    "play",
    "EF: Start something",
    () => new StartSomethingModal(plugin.app, gateway).open()
  );
}

// src/main.ts
var EFPlugin = class extends import_obsidian4.Plugin {
  async onload() {
    this.gateway = new TaskNotesGateway(this.app);
    registerReclassifyCommands(this, this.gateway);
    registerInstallViewCommand(this);
    registerStartCommand(this, this.gateway);
    this.app.workspace.onLayoutReady(() => void this.activate());
  }
  async activate() {
    const ready = await this.gateway.whenReady();
    if (!ready) {
      new import_obsidian4.Notice(
        "TaskNotes EF Layer: TaskNotes not found or not ready. The EF layer is inactive.",
        8e3
      );
      return;
    }
    if (!this.verifyFields()) return;
    const ref = this.gateway.onTaskCreated((task) => void this.classifyOnCreate(task.path, task));
    if (ref) this.registerEvent(ref);
  }
  /** Returns true when all seven ef_* fields are configured. */
  verifyFields() {
    const keys = this.gateway.userFieldKeys();
    if (!keys) return false;
    const missing = findMissingFields(keys);
    if (missing.length === 0) return true;
    const list = missing.map((f) => `\u2022 ${f.displayName} \u2014 key "${f.key}", type ${f.type}`).join("\n");
    new import_obsidian4.Notice(
      `TaskNotes EF Layer: ${missing.length} of ${EF_FIELDS.length} EF fields are missing.
Add them in Settings \u2192 Task Properties \u2192 "Add new user field":
${list}
The EF layer stays inactive until they exist.`,
      0
      // persist until dismissed
    );
    console.warn("[ef-layer] Missing EF user fields:", missing.map((f) => f.key));
    return false;
  }
  async classifyOnCreate(path, task) {
    const classification = classify(toTaskDTO(task));
    const patch = buildEfPatch(classification, (/* @__PURE__ */ new Date()).toISOString());
    const outcome = await this.gateway.updateTask(path, patch, "classify on create");
    if (!outcome.ok) {
      console.warn("[ef-layer] classify-on-create failed:", path, outcome);
    }
  }
};
