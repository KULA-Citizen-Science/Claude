import { describe, it, expect, vi } from "vitest";
import type { App } from "obsidian";
import { TaskNotesGateway, MUTATION_SOURCE } from "./tasknotes-gateway";
import type { MutationContext, RuntimeEvent } from "./runtime-api";

/** Build a fake TaskNotes runtime API with controllable capabilities, and a
 *  fake App that exposes it via app.plugins.getPlugin("tasknotes"). */
function makeHarness(opts: {
  apiVersion?: number;
  capabilities?: string[];
  updateImpl?: (path: string, patch: Record<string, unknown>, ctx?: MutationContext) => unknown;
  present?: boolean;
  userFields?: unknown[];
}) {
  const capabilities = new Set(opts.capabilities ?? []);
  let createdHandler: ((e: RuntimeEvent) => void) | undefined;

  const api = {
    apiVersion: opts.apiVersion ?? 1,
    hasCapability: (c: string) => capabilities.has(c),
    lifecycle: { ready: vi.fn().mockResolvedValue(undefined) },
    events: {
      on: (name: string, handler: (e: RuntimeEvent) => void) => {
        if (name === "task.created") createdHandler = handler;
        return { name } as unknown;
      },
      off: vi.fn(),
    },
    tasks: {
      get: vi.fn(),
      list: vi.fn().mockResolvedValue([]),
      update: vi.fn(async (path: string, patch: Record<string, unknown>, ctx?: MutationContext) =>
        opts.updateImpl ? opts.updateImpl(path, patch, ctx) : { path, ...patch },
      ),
    },
    catalog: {
      userFields: () => opts.userFields ?? [{ key: "ef_primary" }, { key: "ef_load" }],
      statuses: () => [{ value: "done", isCompleted: true }],
    },
    query: { tasks: vi.fn().mockResolvedValue({ tasks: [], matched: 0, total: 0, returned: 0 }) },
    errors: {
      toResult: async <T>(fn: () => Promise<T> | T) => {
        try {
          return { ok: true as const, value: await fn() };
        } catch (e) {
          return { ok: false as const, error: { name: "TaskNotesApiError", code: "operation_failed", message: String(e) } };
        }
      },
      normalize: (e: unknown) => ({ name: "TaskNotesApiError", code: "operation_failed", message: String(e) }),
    },
  };

  const app = {
    plugins: { getPlugin: (id: string) => (opts.present === false ? null : id === "tasknotes" ? { api } : null) },
  } as unknown as App;

  return { app, api, emitCreated: (e: RuntimeEvent) => createdHandler?.(e) };
}

describe("version + presence gating", () => {
  it("resolve() is null when TaskNotes is absent", () => {
    const { app } = makeHarness({ present: false });
    expect(new TaskNotesGateway(app).resolve()).toBeNull();
  });
  it("resolve() is null for a mismatched apiVersion", () => {
    const { app } = makeHarness({ apiVersion: 2, capabilities: ["tasks.write"] });
    expect(new TaskNotesGateway(app).resolve()).toBeNull();
  });
  it("resolve() returns the api at version 1", () => {
    const { app } = makeHarness({ apiVersion: 1 });
    expect(new TaskNotesGateway(app).isAvailable()).toBe(true);
  });
});

describe("updateTask", () => {
  it("refuses without the tasks.write capability", async () => {
    const { app, api } = makeHarness({ capabilities: [] });
    const out = await new TaskNotesGateway(app).updateTask("Tasks/x.md", { ef_load: 2 }, "why");
    expect(out).toEqual({ ok: false, reason: "no-capability" });
    expect(api.tasks.update).not.toHaveBeenCalled();
  });

  it("stamps the ef-layer mutation context on the write", async () => {
    const { app, api } = makeHarness({ capabilities: ["tasks.write"] });
    const out = await new TaskNotesGateway(app).updateTask("Tasks/x.md", { ef_load: 3 }, "classify on create");
    expect(out.ok).toBe(true);
    const ctx = api.tasks.update.mock.calls[0][2] as MutationContext;
    expect(ctx.source).toBe(MUTATION_SOURCE);
    expect(ctx.reason).toBe("classify on create");
    expect(typeof ctx.correlationId).toBe("string");
    expect(ctx.correlationId.length).toBeGreaterThan(0);
  });

  it("surfaces a TaskNotesApiError as a typed failure", async () => {
    const { app } = makeHarness({
      capabilities: ["tasks.write"],
      updateImpl: () => {
        throw new Error("boom");
      },
    });
    const out = await new TaskNotesGateway(app).updateTask("Tasks/x.md", {}, "r");
    expect(out.ok).toBe(false);
    if (!out.ok) expect(out.reason).toBe("error");
  });
});

describe("onTaskCreated self-write filtering", () => {
  it("ignores events that originate from ef-layer", () => {
    const { app, emitCreated } = makeHarness({ capabilities: ["tasks.events"] });
    const handler = vi.fn();
    new TaskNotesGateway(app).onTaskCreated(handler);

    emitCreated({ event: "task.created", source: MUTATION_SOURCE, task: { path: "Tasks/self.md" } });
    expect(handler).not.toHaveBeenCalled();

    emitCreated({ event: "task.created", source: "user", task: { path: "Tasks/other.md" } });
    expect(handler).toHaveBeenCalledTimes(1);
    expect(handler.mock.calls[0][0]).toEqual({ path: "Tasks/other.md" });
  });

  it("returns null without the tasks.events capability", () => {
    const { app } = makeHarness({ capabilities: [] });
    expect(new TaskNotesGateway(app).onTaskCreated(vi.fn())).toBeNull();
  });
});

describe("userFieldKeys", () => {
  it("returns the configured keys when catalog is readable", () => {
    const { app } = makeHarness({ capabilities: ["catalog.read"] });
    expect(new TaskNotesGateway(app).userFieldKeys()).toEqual(new Set(["ef_primary", "ef_load"]));
  });
  it("is null without the catalog.read capability", () => {
    const { app } = makeHarness({ capabilities: [] });
    expect(new TaskNotesGateway(app).userFieldKeys()).toBeNull();
  });

  it("detects keys regardless of which property the runtime uses (key/id/property)", () => {
    const { app } = makeHarness({
      capabilities: ["catalog.read"],
      userFields: [
        { key: "ef_primary" }, // key
        { id: "ef_load" }, // id only
        { property: "ef_social" }, // property only
        { displayName: "EF Effort", propertyName: "ef_effort" }, // propertyName
      ],
    });
    const keys = new TaskNotesGateway(app).userFieldKeys();
    expect(keys).not.toBeNull();
    for (const k of ["ef_primary", "ef_load", "ef_social", "ef_effort"]) {
      expect(keys?.has(k)).toBe(true);
    }
  });
});

describe("whenReady", () => {
  it("returns false quickly when TaskNotes never appears", async () => {
    const { app } = makeHarness({ present: false });
    expect(await new TaskNotesGateway(app).whenReady(30, 10)).toBe(false);
  });
  it("awaits lifecycle.ready when present", async () => {
    const { app, api } = makeHarness({ apiVersion: 1 });
    expect(await new TaskNotesGateway(app).whenReady(1000, 10)).toBe(true);
    expect(api.lifecycle.ready).toHaveBeenCalled();
  });
});
