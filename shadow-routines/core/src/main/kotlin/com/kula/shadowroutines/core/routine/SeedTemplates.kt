package com.kula.shadowroutines.core.routine

import com.kula.shadowroutines.core.routine.SoftAspectCategory.COMMUNICATION
import com.kula.shadowroutines.core.routine.SoftAspectCategory.FOLLOW_UP
import com.kula.shadowroutines.core.routine.SoftAspectCategory.LOGISTICS
import com.kula.shadowroutines.core.routine.SoftAspectCategory.SELF_CARE
import com.kula.shadowroutines.core.routine.SoftAspectCategory.TIME_BUFFER

/**
 * The built-in v0 routine templates. These are plain data so they can be seeded into the
 * database on first run and unit-tested without Android. Ids are stable strings; keep them
 * stable across versions so completed routines keep pointing at a real template.
 */
object SeedTemplates {

    val all: List<RoutineTemplate> by lazy {
        listOf(meeting, workshop, commute, deepWork, errand, dailyRoutine)
    }

    fun byId(id: String): RoutineTemplate? = all.firstOrNull { it.id == id }

    // Small helper so the template definitions below stay readable.
    private fun aspects(vararg specs: Triple<SoftAspectCategory, String, Boolean>): List<SoftAspect> =
        specs.mapIndexed { index, (category, label, checked) ->
            SoftAspect(
                id = "a${index + 1}",
                category = category,
                label = label,
                defaultChecked = checked,
                order = index,
            )
        }

    private infix fun SoftAspectCategory.to2(label: String) = Triple(this, label, false)

    val meeting = RoutineTemplate(
        id = "meeting",
        name = "Meeting",
        description = "A scheduled conversation with other people.",
        contextTags = listOf("work", "communication", "time", "people"),
        aspects = aspects(
            TIME_BUFFER to2 "Add 10 min travel/setup buffer before",
            TIME_BUFFER to2 "Block 10 min after for notes, don't schedule back-to-back",
            COMMUNICATION to2 "Confirm time & location with attendees",
            COMMUNICATION to2 "Send agenda or purpose ahead of time",
            LOGISTICS to2 "Check the room / video link works",
            LOGISTICS to2 "Have notes, documents, and any demo ready",
            SELF_CARE to2 "Water / bathroom before it starts",
            FOLLOW_UP to2 "Write a 3-line summary & action items after",
            FOLLOW_UP to2 "Send follow-up / thank-you within a day",
        ),
    )

    val workshop = RoutineTemplate(
        id = "workshop",
        name = "Workshop planning",
        description = "Designing and running a session for a group.",
        contextTags = listOf("work", "creativity", "teaching", "preparation", "time"),
        aspects = aspects(
            TIME_BUFFER to2 "Estimate conservatively — double your first guess",
            TIME_BUFFER to2 "Build in breaks and buffer between activities",
            COMMUNICATION to2 "Save-the-date sent to participants",
            COMMUNICATION to2 "Confirmation & joining details a week before",
            COMMUNICATION to2 "Reminder the day before",
            LOGISTICS to2 "Book the space / room",
            LOGISTICS to2 "Gather materials & handouts",
            LOGISTICS to2 "Tech check: projector, sound, internet",
            SELF_CARE to2 "Plan snacks / water for yourself and the group",
            FOLLOW_UP to2 "Send notes / recap and thank-yous after",
            FOLLOW_UP to2 "Capture what to change next time",
        ),
    )

    val commute = RoutineTemplate(
        id = "commute",
        name = "Commute / travel",
        description = "Getting from one place to another on time.",
        contextTags = listOf("travel", "time", "transition"),
        aspects = aspects(
            TIME_BUFFER to2 "Add a buffer for delays — leave earlier than feels needed",
            LOGISTICS to2 "Check route, transport times, or fuel",
            LOGISTICS to2 "Keys, wallet, phone, charger, ticket",
            SELF_CARE to2 "Water and a snack for the journey",
            SELF_CARE to2 "Something to occupy the transition (podcast, music)",
            FOLLOW_UP to2 "Note anything to do on arrival before you forget",
        ),
    )

    val deepWork = RoutineTemplate(
        id = "deep-work",
        name = "Deep work block",
        description = "A protected block of focused, high-effort work.",
        contextTags = listOf("focus", "work", "solitude", "time", "creativity"),
        aspects = aspects(
            TIME_BUFFER to2 "Pick a realistic block length (start with 45–60 min)",
            COMMUNICATION to2 "Tell people you'll be unavailable / set status",
            LOGISTICS to2 "Silence notifications, close distracting tabs",
            LOGISTICS to2 "One clear next action written down before you start",
            SELF_CARE to2 "Water, snack, bathroom before you begin",
            SELF_CARE to2 "Plan a decompression break for after",
            FOLLOW_UP to2 "Note where you stopped so restarting is easy",
        ),
    )

    val errand = RoutineTemplate(
        id = "errand",
        name = "Errand",
        description = "A short out-and-about task with a concrete goal.",
        contextTags = listOf("logistics", "time", "everyday"),
        aspects = aspects(
            TIME_BUFFER to2 "Check opening hours and travel time",
            LOGISTICS to2 "List everything you need to bring or return",
            LOGISTICS to2 "Wallet, bags, phone, list",
            FOLLOW_UP to2 "Put away / file anything you brought back",
        ),
    )

    val dailyRoutine = RoutineTemplate(
        id = "daily-routine",
        name = "Daily routine",
        description = "A recurring anchor routine (morning, evening, reset).",
        contextTags = listOf("everyday", "self-care", "habit", "time"),
        aspects = aspects(
            SELF_CARE to2 "Meds / supplements",
            SELF_CARE to2 "Water & something to eat",
            SELF_CARE to2 "Movement or a moment outside",
            LOGISTICS to2 "Prep one thing for tomorrow-you",
            FOLLOW_UP to2 "Quick glance at what's coming up next",
        ),
    )
}
