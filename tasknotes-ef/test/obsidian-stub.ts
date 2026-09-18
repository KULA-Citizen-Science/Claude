// Minimal `obsidian` stub for unit tests. The real module is provided by the
// Obsidian app at runtime and has no importable JS entry point, so test files
// that transitively import value symbols (e.g. `Notice`) alias to this instead.
// Only the surface our pure/planning code touches needs to exist here.

export class Notice {
  constructor(_message?: string, _timeout?: number) {}
  setMessage(_message: string): this {
    return this;
  }
  hide(): void {}
}

export class Plugin {
  addCommand(_cmd: unknown): void {}
  registerEvent(_ref: unknown): void {}
}

export class Component {}
export class TFile {}
export class App {}
