// NextQuest engine — a direct port of core/src/main/kotlin/com/kula/nextquest/domain/*.
// Keep in sync with the Kotlin source: web/test/golden.txt locks the two implementations
// together (see web/test/golden.mjs and core/.../WebParityTest.kt).
//
// These describe established models of ADHD / executive function. Decision-support
// scaffolding, not clinical advice. Evidence write-up: docs/RESEARCH.md.

/** Evidence tiers, shown as a one-character marker beside each framework tag. */
export const EVIDENCE = {
  PEER_REVIEWED: { marker: "✓", label: "peer-reviewed" },
  CLINICAL_HEURISTIC: { marker: "~", label: "heuristic" },
};

const P = "PEER_REVIEWED";
const H = "CLINICAL_HEURISTIC";

export const Frameworks = {
  TIME_BLINDNESS: {
    tag: "Time blindness", tier: P,
    source: "Barkley (1997; 2012) — behaviour falls under 'the temporal now', a.k.a. temporal myopia. Timing deficits meta-analysed in Marx et al. (2022, JAACAP) and Zheng et al. (2022, J. Attention Disorders): estimates run short, an accelerated internal clock.",
  },
  ACTIVATION_ENERGY: {
    tag: "Activation", tier: P,
    source: "Brown's 'Activation' cluster (getting started/organising/prioritising) + Sonuga-Barke's dual/triple-pathway model (2002–2010): executive dysfunction and delay aversion make initiation a distinct, impaired step. 'Activation energy' framing is coaching shorthand.",
  },
  WORKING_MEMORY: {
    tag: "Working memory", tier: P,
    source: "Barkley — weak nonverbal working memory; holding a multi-step sequence in mind fails. Safren's CBT (2010, JAMA, RCT) 'Organising Multiple Tasks' module externalises it onto lists.",
  },
  PRIORITISATION: {
    tag: "Where to start", tier: P,
    source: "Brown's Activation (prioritising) + Safren's A/B/C priority ratings and 'Managing Overwhelming Tasks' + GTD's single next physical action: collapse the open task to one concrete start.",
  },
  DEFINE_DONE: {
    tag: "Define 'done'", tier: P,
    source: "Solanto's Meta-Cognitive Therapy (2010, Am. J. Psychiatry, RCT) + Safren — open/ambiguous tasks stall; the evidence-based move is a mandatory 'define the deliverable' pre-step before scheduling.",
  },
  SUSTAINED_EFFORT: {
    tag: "Sustained effort", tier: P,
    source: "Solanto lists 'avoidance of tasks requiring sustained mental effort' as a core target; Safren's 'break tasks to fit your attention span'. Effort/focus regulation (Brown; Barkley).",
  },
  COGNITIVE_LOAD: {
    tag: "Energy window", tier: H,
    source: "Occupational-therapy energy/task-demand matching (UK Adult ADHD Network consensus, BMC Psychiatry 2021) + Barkley's 'point of performance': schedule heavy load in your peak/medication window.",
  },
  TRANSITION_COST: {
    tag: "Transition cost", tier: H,
    source: "Set-shifting/task-switching difficulty in ADHD (EF literature) + OT task-demand analysis: leaving one context for another, especially the house, carries a large, underestimated start cost.",
  },
  INTEREST_DRIVEN: {
    tag: "Interest-driven", tier: H,
    source: "Dodson's interest-based nervous system / INCUP (Interest, Novelty, Challenge, Urgency, Passion) — a Psychiatric Times (2006) practitioner heuristic, not validated science, but a real motivational lever.",
  },
  NO_FINISH_LINE: {
    tag: "No finish line", tier: H,
    source: "INCUP 'urgency' (Dodson, heuristic) resting on peer-reviewed steeper temporal discounting in ADHD: with no endpoint/deadline there's no urgency signal, so the task never gains traction.",
  },
  WALL_OF_AWFUL: {
    tag: "Wall of Awful", tier: H,
    source: "Brendan Mahan's coaching metaphor (2016): accumulated emotional charge becomes the real barrier to starting. Overlaps peer-reviewed delay aversion (Sonuga-Barke).",
  },
  OBJECT_PERMANENCE: {
    tag: "Out of sight", tier: H,
    source: "Popular ADHD 'time/object permanence' framing, grounded in Barkley's externalisation principle: a plan not visibly present drops out of awareness during gaps and waits.",
  },
};

/** Build an activity with the same defaults as the Kotlin data class. */
export function activity(fields) {
  return {
    name: "",
    interest: "NEUTRAL",   // BORING | NEUTRAL | ENGAGING
    deadline: "NONE",      // HARD | SOFT | NONE
    structure: "DEFINED",  // DEFINED | OPEN
    duration: "MEDIUM",    // QUICK | MEDIUM | LONG_OR_UNKNOWN
    load: "LIGHT",         // LIGHT | HEAVY
    multiStep: false,
    requiresLeavingHome: false,
    hasDread: false,
    involvesWaiting: false,
    externallyImposed: false,
    ...fields,
  };
}

const durationScore = (a, long, medium) =>
  a.duration === "LONG_OR_UNKNOWN" ? long : a.duration === "MEDIUM" ? medium : 0;

/**
 * The trap taxonomy. Order is the tie-break order (roughly most-blocking → least).
 * `trap` / `strategy` are the two lines shown on the card; `why` stays inspectable.
 */
export const FrictionCatalog = [
  {
    id: "dread",
    trap: "The dread — not the task — is what's stopping you.",
    strategy: "Name the feeling out loud, then do one tiny, non-scary piece.",
    framework: "WALL_OF_AWFUL",
    why: "Emotional charge stacked on the task is the real barrier; shrinking to a non-threatening first move lets you climb over the wall instead of the whole task.",
    score: (a) => (a.hasDread ? 7 : 0),
  },
  {
    id: "beingLate",
    trap: "You'll start getting ready too late and run behind.",
    strategy: "Set the alarm for 'start getting ready', not 'leave'. Work backwards.",
    framework: "TIME_BLINDNESS",
    why: "With a fixed start you must leave for, time blindness makes the lead-up collapse; anchoring the alarm to preparation, not departure, externalises it.",
    score: (a) => (a.requiresLeavingHome ? 3 + (a.deadline === "HARD" ? 4 : 0) : 0),
  },
  {
    id: "waitGap",
    trap: "The wait will swallow the plan — you'll forget to resume.",
    strategy: "Right now, set an alarm for the far end of the wait.",
    framework: "OBJECT_PERMANENCE",
    why: "During an unstructured gap a plan that isn't visibly present drops out of awareness; an alarm makes the resume point exist again.",
    score: (a) => (a.involvesWaiting ? 5 : 0),
  },
  {
    id: "transition",
    trap: "Getting out the door is the hard part, not the errand.",
    strategy: "Pre-stage everything by the door now, while it's on your mind.",
    framework: "TRANSITION_COST",
    why: "Leaving one context for another carries a large, underestimated start cost; removing the friction in advance shrinks the switch.",
    score: (a) => (a.requiresLeavingHome ? 4 : 0),
  },
  {
    id: "cognitiveLoad",
    trap: "Heavy thinking on low fuel — you'll stall or half-do it.",
    strategy: "Book it for your peak/meds window; don't attempt it tired.",
    framework: "COGNITIVE_LOAD",
    why: "Task demand outruns available executive capacity when energy is low; matching heavy load to your best window is the highest-leverage scheduling move.",
    score: (a) => (a.load === "HEAVY" ? 4 : 0),
  },
  {
    id: "workingMemory",
    trap: "You'll lose the thread and drop a step.",
    strategy: "Dump the steps onto paper first; work the list, not your memory.",
    framework: "WORKING_MEMORY",
    why: "Holding a multi-step sequence 'in mind' overflows working memory; offloading it to a visible list removes the load.",
    score: (a) => (a.multiStep ? 5 : 0),
  },
  {
    id: "defineDone",
    trap: "You don't actually know what 'done' looks like yet.",
    strategy: "Before anything else, write one sentence defining 'done'.",
    framework: "DEFINE_DONE",
    why: "An open/ambiguous task has no clear target or first action, so it stalls; defining the deliverable is the evidence-based pre-step before scheduling.",
    score: (a) => (a.structure === "OPEN" ? 4 + (a.multiStep ? 1 : 0) : 0),
  },
  {
    id: "prioritisation",
    trap: "Too many entry points — you'll freeze on where to begin.",
    strategy: "Pick the single next physical action; ignore the rest for now.",
    framework: "PRIORITISATION",
    why: "An open multi-step task offers no obvious start, so activation stalls; collapsing it to one concrete next action gives a foothold.",
    score: (a) => (a.multiStep && a.structure === "OPEN" ? 4 : 0),
  },
  {
    id: "noFinishLine",
    trap: "No deadline means no urgency — it never gets traction.",
    strategy: "Put a real deadline on it and tell someone the deadline.",
    framework: "NO_FINISH_LINE",
    why: "Without urgency the interest-driven brain gets no start signal, and steeper temporal discounting buries distant payoffs; a near, witnessed deadline supplies it.",
    score: (a) => (a.deadline === "NONE" ? 4 + (a.duration === "LONG_OR_UNKNOWN" ? 2 : 0) : 0),
  },
  {
    id: "timeEstimation",
    trap: "It'll eat far more time than it feels like it will.",
    strategy: "Guess the minutes, add half again (×1.5), start a visible countdown.",
    framework: "TIME_BLINDNESS",
    why: "Timing deficits bias estimates short (planning fallacy, amplified in ADHD); a buffer multiplier plus a visible timer externalises time at the point of performance.",
    score: (a) => durationScore(a, 4, 1),
  },
  {
    id: "initiation",
    trap: "You won't cross from deciding to doing.",
    strategy: "Make an if-then: 'When I sit down, I start a 5-min timer on step one.'",
    framework: "ACTIVATION_ENERGY",
    why: "Starting is a distinct executive step that fails on its own; an implementation intention (Gollwitzer) plus a tiny timed first action lowers activation energy below the barrier.",
    score: (a) =>
      2 +
      (a.interest === "BORING" ? 2 : 0) +
      (a.externallyImposed ? 1 : 0) +
      (a.deadline === "NONE" ? 1 : 0),
  },
  {
    id: "boredom",
    trap: "Too dull to hold you — you'll bounce to something shinier.",
    strategy: "Bundle it with music or a podcast, or race the clock.",
    framework: "INTEREST_DRIVEN",
    why: "Low stimulation gives the interest-driven nervous system nothing to lock onto; adding novelty or a challenge supplies the missing pull.",
    score: (a) => (a.interest === "BORING" ? 4 + (a.externallyImposed ? 1 : 0) : 0),
  },
  {
    id: "attentionDrift",
    trap: "You'll drift off somewhere in the middle.",
    strategy: "Work one 25-minute block; park it when the timer rings.",
    framework: "SUSTAINED_EFFORT",
    why: "Sustaining effort as novelty fades is a core deficit; a short fixed block keeps work inside an attention span you can actually hold.",
    score: (a) => (a.interest === "BORING" ? 2 : 0) + durationScore(a, 2, 1),
  },
];

/** The quick-pick menu: common ADHD-hard activities, pre-tagged along the activity dimensions. */
export const Archetypes = [
  {
    id: "boring_admin", icon: "form",
    activity: activity({
      name: "The Boring Admin", interest: "BORING", deadline: "SOFT", structure: "DEFINED",
      duration: "LONG_OR_UNKNOWN", load: "HEAVY", multiStep: true, hasDread: true, externallyImposed: true,
    }),
  },
  {
    id: "the_reply", icon: "envelope",
    activity: activity({
      name: "The Reply", interest: "BORING", deadline: "SOFT", structure: "DEFINED",
      duration: "QUICK", externallyImposed: true,
    }),
  },
  {
    id: "the_appointment", icon: "clock",
    activity: activity({
      name: "The Appointment", interest: "NEUTRAL", deadline: "HARD", structure: "DEFINED",
      duration: "QUICK", requiresLeavingHome: true, involvesWaiting: true,
    }),
  },
  {
    id: "big_project", icon: "mountain",
    activity: activity({
      name: "The Big Project", interest: "NEUTRAL", deadline: "NONE", structure: "OPEN",
      duration: "LONG_OR_UNKNOWN", load: "HEAVY", multiStep: true,
    }),
  },
  {
    id: "chore", icon: "broom",
    activity: activity({
      name: "The Chore You Avoid", interest: "BORING", deadline: "NONE", structure: "DEFINED",
      duration: "MEDIUM",
    }),
  },
  {
    id: "wind_down", icon: "moon",
    activity: activity({
      name: "The Wind-Down", interest: "NEUTRAL", deadline: "SOFT", structure: "DEFINED",
      duration: "LONG_OR_UNKNOWN",
    }),
  },
  {
    id: "the_errand", icon: "bag",
    activity: activity({
      name: "The Errand", interest: "NEUTRAL", deadline: "SOFT", structure: "DEFINED",
      duration: "QUICK", requiresLeavingHome: true,
    }),
  },
];

/** Toggle keys of the "Other…" flow, in UI order. The string form is a 7-char 0/1 bitfield. */
export const CUSTOM_TOGGLES = [
  { key: "boring", label: "It bores me" },
  { key: "hardDeadline", label: "Fixed time / hard deadline" },
  { key: "unclear", label: "Fuzzy — unclear what 'done' is" },
  { key: "longUnknown", label: "Long or unknown length" },
  { key: "manySteps", label: "Lots of little steps" },
  { key: "leaveHouse", label: "I have to leave the house" },
  { key: "dread", label: "Thinking about it brings dread" },
];

/** Same mapping as the Android "Other…" panel. `bits` is e.g. "1010011". */
export function customActivity(bits) {
  const on = (i) => bits[i] === "1";
  return activity({
    name: "Your Quest",
    interest: on(0) ? "BORING" : "NEUTRAL",
    deadline: on(1) ? "HARD" : "NONE",
    structure: on(2) ? "OPEN" : "DEFINED",
    duration: on(3) ? "LONG_OR_UNKNOWN" : "MEDIUM",
    multiStep: on(4),
    requiresLeavingHome: on(5),
    hasDread: on(6),
  });
}

export const FrictionEngine = {
  /** Ranked, non-empty. Highest score first; ties break by catalog order. */
  analyze(a) {
    const ranked = FrictionCatalog
      .map((point, index) => ({ point, score: point.score(a), index }))
      .filter((x) => x.score > 0)
      .sort((x, y) => y.score - x.score || x.index - y.index)
      .map((x) => x.point);
    return ranked.length ? ranked : [FrictionCatalog.find((p) => p.id === "initiation")];
  },
  topFor(a) {
    return this.analyze(a)[0];
  },
};
