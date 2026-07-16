// The ONLY module besides main.ts that touches Obsidian / TaskNotes. Every hard
// constraint is enforced here so the rest of the plugin can't violate them:
//   - apiVersion === 1 + hasCapability() gate on every entry point
//   - graceful handling when TaskNotes is absent / not-yet-loaded, retried
//     after lifecycle.ready()
//   - mutation context { source: "ef-layer", correlationId, reason } on writes
//   - inbound events with source === "ef-layer" ignored (no self-trigger loop)
//   - TaskNotesApiError handled via api.errors.toResult

import type { App, EventRef } from "obsidian";
import type {
  ApiResult,
  RuntimeApiV1,
  RuntimeEvent,
  MutationContext,
  TaskNotesPlugin,
  TaskNotesTask,
  UserFieldDef,
} from "./runtime-api";

/** The source tag stamped on our writes and used to ignore our own events. */
export const MUTATION_SOURCE = "ef-layer";

interface PluginsRegistry {
  getPlugin(id: string): TaskNotesPlugin | null;
}

export type WriteOutcome =
  | { ok: true; task: TaskNotesTask }
  | { ok: false; reason: "no-capability" | "error"; code?: string; message?: string };

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function randomId(): string {
  const c = (globalThis as { crypto?: { randomUUID?: () => string } }).crypto;
  return c?.randomUUID ? c.randomUUID() : `ef-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export class TaskNotesGateway {
  constructor(private readonly app: App) {}

  private plugin(): TaskNotesPlugin | null {
    // `app.plugins` is not in Obsidian's public typings; access it structurally.
    const plugins = (this.app as unknown as { plugins?: PluginsRegistry }).plugins;
    return plugins?.getPlugin("tasknotes") ?? null;
  }

  /** Resolve the runtime API with the version gate applied. Null when TaskNotes
   *  is missing, not yet loaded, or a different major API version. */
  resolve(): RuntimeApiV1 | null {
    const api = this.plugin()?.api;
    if (!api || api.apiVersion !== 1) return null;
    return api;
  }

  isAvailable(): boolean {
    return this.resolve() !== null;
  }

  /** Resolve and require a capability in one step. */
  private apiWith(capability: string): RuntimeApiV1 | null {
    const api = this.resolve();
    if (!api || !api.hasCapability(capability)) return null;
    return api;
  }

  /**
   * Wait until TaskNotes is present and its runtime reports ready. Handles the
   * common case where TaskNotes loads after us, by polling for the API up to
   * `timeoutMs`, then awaiting `lifecycle.ready()`. Returns false on timeout.
   */
  async whenReady(timeoutMs = 15000, pollMs = 250): Promise<boolean> {
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

  private context(reason: string): MutationContext {
    return { source: MUTATION_SOURCE, correlationId: randomId(), reason };
  }

  /** The configured user-field property keys, or null if the catalog is
   *  unreadable (TaskNotes absent / capability missing). */
  userFieldKeys(): Set<string> | null {
    const api = this.apiWith("catalog.read");
    if (!api) return null;
    let defs: UserFieldDef[];
    try {
      defs = api.catalog.userFields() ?? [];
    } catch {
      return null;
    }
    return new Set(defs.map((f) => f.key).filter((k): k is string => Boolean(k)));
  }

  /**
   * Subscribe to `task.created`, skipping events caused by our own writes.
   * Returns an EventRef to hand to Plugin.registerEvent, or null if the events
   * capability is unavailable.
   */
  onTaskCreated(handler: (task: TaskNotesTask, event: RuntimeEvent) => void): EventRef | null {
    const api = this.apiWith("tasks.events");
    if (!api) return null;
    return api.events.on("task.created", (event: RuntimeEvent) => {
      if (event.source === MUTATION_SOURCE) return; // never react to our own writes
      const task = event.task ?? event.after;
      if (task) handler(task, event);
    });
  }

  /** Write an EF patch through the update service, with mutation context and
   *  typed-error handling. */
  async updateTask(
    path: string,
    patch: Record<string, unknown>,
    reason: string,
  ): Promise<WriteOutcome> {
    const api = this.apiWith("tasks.write");
    if (!api) return { ok: false, reason: "no-capability" };

    const result: ApiResult<TaskNotesTask> = await api.errors.toResult(() =>
      api.tasks.update(path, patch, this.context(reason)),
    );
    if (result.ok) return { ok: true, task: result.value };
    return { ok: false, reason: "error", code: result.error.code, message: result.error.message };
  }

  /** All tasks in scope, preferring the stable query API and falling back to
   *  tasks.list(). Returns [] when nothing is readable. */
  async listTasks(): Promise<TaskNotesTask[]> {
    const q = this.apiWith("query.tasks");
    if (q) {
      try {
        const res = await q.query.tasks({ scope: { includeArchived: false } });
        return res.tasks ?? [];
      } catch {
        // fall through to tasks.list
      }
    }
    const api = this.apiWith("tasks.read");
    if (!api) return [];
    try {
      return (await api.tasks.list()) ?? [];
    } catch {
      return [];
    }
  }
}
