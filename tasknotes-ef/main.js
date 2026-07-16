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
var import_obsidian3 = require("obsidian");

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
  const startOfDay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const today = startOfDay(now);
  const dueDay = startOfDay(new Date(parsed));
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

// src/main.ts
var EFPlugin = class extends import_obsidian3.Plugin {
  async onload() {
    this.gateway = new TaskNotesGateway(this.app);
    registerReclassifyCommands(this, this.gateway);
    registerInstallViewCommand(this);
    this.app.workspace.onLayoutReady(() => void this.activate());
  }
  async activate() {
    const ready = await this.gateway.whenReady();
    if (!ready) {
      new import_obsidian3.Notice(
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
    new import_obsidian3.Notice(
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
