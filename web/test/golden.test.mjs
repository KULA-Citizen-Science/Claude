// Golden rankings: the web engine's ordered friction ids for every archetype and every one of the
// 128 "Other…" toggle combinations. core/.../WebParityTest.kt checks the Kotlin engine against the
// same file, so the two implementations can't drift apart silently.
//   node web/test/golden.test.mjs --write   regenerate golden.txt (after a deliberate engine change)
//   node --test "web/test/*.test.mjs"              verifies the committed file still matches
import { writeFileSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import test from "node:test";
import assert from "node:assert/strict";
import { Archetypes, FrictionEngine, customActivity } from "../js/engine.js";

const FILE = fileURLToPath(new URL("./golden.txt", import.meta.url));

export function build() {
  const lines = [];
  for (const a of Archetypes) {
    lines.push(`${a.id}: ${FrictionEngine.analyze(a.activity).map((p) => p.id).join(",")}`);
  }
  for (let n = 0; n < 128; n++) {
    const bits = n.toString(2).padStart(7, "0");
    lines.push(`custom/${bits}: ${FrictionEngine.analyze(customActivity(bits)).map((p) => p.id).join(",")}`);
  }
  return lines.join("\n") + "\n";
}

if (process.argv.includes("--write")) {
  writeFileSync(FILE, build());
  console.log("wrote", FILE);
} else {
  test("golden.txt matches the current engine", () => {
    assert.equal(readFileSync(FILE, "utf8"), build());
  });
}
