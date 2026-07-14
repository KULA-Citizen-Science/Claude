package com.kula.shadowroutines.data.repository

import com.kula.shadowroutines.core.quote.QuoteQuery
import com.kula.shadowroutines.core.routine.RoutineTemplate
import com.kula.shadowroutines.core.routine.SeedTemplates
import com.kula.shadowroutines.data.db.RoutineDao
import com.kula.shadowroutines.data.db.RoutineWithItems
import com.kula.shadowroutines.data.db.entity.ChecklistItemEntity
import com.kula.shadowroutines.data.db.entity.RoutineEntity
import com.kula.shadowroutines.data.db.entity.RoutineStatus
import kotlinx.coroutines.flow.Flow

/** Everything needed to build the reward after a routine is completed. */
data class CompletedRoutine(val title: String, val query: QuoteQuery)

class RoutineRepository(private val dao: RoutineDao) {

    val templates: List<RoutineTemplate> get() = SeedTemplates.all

    fun template(id: String): RoutineTemplate? = SeedTemplates.byId(id)

    fun activeRoutines(): Flow<List<RoutineWithItems>> =
        dao.observeByStatus(RoutineStatus.ACTIVE)

    fun observeRoutine(id: Long): Flow<RoutineWithItems?> = dao.observeRoutine(id)

    fun completedCount(): Flow<Int> = dao.countByStatus(RoutineStatus.COMPLETED)

    /**
     * Starts a routine, snapshotting the template's aspects into checklist items. Pass a null
     * [templateId] for an ad-hoc routine (no checklist). Returns the new routine id.
     */
    suspend fun start(templateId: String?, title: String, description: String?): Long {
        val template = templateId?.let { SeedTemplates.byId(it) }
        val routineId = dao.insertRoutine(
            RoutineEntity(
                templateId = templateId,
                title = title,
                description = description,
                contextTags = (template?.contextTags ?: emptyList()).joinToString(","),
                status = RoutineStatus.ACTIVE,
                createdAt = System.currentTimeMillis(),
                completedAt = null,
            ),
        )
        val items = template?.aspects.orEmpty().map { aspect ->
            ChecklistItemEntity(
                routineId = routineId,
                sourceAspectId = aspect.id,
                category = aspect.category.name,
                label = aspect.label,
                isChecked = aspect.defaultChecked,
                order = aspect.order,
            )
        }
        if (items.isNotEmpty()) dao.insertItems(items)
        return routineId
    }

    suspend fun setItemChecked(itemId: Long, checked: Boolean) =
        dao.setItemChecked(itemId, checked)

    /** Marks a routine completed and returns the context for building its reward quote. */
    suspend fun complete(routineId: Long): CompletedRoutine? {
        dao.updateStatus(routineId, RoutineStatus.COMPLETED, System.currentTimeMillis())
        val routine = dao.getRoutine(routineId) ?: return null
        val queryText = listOfNotNull(routine.title, routine.description).joinToString(" ")
        val tags = routine.contextTags.split(",").filter { it.isNotBlank() }
        return CompletedRoutine(routine.title, QuoteQuery(text = queryText, contextTags = tags))
    }
}
