// Port of FrictionEngineTest + CatalogIntegrityTest + SceneArtTest (core/src/test/...).
// Run (repo root): node --test "web/test/*.test.mjs"   (or: cd web && npm test)
import test from "node:test";
import assert from "node:assert/strict";
import {
  Archetypes, FrictionCatalog, FrictionEngine, Frameworks, EVIDENCE, activity, customActivity,
} from "../js/engine.js";
import { TRAPS, EMBLEMS } from "../js/art.js";

const arch = (id) => Archetypes.find((a) => a.id === id).activity;

test("every activity yields at least one friction point", () => {
  assert.ok(FrictionEngine.analyze(activity({ name: "Nothing special" })).length > 0);
});

test("analysis is deterministic", () => {
  const a = arch("big_project");
  assert.deepEqual(FrictionEngine.analyze(a).map((p) => p.id), FrictionEngine.analyze(a).map((p) => p.id));
});

test("ranking is sorted by score, descending", () => {
  const a = arch("big_project");
  const scores = FrictionEngine.analyze(a).map((p) => p.score(a));
  assert.deepEqual(scores, [...scores].sort((x, y) => y - x));
});

test("only relevant points appear", () => {
  const easy = activity({ name: "Fun quick thing", interest: "ENGAGING", deadline: "SOFT", duration: "QUICK" });
  const ids = FrictionEngine.analyze(easy).map((p) => p.id);
  for (const absent of ["beingLate", "transition", "waitGap", "dread"]) assert.ok(!ids.includes(absent), absent);
});

const tops = [
  ["boring_admin", "dread", "WALL_OF_AWFUL"],
  ["the_appointment", "beingLate", "TIME_BLINDNESS"],
  ["big_project", "noFinishLine", null],
  ["chore", "initiation", "ACTIVATION_ENERGY"],
  ["the_errand", "transition", null],
  ["wind_down", "timeEstimation", "TIME_BLINDNESS"],
];
for (const [id, expectId, expectFw] of tops) {
  test(`${id} tops at ${expectId}`, () => {
    const top = FrictionEngine.topFor(arch(id));
    assert.equal(top.id, expectId);
    if (expectFw) assert.equal(top.framework, expectFw);
  });
}

test("property-driven points surface", () => {
  const ids = (a) => FrictionEngine.analyze(a).map((p) => p.id);
  assert.ok(ids(activity({ multiStep: true })).includes("workingMemory"));
  assert.ok(ids(activity({ load: "HEAVY" })).includes("cognitiveLoad"));
  assert.ok(ids(activity({ structure: "OPEN" })).includes("defineDone"));
});

test("catalog: copy is non-empty, unique ids, and short on the surface", () => {
  const ids = FrictionCatalog.map((p) => p.id);
  assert.equal(new Set(ids).size, ids.length);
  assert.equal(FrictionCatalog.length, 13);
  for (const p of FrictionCatalog) {
    assert.ok(p.trap && p.strategy && p.why, p.id);
    assert.ok(p.trap.length <= 60, `trap too long: ${p.id}`);
    assert.ok(p.strategy.length <= 80, `strategy too long: ${p.id}`);
    assert.ok(Frameworks[p.framework], `unknown framework for ${p.id}`);
  }
});

test("frameworks have tag, tier and source", () => {
  for (const [k, f] of Object.entries(Frameworks)) {
    assert.ok(f.tag && f.source, k);
    assert.ok(EVIDENCE[f.tier], k);
  }
});

test("archetype ids are unique and each produces a reading", () => {
  const ids = Archetypes.map((a) => a.id);
  assert.equal(new Set(ids).size, ids.length);
  assert.equal(Archetypes.length, 7);
  for (const a of Archetypes) assert.ok(FrictionEngine.analyze(a.activity).length > 0, a.id);
});

test("custom quest: dread toggle surfaces the Wall of Awful first", () => {
  assert.equal(FrictionEngine.topFor(customActivity("0000001")).id, "dread");
});

// ---- art

const PALETTE = ".KWDGCMLPS";
test("art covers every friction point with well-formed sprites", () => {
  const ids = new Set(FrictionCatalog.map((p) => p.id));
  assert.deepEqual(new Set(Object.keys(TRAPS)), ids);
  assert.deepEqual(new Set(Object.keys(EMBLEMS)), ids);
  for (const [id, rows] of [...Object.entries(TRAPS), ...Object.entries(EMBLEMS)]) {
    assert.ok(rows.length > 0 && rows[0].length > 0, id);
    for (const row of rows) {
      assert.equal(row.length, rows[0].length, `${id}: ragged row`);
      for (const ch of row) assert.ok(PALETTE.includes(ch), `${id}: bad char ${ch}`);
    }
    assert.ok(rows.some((r) => /[^.]/.test(r)), `${id}: blank sprite`);
  }
});
