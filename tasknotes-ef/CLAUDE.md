# TaskNotes EF Layer — project guide (EF taxonomy source of truth)

An Obsidian **companion plugin for TaskNotes** that adds an executive-function
(EF) layer over the tasks TaskNotes already owns. Its job: **reduce the decision
cost of starting a task.** TaskNotes stays the source of truth for task data.

## Hard constraints (do not violate)

- **TaskNotes owns task data. Never write frontmatter directly.** All mutations
  go through the runtime API: `app.plugins.getPlugin("tasknotes").api`.
- **Gate every use:** check `api.apiVersion === 1` and `api.hasCapability(...)`.
  Handle TaskNotes being absent or not yet loaded; retry after
  `api.lifecycle.ready()`.
- Pass a mutation context `{ source: "ef-layer", correlationId, reason }` on
  every write, and **ignore inbound events where `event.source === "ef-layer"`**
  to avoid self-trigger loops.
- **Do not reimplement anything TaskNotes provides:** no NL date parsing (use
  `api.nlp.parse`), no views (ship `.base` files), no scheduling/reminders, no
  time tracking, no pomodoro.

## EF taxonomy

Seven `ef_*` TaskNotes **user fields** (you create them in
*Settings → Task Properties*; the plugin only verifies they exist and writes to
them — it never creates them):

| Field | Type | Values | Meaning |
|---|---|---|---|
| `ef_primary` | Text | one category (below) | Dominant EF barrier to *starting* |
| `ef_secondary` | List | zero+ categories | Other barriers present |
| `ef_load` | Number | 1–3 | Working-memory / cognitive load |
| `ef_social` | Text | `solo` \| `async` \| `live` | Social demand |
| `ef_effort` | Number | 1–4 | Activation energy to begin |
| `ef_confidence` | Number | 0.0–1.0 | Classifier confidence |
| `ef_classified_at` | Text (ISO) | timestamp | Provenance stamp; presence = "classified" |

### `ef_primary` / `ef_secondary` categories (the seven EF barriers)

- **`initiation`** — vague / aversive start; the "just begin" barrier. Default
  fallback when nothing else scores.
- **`planning`** — needs decomposition or sequencing before it can start.
- **`organization`** — needs gathering materials / context / info first.
- **`focus`** — long, sustained-attention demand.
- **`decision`** — ambiguous; requires choosing among options.
- **`emotional`** — anxiety / aversion / conflict-laden.
- **`routine`** — habitual, low-friction, recurring.

### Derived signals (inputs, NOT stored fields)

Computed **only** from structured TaskNotes fields — never by re-parsing title
text:

- `duration` ← `timeEstimate`: none / quick (<30m) / medium (30–120m) / long (>120m)
- `timePressure` ← `due`: none / soon (≤2 days) / overdue
- `routine` ← `recurrence`: boolean (non-empty rule ⇒ routine)

## Architecture

- `src/classifier/**` — **pure**, zero `obsidian`/TaskNotes imports, unit-tested.
  Pipeline: `deriveSignals → scoreCategories → pickPrimarySecondary → compute*`.
  Tune behavior by editing `lexicon.ts` (data-only).
- `src/gateway/tasknotes-gateway.ts` — the only module besides `main.ts` that
  touches Obsidian/TaskNotes. Owns capability gating, `lifecycle.ready()`
  retry, mutation context, and self-write event filtering.
- `src/main.ts` — plugin entry: field verification, event wiring, commands.
- `views/ef-triage.base` — shipped view grouping open tasks by `ef_primary`.

## Commands

- **EF: reclassify vault (dry run)** — reports intended writes, writes nothing.
- **EF: reclassify vault (apply)** — classifies tasks with no `ef_classified_at`.
- **EF: install triage view** — copies `ef-triage.base` into `TaskNotes/Views/`.

## Build & test

```bash
npm install
npm test          # vitest — pure classifier unit tests
npm run build     # tsc --noEmit + esbuild → main.js
```

## Scope

Milestone 1 is **classification only**. Out of scope: initiation strategies,
nudges, the next-action picker, embeddings, any network call.
