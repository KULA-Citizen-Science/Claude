package com.kula.nextquest.domain

/**
 * The quick-pick menu: common ADHD-hard activities, each pre-tagged along the [Activity]
 * dimensions so a single tap yields a reading. "Other…" is not here — that flow builds a custom
 * [Activity] from the user's toggles (see the app layer).
 *
 * Tags encode where each archetype typically breaks down; they aren't claims about every instance.
 */
object Archetypes {

    /** Stable id + a one-word pixel-icon hint the UI can switch on. */
    data class Archetype(val id: String, val icon: String, val activity: Activity)

    val all: List<Archetype> = listOf(
        Archetype(
            id = "boring_admin", icon = "form",
            activity = Activity(
                name = "The Boring Admin",
                interest = Interest.BORING,
                deadline = Deadline.SOFT,
                structure = Structure.DEFINED,
                duration = Duration.LONG_OR_UNKNOWN,
                load = CognitiveLoad.HEAVY,
                multiStep = true,
                hasDread = true,
                externallyImposed = true,
            ),
        ),
        Archetype(
            id = "the_reply", icon = "envelope",
            activity = Activity(
                name = "The Reply",
                interest = Interest.BORING,
                deadline = Deadline.SOFT,
                structure = Structure.DEFINED,
                duration = Duration.QUICK,
                externallyImposed = true,
            ),
        ),
        Archetype(
            id = "the_appointment", icon = "clock",
            activity = Activity(
                name = "The Appointment",
                interest = Interest.NEUTRAL,
                deadline = Deadline.HARD,
                structure = Structure.DEFINED,
                duration = Duration.QUICK,
                requiresLeavingHome = true,
                involvesWaiting = true,
            ),
        ),
        Archetype(
            id = "big_project", icon = "mountain",
            activity = Activity(
                name = "The Big Project",
                interest = Interest.NEUTRAL,
                deadline = Deadline.NONE,
                structure = Structure.OPEN,
                duration = Duration.LONG_OR_UNKNOWN,
                load = CognitiveLoad.HEAVY,
                multiStep = true,
            ),
        ),
        Archetype(
            id = "chore", icon = "broom",
            activity = Activity(
                name = "The Chore You Avoid",
                interest = Interest.BORING,
                deadline = Deadline.NONE,
                structure = Structure.DEFINED,
                duration = Duration.MEDIUM,
            ),
        ),
        Archetype(
            id = "wind_down", icon = "moon",
            activity = Activity(
                name = "The Wind-Down",
                interest = Interest.NEUTRAL,
                deadline = Deadline.SOFT,
                structure = Structure.DEFINED,
                duration = Duration.LONG_OR_UNKNOWN,
            ),
        ),
        Archetype(
            id = "the_errand", icon = "bag",
            activity = Activity(
                name = "The Errand",
                interest = Interest.NEUTRAL,
                deadline = Deadline.SOFT,
                structure = Structure.DEFINED,
                duration = Duration.QUICK,
                requiresLeavingHome = true,
            ),
        ),
    )
}
