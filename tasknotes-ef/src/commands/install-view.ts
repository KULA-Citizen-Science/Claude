// "EF: install triage view" — copies the shipped ef-triage.base into the vault's
// TaskNotes/Views/ folder. This writes a Bases view config file (a static asset),
// not task data, so it does not go through the TaskNotes API. It never
// overwrites an existing file, so a customized view is safe.

import { Notice, normalizePath, type Plugin } from "obsidian";
import { EF_TRIAGE_BASE, EF_TRIAGE_VIEW_PATH } from "../views/ef-triage-base";

async function ensureFolder(plugin: Plugin, folderPath: string): Promise<void> {
  const parts = folderPath.split("/");
  let current = "";
  for (const part of parts) {
    current = current ? `${current}/${part}` : part;
    if (!(await plugin.app.vault.adapter.exists(current))) {
      await plugin.app.vault.createFolder(current);
    }
  }
}

export async function installTriageView(plugin: Plugin): Promise<void> {
  const path = normalizePath(EF_TRIAGE_VIEW_PATH);

  if (await plugin.app.vault.adapter.exists(path)) {
    new Notice(`EF: ${EF_TRIAGE_VIEW_PATH} already exists — not overwriting.`);
    return;
  }

  try {
    await ensureFolder(plugin, "TaskNotes/Views");
    await plugin.app.vault.create(path, EF_TRIAGE_BASE);
    new Notice(`EF: installed ${EF_TRIAGE_VIEW_PATH}. Enable the Bases core plugin, then open it.`);
  } catch (e) {
    console.error("[ef-layer] failed to install triage view:", e);
    new Notice("EF: failed to install the triage view — see console.");
  }
}

export function registerInstallViewCommand(plugin: Plugin): void {
  plugin.addCommand({
    id: "install-triage-view",
    name: "EF: install triage view",
    callback: () => void installTriageView(plugin),
  });
}
