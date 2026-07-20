package com.kula.nextquest.domain

/**
 * Maps an [Activity] to its likely friction points, ranked most-blocking first.
 *
 * Pure and deterministic: given the same activity it always returns the same ordered list. The UI
 * shows the top one and lets the user cycle through the rest ("NEXT TRAP"). Every activity yields
 * at least one point, because [FrictionCatalog]'s `initiation` has a non-zero base score.
 */
object FrictionEngine {

    /** Ranked, non-empty. Ties break by declaration order in [FrictionCatalog.all]. */
    fun analyze(activity: Activity): List<FrictionPoint> {
        val scored = FrictionCatalog.all
            .mapIndexed { index, point -> Triple(point, point.score(activity), index) }
            .filter { it.second > 0 }
            // Highest score first; stable tie-break on original catalog order.
            .sortedWith(compareByDescending<Triple<FrictionPoint, Int, Int>> { it.second }
                .thenBy { it.third })
            .map { it.first }

        // Safety net: never hand the UI an empty reading.
        return scored.ifEmpty { listOf(FrictionCatalog.all.first { it.id == "initiation" }) }
    }

    /** Convenience: just the top friction point for an activity. */
    fun topFor(activity: Activity): FrictionPoint = analyze(activity).first()
}
