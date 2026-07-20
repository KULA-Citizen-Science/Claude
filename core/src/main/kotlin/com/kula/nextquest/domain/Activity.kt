package com.kula.nextquest.domain

/** How stimulating the activity is to *this* nervous system (INCUP: interest). */
enum class Interest { BORING, NEUTRAL, ENGAGING }

/** Time pressure — the urgency signal an ADHD brain does or doesn't get. Distinct from structure. */
enum class Deadline { HARD, SOFT, NONE }

/**
 * Whether the task's steps and endpoint are clear. DEFINED = you know what "done" and the first
 * action are; OPEN = ambiguous, needs a "define the deliverable" pre-step. Kept separate from
 * [Deadline] because an open task and an un-urgent task fail for different reasons.
 */
enum class Structure { DEFINED, OPEN }

/** Rough size. LONG_OR_UNKNOWN is the danger zone for time estimation. */
enum class Duration { QUICK, MEDIUM, LONG_OR_UNKNOWN }

/** Mental demand — used to match heavy work to a peak/medication energy window. */
enum class CognitiveLoad { LIGHT, HEAVY }

/**
 * A planned next activity, described along a handful of orthogonal, research-grounded dimensions.
 * The engine reasons over these properties — not over the activity's name — so anything the user
 * can characterise, it can advise on.
 *
 * The dimensions follow the synthesized taxonomy in `docs/RESEARCH.md` (activation, EF demand,
 * structure, time-estimate, energy/load, interest/urgency). Archetypes ship as pre-filled
 * [Activity] values; the "Other…" flow lets the user set the high-signal ones via quick toggles.
 */
data class Activity(
    /** Display name, e.g. "The Boring Admin". Not used by the engine's reasoning. */
    val name: String,
    val interest: Interest = Interest.NEUTRAL,
    val deadline: Deadline = Deadline.NONE,
    val structure: Structure = Structure.DEFINED,
    val duration: Duration = Duration.MEDIUM,
    val load: CognitiveLoad = CognitiveLoad.LIGHT,
    /** Many steps to sequence vs. a single action. */
    val multiStep: Boolean = false,
    /** Requires leaving the house / a real context switch to begin. */
    val requiresLeavingHome: Boolean = false,
    /** Thinking about it brings dread / avoidance (emotional charge). */
    val hasDread: Boolean = false,
    /** Involves an unstructured wait (an appointment, something cooking, a reply). */
    val involvesWaiting: Boolean = false,
    /** Imposed by someone else rather than chosen (lowers intrinsic pull). */
    val externallyImposed: Boolean = false,
)
