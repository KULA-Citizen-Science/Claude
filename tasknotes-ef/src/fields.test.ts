import { describe, it, expect } from "vitest";
import { EF_FIELDS, EF_FIELD_KEYS, findMissingFields, buildEfPatch } from "./fields";

describe("EF_FIELDS", () => {
  it("declares exactly seven fields with unique keys", () => {
    expect(EF_FIELDS).toHaveLength(7);
    expect(new Set(EF_FIELD_KEYS).size).toBe(7);
  });
});

describe("findMissingFields", () => {
  it("returns nothing when all present", () => {
    expect(findMissingFields(new Set(EF_FIELD_KEYS))).toEqual([]);
  });
  it("lists only the absent fields", () => {
    const present = new Set(EF_FIELD_KEYS.filter((k) => k !== "ef_social" && k !== "ef_load"));
    const missing = findMissingFields(present).map((f) => f.key);
    expect(missing.sort()).toEqual(["ef_load", "ef_social"]);
  });
});

describe("buildEfPatch", () => {
  it("maps a classification plus provenance stamp to a frontmatter patch", () => {
    const patch = buildEfPatch(
      { ef_primary: "focus", ef_secondary: ["planning"], ef_load: 2, ef_social: "solo", ef_effort: 3, ef_confidence: 0.7 },
      "2026-07-14T12:00:00.000Z",
    );
    expect(patch).toEqual({
      ef_primary: "focus",
      ef_secondary: ["planning"],
      ef_load: 2,
      ef_social: "solo",
      ef_effort: 3,
      ef_confidence: 0.7,
      ef_classified_at: "2026-07-14T12:00:00.000Z",
    });
    // every stored key is a declared field
    for (const key of Object.keys(patch)) expect(EF_FIELD_KEYS).toContain(key);
  });
});
