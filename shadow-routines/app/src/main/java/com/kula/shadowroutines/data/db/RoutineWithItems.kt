package com.kula.shadowroutines.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.kula.shadowroutines.data.db.entity.ChecklistItemEntity
import com.kula.shadowroutines.data.db.entity.RoutineEntity

/** A routine plus its checklist items, ordered for display by the DAO query. */
data class RoutineWithItems(
    @Embedded val routine: RoutineEntity,
    @Relation(parentColumn = "id", entityColumn = "routineId")
    val items: List<ChecklistItemEntity>,
) {
    val checkedCount: Int get() = items.count { it.isChecked }
    val totalCount: Int get() = items.size
}
