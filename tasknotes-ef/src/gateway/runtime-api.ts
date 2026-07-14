// Minimal structural contract for the subset of the TaskNotes runtime API this
// plugin uses. The real contract ships as `tasknotes/src/api/runtime-api.ts`
// (intended to become `@tasknotes/runtime-api`); we declare only what we call so
// we don't take a build dependency on TaskNotes. Matched structurally at runtime.

import type { EventRef } from "obsidian";

export interface MutationContext {
  source: string;
  correlationId: string;
  reason: string;
}

/** A configured user-defined field, from `api.catalog.userFields()`. */
export interface UserFieldDef {
  key: string; // frontmatter property name
  id?: string;
  displayName?: string;
  type?: string;
}

/** A TaskNotes task. User fields surface as top-level frontmatter keys, hence
 *  the index signature. We only read a handful of core fields. */
export interface TaskNotesTask {
  path: string;
  title?: string;
  status?: string;
  tags?: string[];
  contexts?: string[];
  due?: string | null;
  scheduled?: string | null;
  timeEstimate?: number | null;
  recurrence?: string | null;
  [key: string]: unknown;
}

/** Normalized runtime event payload (see docs "Events"). */
export interface RuntimeEvent {
  event: string;
  taskPath?: string;
  task?: TaskNotesTask;
  before?: TaskNotesTask;
  after?: TaskNotesTask;
  source?: string;
  correlationId?: string;
  reason?: string;
}

export interface TaskNotesApiErrorShape {
  name: string;
  code: string;
  message: string;
  status?: number;
  details?: unknown;
}

export type ApiResult<T> =
  | { ok: true; value: T }
  | { ok: false; error: TaskNotesApiErrorShape };

export interface QueryResult {
  tasks: TaskNotesTask[];
  matched: number;
  total: number;
  returned: number;
}

export interface RuntimeApiV1 {
  apiVersion: number;
  hasCapability(capability: string): boolean;

  lifecycle: {
    ready(): Promise<void>;
  };
  events: {
    on(name: string, handler: (event: RuntimeEvent) => void): EventRef;
    off(ref: EventRef): void;
  };
  tasks: {
    get(path: string): Promise<TaskNotesTask | null>;
    list(query?: unknown): Promise<TaskNotesTask[]>;
    update(
      path: string,
      patch: Record<string, unknown>,
      context?: MutationContext,
    ): Promise<TaskNotesTask>;
  };
  catalog: {
    userFields(): UserFieldDef[];
  };
  query: {
    tasks(query: unknown): Promise<QueryResult>;
  };
  errors: {
    toResult<T>(fn: () => Promise<T> | T): Promise<ApiResult<T>>;
    normalize(err: unknown): TaskNotesApiErrorShape;
  };
}

export interface TaskNotesPlugin {
  api?: RuntimeApiV1;
}
