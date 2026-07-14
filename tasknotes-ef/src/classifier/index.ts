// Public surface of the pure classifier. The rest of the plugin imports only
// from here, keeping the runtime-free boundary explicit.

export * from "./types";
export { deriveSignals, bucketDuration, bucketTimePressure, isRoutine } from "./signals";
export {
  classify,
  scoreCategories,
  pickPrimarySecondary,
  computeSocial,
  computeLoad,
  computeEffort,
  computeConfidence,
} from "./classify";
