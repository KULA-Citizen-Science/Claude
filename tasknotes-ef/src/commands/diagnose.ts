// "EF: Diagnose EF fields" — a mobile-readable report of what TaskNotes actually
// returns for user fields, so we can tell "fields not registered" from "plugin
// misreading the field shape" without a desktop dev console.

import { App, Modal, Plugin } from "obsidian";
import { EF_FIELD_KEYS } from "../fields";
import { resolveTaskPath } from "../picker/map";
import type { TaskNotesGateway } from "../gateway/tasknotes-gateway";

const STYLE_ID = "ef-diagnose-styles";
const STYLES = `
.ef-diagnose pre { white-space: pre-wrap; word-break: break-word; max-height: 40vh; overflow: auto;
  background: var(--background-secondary); padding: 10px; border-radius: 4px; font-size: 0.8em; }
.ef-diagnose .ef-ok { color: var(--text-success); }
.ef-diagnose .ef-bad { color: var(--text-error); }
`;

class DiagnoseModal extends Modal {
  constructor(app: App, private readonly gateway: TaskNotesGateway) {
    super(app);
  }

  async onOpen(): Promise<void> {
    injectStyles();
    this.titleEl.setText("EF diagnosis");
    const c = this.contentEl;
    c.addClass("ef-diagnose");

    if (!this.gateway.isAvailable()) {
      c.createEl("p", { text: "TaskNotes runtime API is not available (not loaded, or apiVersion ≠ 1)." });
      return;
    }

    this.renderFields(c);
    await this.renderTaskPaths(c);
  }

  private renderFields(c: HTMLElement): void {
    c.createEl("h4", { text: "User fields" });
    const raw = this.gateway.userFieldsRaw();
    if (raw === null) {
      c.createEl("p", { text: "Could not read the field catalog (catalog.read capability unavailable)." });
      return;
    }

    const keys = this.gateway.userFieldKeys() ?? new Set<string>();
    const found = EF_FIELD_KEYS.filter((k) => keys.has(k));
    const missing = EF_FIELD_KEYS.filter((k) => !keys.has(k));

    c.createEl("p", {
      cls: found.length === EF_FIELD_KEYS.length ? "ef-ok" : "ef-bad",
      text: `EF fields detected: ${found.length}/${EF_FIELD_KEYS.length}` + (missing.length ? ` (missing: ${missing.join(", ")})` : ""),
    });
  }

  private async renderTaskPaths(c: HTMLElement): Promise<void> {
    c.createEl("h4", { text: "Task paths" });
    const loading = c.createEl("p", { text: "Checking tasks…" });

    const tasks = await this.gateway.listTasks();
    loading.remove();

    const missing = tasks.filter((t) => !this.gateway.fileExists(resolveTaskPath(t)));
    c.createEl("p", {
      cls: missing.length ? "ef-bad" : "ef-ok",
      text: `listTasks() returned ${tasks.length}; ${missing.length} have no matching file on disk.`,
    });

    const sample = tasks.slice(0, 5).map((t) => {
      const p = resolveTaskPath(t);
      return `${this.gateway.fileExists(p) ? "✓" : "✗"} ${p || "(no path resolved)"}`;
    });
    c.createEl("p", { text: "Sample paths (✓ = file found), then the first raw task object:" });
    c.createEl("pre", {
      text: sample.join("\n") + (tasks[0] ? "\n\n" + JSON.stringify(tasks[0], null, 2) : ""),
    });
  }

  onClose(): void {
    this.contentEl.empty();
  }
}

function injectStyles(): void {
  if (document.getElementById(STYLE_ID)) return;
  const el = document.createElement("style");
  el.id = STYLE_ID;
  el.textContent = STYLES;
  document.head.appendChild(el);
}

export function registerDiagnoseCommand(plugin: Plugin, gateway: TaskNotesGateway): void {
  plugin.addCommand({
    id: "diagnose-ef-fields",
    name: "EF: Diagnose EF fields",
    callback: () => new DiagnoseModal(plugin.app, gateway).open(),
  });
}
