// "Start something" — the one-decision picker. Opens a modal that shows a single
// easiest-to-start task, why it was picked, and a concrete first move, with
// re-roll and size filters. This is an interactive command surface, not a task
// *view* (no list/kanban/calendar) — it doesn't duplicate TaskNotes' views.

import { App, Modal, Notice, Plugin, setIcon } from "obsidian";
import { rankCandidates, firstStep, type Candidate, type TimeCap } from "./select";
import { toPickerTask } from "./map";
import type { TaskNotesGateway } from "../gateway/tasknotes-gateway";

// Status a task moves to when you commit to starting it. Configurable later;
// TaskNotes' default includes "in-progress".
const IN_PROGRESS_STATUS = "in-progress";

const EFFORT_LABEL: Record<number, string> = { 1: "very easy", 2: "easy", 3: "moderate", 4: "heavy" };
const SIZE_LABEL: Record<Candidate["durationLabel"], string | null> = {
  quick: "quick",
  medium: "medium",
  long: "long",
  unsized: null,
};
const URGENCY_LABEL: Record<Candidate["urgency"], string | null> = {
  overdue: "⚠ overdue",
  soon: "due soon",
  none: null,
};

const STYLE_ID = "ef-start-styles";
const STYLES = `
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

const TIME_CAPS: Array<{ cap: TimeCap; label: string }> = [
  { cap: "5m", label: "5 min" },
  { cap: "25m", label: "25 min" },
  { cap: "more", label: "more" },
];

export class StartSomethingModal extends Modal {
  private readonly gateway: TaskNotesGateway;
  private timeCap: TimeCap = "more";
  private readonly exclude = new Set<string>();
  private ranked: Candidate[] = [];
  private completed = new Set<string>(["done"]);

  constructor(app: App, gateway: TaskNotesGateway) {
    super(app);
    this.gateway = gateway;
  }

  async onOpen(): Promise<void> {
    injectStyles();
    this.titleEl.setText("Start something");
    this.modalEl.addClass("ef-start-modal");

    if (!this.gateway.isAvailable()) {
      this.renderMessage("TaskNotes isn't ready — can't pick a task right now.");
      return;
    }
    await this.reload();
  }

  onClose(): void {
    this.contentEl.empty();
  }

  /** Refetch tasks and re-rank (used on open and when the size filter changes). */
  private async reload(): Promise<void> {
    this.renderMessage("Finding the easiest thing to start…");
    this.completed = this.gateway.completedStatuses();
    const tasks = (await this.gateway.listTasks()).map(toPickerTask);
    this.ranked = rankCandidates(tasks, {
      timeCap: this.timeCap,
      completedStatuses: this.completed,
      exclude: this.exclude,
    });
    this.render();
  }

  private render(): void {
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

  private renderFilters(parent: HTMLElement): void {
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

  private renderPick(parent: HTMLElement, pick: Candidate): void {
    const title = parent.createDiv({ cls: "ef-pick-title", text: pick.title || "(untitled task)" });
    title.addEventListener("click", () => this.openTask(pick.path));

    const chips = parent.createDiv({ cls: "ef-chips" });
    const labels = [
      EFFORT_LABEL[pick.effort],
      SIZE_LABEL[pick.durationLabel],
      URGENCY_LABEL[pick.urgency],
      pick.primary,
    ].filter((x): x is string => Boolean(x));
    for (const l of labels) chips.createSpan({ cls: "ef-chip", text: l });

    const step = parent.createDiv({ cls: "ef-firststep" });
    step.createDiv({ cls: "ef-firststep-label", text: "First move" });
    const stepText = step.createDiv({ cls: "ef-firststep-text", text: firstStep(pick.primary) });
    // Upgrade to the first real subtask if there is one and we're still on this pick.
    void this.gateway.firstIncompleteSubtask(pick.path).then((sub) => {
      if (sub && this.ranked[0]?.path === pick.path) stepText.setText(firstStep(pick.primary, sub));
    });

    const actions = parent.createDiv({ cls: "ef-actions" });
    this.button(actions, "play", "Start", "cta", () => this.startTask(pick.path));
    this.button(actions, "file", "Open", "", () => this.openTask(pick.path));
    this.button(actions, "dice", "Show another", "", () => this.showAnother(pick.path));
  }

  private renderEmpty(parent: HTMLElement): void {
    const msg =
      this.timeCap === "more"
        ? "Nothing to start right now — everything's done, blocked, or parked for later. 🎉"
        : "Nothing that small right now. Try a longer window.";
    parent.createDiv({ cls: "ef-msg", text: msg });
  }

  private renderMessage(text: string): void {
    const { contentEl } = this;
    contentEl.empty();
    contentEl.createDiv({ cls: "ef-msg", text });
  }

  private button(
    parent: HTMLElement,
    icon: string,
    label: string,
    cls: string,
    onClick: () => void,
  ): void {
    const btn = parent.createEl("button", { cls });
    setIcon(btn.createSpan(), icon);
    btn.createSpan({ text: ` ${label}` });
    btn.addEventListener("click", onClick);
  }

  private showAnother(currentPath: string): void {
    this.exclude.add(currentPath);
    this.ranked = this.ranked.filter((c) => !this.exclude.has(c.path));
    this.render();
  }

  private openTask(path: string): void {
    void this.app.workspace.openLinkText(path, "", false);
    this.close();
  }

  private async startTask(path: string): Promise<void> {
    const outcome = await this.gateway.setStatus(path, IN_PROGRESS_STATUS, "started from picker");
    if (!outcome.ok) new Notice("Couldn't mark it in-progress — opening it anyway.");
    this.openTask(path);
  }
}

function injectStyles(): void {
  if (document.getElementById(STYLE_ID)) return;
  const el = document.createElement("style");
  el.id = STYLE_ID;
  el.textContent = STYLES;
  document.head.appendChild(el);
}

export function registerStartCommand(plugin: Plugin, gateway: TaskNotesGateway): void {
  plugin.addCommand({
    id: "start-something",
    name: "EF: Start something",
    callback: () => new StartSomethingModal(plugin.app, gateway).open(),
  });
  plugin.addRibbonIcon("play", "EF: Start something", () =>
    new StartSomethingModal(plugin.app, gateway).open(),
  );
}
