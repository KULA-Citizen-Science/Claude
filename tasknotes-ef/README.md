# TaskNotes EF Layer

An Obsidian **companion plugin for [TaskNotes](https://github.com/callumalpass/tasknotes)**
that adds an executive-function (EF) layer over the tasks TaskNotes already
owns, to **lower the decision cost of starting a task**.

TaskNotes stays the source of truth. This plugin never writes frontmatter
directly — it reads tasks and writes computed `ef_*` fields exclusively through
the TaskNotes runtime API (`app.plugins.getPlugin("tasknotes").api`), gated on
`apiVersion === 1` and per-method capabilities.

> **Milestone 1 = classification only.** Nudges, initiation strategies, the
> next-action picker, embeddings, and network calls are out of scope.

## Setup (one time)

The plugin **verifies** its fields but never creates them. In
**Settings → Task Properties → "Add new user field"**, create these seven:

| Display Name | Property key | Type |
|---|---|---|
| EF Primary | `ef_primary` | Text |
| EF Secondary | `ef_secondary` | List |
| EF Load | `ef_load` | Number |
| EF Social | `ef_social` | Text |
| EF Effort | `ef_effort` | Number |
| EF Confidence | `ef_confidence` | Number |
| EF Classified At | `ef_classified_at` | Text |

If any are missing, the plugin shows a persistent notice listing them and stays
passive (it won't write) until they exist.

## What it does

- **EF: Start something** (command + ribbon ▶) — the point of the whole plugin.
  Shows **one** easiest-to-start task (open, not blocked, not parked for later),
  why it was picked, and a concrete **first move**, with `5m / 25m / more` size
  filters and a *Show another* re-roll. `Start` marks it in-progress and opens it.
  "Ease me in" ordering: lowest effort first, urgency only as a tiebreak.
- **On task creation** — classifies the task and writes its EF profile.
- **EF: reclassify vault (dry run)** — previews what it *would* write (console +
  notice); writes nothing.
- **EF: reclassify vault (apply)** — classifies every task with no
  `ef_classified_at` stamp.
- **EF: install triage view** — copies `views/ef-triage.base` into
  `TaskNotes/Views/` (a Bases view grouping open tasks by `ef_primary`,
  lowest-effort first). Requires the **Bases** core plugin.

## The EF taxonomy

Seven barrier categories for `ef_primary`/`ef_secondary`: `initiation`,
`planning`, `organization`, `focus`, `decision`, `emotional`, `routine`.
`duration`/`time_pressure`/`routine` are derived **only** from the structured
`timeEstimate`/`due`/`recurrence` fields — never by re-parsing text. See
`CLAUDE.md` for the full taxonomy and scales.

## Development

```bash
npm install
npm test        # vitest — the pure classifier + adapter + planning logic
npm run build   # tsc --noEmit + esbuild -> main.js
```

Architecture: `src/classifier/**` is pure (zero `obsidian`/TaskNotes imports,
unit-tested). `src/gateway/tasknotes-gateway.ts` is the sole choke point for
TaskNotes/Obsidian access and enforces every hard constraint (version +
capability gating, `lifecycle.ready()` retry, `{source:"ef-layer"}` mutation
context, self-write event filtering).

### Manual in-vault check

1. Load the plugin in a vault with TaskNotes installed.
2. Before creating the fields: confirm the missing-fields notice appears.
3. Create the seven fields, reload: create a task and confirm `ef_*` values
   appear in its frontmatter.
4. Run **EF: install triage view**, enable Bases, open the view.
