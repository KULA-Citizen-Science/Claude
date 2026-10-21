package com.kula.nextquest.domain

/**
 * One way an activity is likely to derail an inattentive ADHD brain, paired with a single
 * concrete counter-move.
 *
 * [trap] and [strategy] are the two lines shown on the card — deliberately short. [framework] is
 * the tiny tag (and evidence marker) beside them. [why] is the longer reasoning (not shown in the
 * minimal UI, kept so the mapping stays inspectable and testable). [score] rates how strongly this
 * friction point applies to a given [Activity]; 0 means "not relevant here". The engine ranks by
 * score.
 *
 * The catalog lives in [FrictionCatalog]. Declaration order there is also the tie-break order, so
 * it runs roughly most-blocking → least when two points score equally.
 */
data class FrictionPoint(
    val id: String,
    val trap: String,
    val strategy: String,
    val framework: Framework,
    val why: String,
    val score: (Activity) -> Int,
)

object FrictionCatalog {

    val all: List<FrictionPoint> = listOf(
        FrictionPoint(
            id = "dread",
            trap = "The dread — not the task — is what's stopping you.",
            strategy = "Name the feeling out loud, then do one tiny, non-scary piece.",
            framework = Framework.WALL_OF_AWFUL,
            why = "Emotional charge stacked on the task is the real barrier; shrinking to a " +
                "non-threatening first move lets you climb over the wall instead of the whole task.",
            score = { a -> if (a.hasDread) 7 else 0 },
        ),
        FrictionPoint(
            id = "beingLate",
            trap = "You'll start getting ready too late and run behind.",
            strategy = "Set the alarm for 'start getting ready', not 'leave'. Work backwards.",
            framework = Framework.TIME_BLINDNESS,
            why = "With a fixed start you must leave for, time blindness makes the lead-up " +
                "collapse; anchoring the alarm to preparation, not departure, externalises it.",
            score = { a ->
                if (a.requiresLeavingHome) {
                    3 + (if (a.deadline == Deadline.HARD) 4 else 0)
                } else 0
            },
        ),
        FrictionPoint(
            id = "waitGap",
            trap = "The wait will swallow the plan — you'll forget to resume.",
            strategy = "Right now, set an alarm for the far end of the wait.",
            framework = Framework.OBJECT_PERMANENCE,
            why = "During an unstructured gap a plan that isn't visibly present drops out of " +
                "awareness; an alarm makes the resume point exist again.",
            score = { a -> if (a.involvesWaiting) 5 else 0 },
        ),
        FrictionPoint(
            id = "transition",
            trap = "Getting out the door is the hard part, not the errand.",
            strategy = "Pre-stage everything by the door now, while it's on your mind.",
            framework = Framework.TRANSITION_COST,
            why = "Leaving one context for another carries a large, underestimated start cost; " +
                "removing the friction in advance shrinks the switch.",
            score = { a -> if (a.requiresLeavingHome) 4 else 0 },
        ),
        FrictionPoint(
            id = "cognitiveLoad",
            trap = "Heavy thinking on low fuel — you'll stall or half-do it.",
            strategy = "Book it for your peak/meds window; don't attempt it tired.",
            framework = Framework.COGNITIVE_LOAD,
            why = "Task demand outruns available executive capacity when energy is low; matching " +
                "heavy load to your best window is the highest-leverage scheduling move.",
            score = { a -> if (a.load == CognitiveLoad.HEAVY) 4 else 0 },
        ),
        FrictionPoint(
            id = "workingMemory",
            trap = "You'll lose the thread and drop a step.",
            strategy = "Dump the steps onto paper first; work the list, not your memory.",
            framework = Framework.WORKING_MEMORY,
            why = "Holding a multi-step sequence 'in mind' overflows working memory; offloading " +
                "it to a visible list removes the load.",
            score = { a -> if (a.multiStep) 5 else 0 },
        ),
        FrictionPoint(
            id = "defineDone",
            trap = "You don't actually know what 'done' looks like yet.",
            strategy = "Before anything else, write one sentence defining 'done'.",
            framework = Framework.DEFINE_DONE,
            why = "An open/ambiguous task has no clear target or first action, so it stalls; " +
                "defining the deliverable is the evidence-based pre-step before scheduling.",
            score = { a ->
                if (a.structure == Structure.OPEN) 4 + (if (a.multiStep) 1 else 0) else 0
            },
        ),
        FrictionPoint(
            id = "prioritisation",
            trap = "Too many entry points — you'll freeze on where to begin.",
            strategy = "Pick the single next physical action; ignore the rest for now.",
            framework = Framework.PRIORITISATION,
            why = "An open multi-step task offers no obvious start, so activation stalls; " +
                "collapsing it to one concrete next action gives a foothold.",
            score = { a -> if (a.multiStep && a.structure == Structure.OPEN) 4 else 0 },
        ),
        FrictionPoint(
            id = "noFinishLine",
            trap = "No deadline means no urgency — it never gets traction.",
            strategy = "Put a real deadline on it and tell someone the deadline.",
            framework = Framework.NO_FINISH_LINE,
            why = "Without urgency the interest-driven brain gets no start signal, and steeper " +
                "temporal discounting buries distant payoffs; a near, witnessed deadline supplies it.",
            score = { a ->
                if (a.deadline == Deadline.NONE) {
                    4 + (if (a.duration == Duration.LONG_OR_UNKNOWN) 2 else 0)
                } else 0
            },
        ),
        FrictionPoint(
            id = "timeEstimation",
            trap = "It'll eat far more time than it feels like it will.",
            strategy = "Guess the minutes, add half again (×1.5), start a visible countdown.",
            framework = Framework.TIME_BLINDNESS,
            why = "Timing deficits bias estimates short (planning fallacy, amplified in ADHD); a " +
                "buffer multiplier plus a visible timer externalises time at the point of performance.",
            score = { a ->
                when (a.duration) {
                    Duration.LONG_OR_UNKNOWN -> 4
                    Duration.MEDIUM -> 1
                    Duration.QUICK -> 0
                }
            },
        ),
        FrictionPoint(
            id = "initiation",
            trap = "You won't cross from deciding to doing.",
            strategy = "Make an if-then: 'When I sit down, I start a 5-min timer on step one.'",
            framework = Framework.ACTIVATION_ENERGY,
            why = "Starting is a distinct executive step that fails on its own; an implementation " +
                "intention (Gollwitzer) plus a tiny timed first action lowers activation energy " +
                "below the barrier.",
            score = { a ->
                2 +
                    (if (a.interest == Interest.BORING) 2 else 0) +
                    (if (a.externallyImposed) 1 else 0) +
                    (if (a.deadline == Deadline.NONE) 1 else 0)
            },
        ),
        FrictionPoint(
            id = "boredom",
            trap = "Too dull to hold you — you'll bounce to something shinier.",
            strategy = "Bundle it with music or a podcast, or race the clock.",
            framework = Framework.INTEREST_DRIVEN,
            why = "Low stimulation gives the interest-driven nervous system nothing to lock " +
                "onto; adding novelty or a challenge supplies the missing pull.",
            score = { a ->
                if (a.interest == Interest.BORING) 4 + (if (a.externallyImposed) 1 else 0) else 0
            },
        ),
        FrictionPoint(
            id = "attentionDrift",
            trap = "You'll drift off somewhere in the middle.",
            strategy = "Work one 25-minute block; park it when the timer rings.",
            framework = Framework.SUSTAINED_EFFORT,
            why = "Sustaining effort as novelty fades is a core deficit; a short fixed block " +
                "keeps work inside an attention span you can actually hold.",
            score = { a ->
                (if (a.interest == Interest.BORING) 2 else 0) +
                    when (a.duration) {
                        Duration.LONG_OR_UNKNOWN -> 2
                        Duration.MEDIUM -> 1
                        Duration.QUICK -> 0
                    }
            },
        ),
    )
}
