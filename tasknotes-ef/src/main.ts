// Plugin entry point. Together with the gateway this is the only module that
// imports `obsidian`. It wires up: field verification (M1.1), classify-on-create
// (M1.3), and the batch commands (M1.4). All TaskNotes access goes through the
// gateway, so the hard constraints are enforced in one place.

import { Notice, Plugin } from "obsidian";
import { classify } from "./classifier";
import { EF_FIELDS, buildEfPatch, findMissingFields } from "./fields";
import { TaskNotesGateway } from "./gateway/tasknotes-gateway";
import { toTaskDTO } from "./gateway/task-dto";
import { registerReclassifyCommands } from "./commands/reclassify";
import { registerInstallViewCommand } from "./commands/install-view";
import { registerStartCommand } from "./picker/start-modal";
import { registerDiagnoseCommand } from "./commands/diagnose";

export default class EFPlugin extends Plugin {
  private gateway!: TaskNotesGateway;

  async onload(): Promise<void> {
    this.gateway = new TaskNotesGateway(this.app);

    // Commands are always available; they re-check readiness when invoked.
    registerReclassifyCommands(this, this.gateway);
    registerInstallViewCommand(this);
    registerStartCommand(this, this.gateway);
    registerDiagnoseCommand(this, this.gateway);

    // Defer live wiring until the workspace (and thus other plugins) are ready.
    this.app.workspace.onLayoutReady(() => void this.activate());
  }

  private async activate(): Promise<void> {
    const ready = await this.gateway.whenReady();
    if (!ready) {
      new Notice(
        "TaskNotes EF Layer: TaskNotes not found or not ready. The EF layer is inactive.",
        8000,
      );
      return;
    }

    // Verify fields; if any are missing we stay passive (no silent field creation).
    if (!this.verifyFields()) return;

    // M1.3 — classify newly created tasks (self-writes are filtered in the gateway).
    const ref = this.gateway.onTaskCreated((task) => void this.classifyOnCreate(task.path, task));
    if (ref) this.registerEvent(ref);
  }

  /** Returns true when all seven ef_* fields are configured. */
  private verifyFields(): boolean {
    const keys = this.gateway.userFieldKeys();
    if (!keys) return false; // catalog unreadable; gateway is inert anyway

    const missing = findMissingFields(keys);
    if (missing.length === 0) return true;

    const list = missing.map((f) => `• ${f.displayName} — key "${f.key}", type ${f.type}`).join("\n");
    new Notice(
      `TaskNotes EF Layer: ${missing.length} of ${EF_FIELDS.length} EF fields are missing.\n` +
        `Add them in Settings → Task Properties → "Add new user field":\n${list}\n` +
        `The EF layer stays inactive until they exist.`,
      0, // persist until dismissed
    );
    console.warn("[ef-layer] Missing EF user fields:", missing.map((f) => f.key));
    return false;
  }

  private async classifyOnCreate(path: string, task: Parameters<typeof toTaskDTO>[0]): Promise<void> {
    const classification = classify(toTaskDTO(task));
    const patch = buildEfPatch(classification, new Date().toISOString());
    const outcome = await this.gateway.updateTask(path, patch, "classify on create");
    if (!outcome.ok) {
      console.warn("[ef-layer] classify-on-create failed:", path, outcome);
    }
  }
}
