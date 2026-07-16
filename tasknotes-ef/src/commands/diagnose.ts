// "EF: Diagnose EF fields" — a mobile-readable report of what TaskNotes actually
// returns for user fields, so we can tell "fields not registered" from "plugin
// misreading the field shape" without a desktop dev console.

import { App, Modal, Plugin } from "obsidian";
import { EF_FIELD_KEYS } from "../fields";
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

  onOpen(): void {
    injectStyles();
    this.titleEl.setText("EF field diagnosis");
    const c = this.contentEl;
    c.addClass("ef-diagnose");

    if (!this.gateway.isAvailable()) {
      c.createEl("p", { text: "TaskNotes runtime API is not available (not loaded, or apiVersion ≠ 1)." });
      return;
    }

    const raw = this.gateway.userFieldsRaw();
    if (raw === null) {
      c.createEl("p", { text: "Could not read the field catalog (catalog.read capability unavailable)." });
      return;
    }

    c.createEl("p", { text: `TaskNotes reports ${raw.length} user field(s).` });

    const keys = this.gateway.userFieldKeys() ?? new Set<string>();
    const found = EF_FIELD_KEYS.filter((k) => keys.has(k));
    const missing = EF_FIELD_KEYS.filter((k) => !keys.has(k));

    c.createEl("p", { cls: found.length === EF_FIELD_KEYS.length ? "ef-ok" : "ef-bad",
      text: `EF fields detected: ${found.length}/${EF_FIELD_KEYS.length}` });
    if (missing.length) c.createEl("p", { text: `Missing: ${missing.join(", ")}` });

    c.createEl("p", { text: "Raw field data from TaskNotes (screenshot this if fields are missing):" });
    c.createEl("pre", { text: JSON.stringify(raw, null, 2) });
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
