package com.kula.shadowroutines.core.routine

/**
 * The five families of "soft aspects" the app externalizes around a task. These are the
 * often-forgotten steps that executive-function research suggests benefit from being made
 * explicit rather than held in working memory.
 */
enum class SoftAspectCategory(val displayName: String) {
    TIME_BUFFER("Time buffers"),
    COMMUNICATION("Communication"),
    LOGISTICS("Logistics"),
    SELF_CARE("Self-care"),
    FOLLOW_UP("Follow-ups"),
}

/**
 * A single checklist item defined by a template. This is a *definition*: when a routine is
 * started, each aspect is snapshotted into a mutable instance item (see the :app data layer),
 * so later edits to a template never rewrite a routine you already completed.
 *
 * @param id stable within a template; used to correlate a snapshot back to its source.
 * @param order display order within the checklist (ascending).
 */
data class SoftAspect(
    val id: String,
    val category: SoftAspectCategory,
    val label: String,
    val defaultChecked: Boolean = false,
    val order: Int,
)

/**
 * A reusable routine definition for a "core task type" (meeting, workshop, commute, ...).
 *
 * @param contextTags seed terms mixed into the quote query when a routine of this type is
 *   completed, so the reward quote can lean toward the task's theme even before the user's
 *   own title/description keywords are considered.
 */
data class RoutineTemplate(
    val id: String,
    val name: String,
    val description: String,
    val contextTags: List<String>,
    val aspects: List<SoftAspect>,
) {
    init {
        require(id.isNotBlank()) { "template id must not be blank" }
        require(aspects.isNotEmpty()) { "template '$id' must define at least one aspect" }
    }
}
