// Verifies a staged site directory is complete: every file the service worker precaches and every
// icon the manifest references must exist. A missing precache entry makes cache.addAll() reject,
// which silently breaks offline install — better to fail the deploy.
// Usage: node web/tools/check-precache.mjs <site-dir>
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

const dir = process.argv[2];
if (!dir) {
  console.error("usage: check-precache.mjs <site-dir>");
  process.exit(2);
}

const sw = readFileSync(join(dir, "sw.js"), "utf8");
const list = sw.match(/const FILES = \[([\s\S]*?)\];/);
if (!list) {
  console.error("could not find the FILES list in sw.js");
  process.exit(1);
}
const precache = [...list[1].matchAll(/"([^"]+)"/g)].map((m) => m[1]);

const manifest = JSON.parse(readFileSync(join(dir, "manifest.webmanifest"), "utf8"));
const icons = manifest.icons.map((i) => i.src);

const missing = [...new Set([...precache, ...icons])].filter(
  (f) => f !== "./" && !existsSync(join(dir, f)),
);
if (missing.length) {
  console.error("missing from site:", missing.join(", "));
  process.exit(1);
}
console.log(`ok: ${precache.length} precached files and ${icons.length} manifest icons present`);
