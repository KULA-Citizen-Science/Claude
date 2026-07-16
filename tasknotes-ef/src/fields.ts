// The seven ef_* user-field specs — the shared source of truth for what the
// plugin verifies (M1.1) and what it writes (M1.3/M1.4). Pure: no obsidian, no
// TaskNotes imports beyond the classifier's output type.

import type { EFClassification } from "./classifier";

export interface EFFieldSpec {
  key: string; // frontmatter property name, as configured in TaskNotes
  displayName: string;
  type: "Text" | "Number" | "List";
  description: string;
}

/** The seven fields the user must create in Settings → Task Properties. The
 *  plugin never creates them; it only verifies and writes. */
export const EF_FIELDS: readonly EFFieldSpec[] = [
  { key: "ef_primary", displayName: "EF Primary", type: "Text", description: "Dominant EF barrier to starting" },
  { key: "ef_secondary", displayName: "EF Secondary", type: "List", description: "Other EF barriers present" },
  { key: "ef_load", displayName: "EF Load", type: "Number", description: "Working-memory / cognitive load (1-3)" },
  { key: "ef_social", displayName: "EF Social", type: "Text", description: "Social demand: solo | async | live" },
  { key: "ef_effort", displayName: "EF Effort", type: "Number", description: "Activation energy to begin (1-4)" },
  { key: "ef_confidence", displayName: "EF Confidence", type: "Number", description: "Classifier confidence (0-1)" },
  { key: "ef_classified_at", displayName: "EF Classified At", type: "Text", description: "Provenance stamp (ISO); presence = classified" },
] as const;

/** The provenance field. Presence of a value here means "already classified". */
export const EF_CLASSIFIED_AT_KEY = "ef_classified_at";

export const EF_FIELD_KEYS: readonly string[] = EF_FIELDS.map((f) => f.key);

/** Which of the required fields are missing from the configured set. */
export function findMissingFields(present: Set<string>): EFFieldSpec[] {
  return EF_FIELDS.filter((f) => !present.has(f.key));
}

/** Build the frontmatter patch written via `api.tasks.update`, stamping the
 *  provenance time so the task counts as classified afterwards. */
export function buildEfPatch(
  classification: EFClassification,
  classifiedAt: string,
): Record<string, unknown> {
  return {
    ef_primary: classification.ef_primary,
    ef_secondary: classification.ef_secondary,
    ef_load: classification.ef_load,
    ef_social: classification.ef_social,
    ef_effort: classification.ef_effort,
    ef_confidence: classification.ef_confidence,
    [EF_CLASSIFIED_AT_KEY]: classifiedAt,
  };
}
