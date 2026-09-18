#!/usr/bin/env node
// Local installer for the TaskNotes EF Layer plugin.
//
//   node install.mjs /path/to/YourVault
//
// Copies manifest.json + main.js into <vault>/.obsidian/plugins/tasknotes-ef/
// and the triage view into <vault>/TaskNotes/Views/ef-triage.base (never
// overwriting an existing view). It does NOT touch TaskNotes' own settings —
// the seven ef_* user fields are created via TaskNotes' UI, or with a config
// snippet generated separately. Run again after pulling changes to update.

import { copyFile, mkdir, access } from "node:fs/promises";
import { constants } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));

async function exists(p) {
  try {
    await access(p, constants.F_OK);
    return true;
  } catch {
    return false;
  }
}

async function main() {
  const vault = process.argv[2];
  if (!vault) {
    console.error("Usage: node install.mjs /path/to/YourVault");
    process.exit(1);
  }
  if (!(await exists(join(vault, ".obsidian")))) {
    console.error(`Not an Obsidian vault (no .obsidian folder): ${vault}`);
    process.exit(1);
  }

  // 1) Plugin files
  const pluginDir = join(vault, ".obsidian", "plugins", "tasknotes-ef");
  await mkdir(pluginDir, { recursive: true });
  for (const file of ["manifest.json", "main.js"]) {
    await copyFile(join(here, file), join(pluginDir, file));
    console.log(`✓ ${join("plugins", "tasknotes-ef", file)}`);
  }

  // 2) Triage view (do not clobber a customized one)
  const viewsDir = join(vault, "TaskNotes", "Views");
  await mkdir(viewsDir, { recursive: true });
  const viewDest = join(viewsDir, "ef-triage.base");
  if (await exists(viewDest)) {
    console.log("• TaskNotes/Views/ef-triage.base already exists — left as-is");
  } else {
    await copyFile(join(here, "views", "ef-triage.base"), viewDest);
    console.log("✓ TaskNotes/Views/ef-triage.base");
  }

  console.log(
    [
      "",
      "Done. In Obsidian:",
      "  1. Settings → Community plugins → Reload plugins → enable “TaskNotes EF Layer”.",
      "  2. Enable the Bases core plugin (Settings → Core plugins) to use the view.",
      "  3. Create the seven ef_* fields (Settings → Task Properties) — see README.md.",
      "     Until they exist the plugin shows a setup notice and writes nothing.",
      "  4. Smoke test: command palette → “EF: reclassify vault (dry run)”, check the console.",
    ].join("\n"),
  );
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
