// The EF Triage view as an embedded string, so the "install triage view"
// command can write it into the vault without reading the plugin's own files.
// This MUST stay byte-for-byte identical to views/ef-triage.base — a test
// (ef-triage-base.test.ts) enforces that.

export const EF_TRIAGE_VIEW_PATH = "TaskNotes/Views/ef-triage.base";

export const EF_TRIAGE_BASE = `# EF Triage — open tasks grouped by their primary executive-function barrier.
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
