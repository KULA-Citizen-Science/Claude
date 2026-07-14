package com.kula.shadowroutines.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Lifecycle of a routine instance. v0 uses two states; PENDING/scheduled can come later. */
enum class RoutineStatus { ACTIVE, COMPLETED }

/**
 * A started routine ("I'm about to..."). [contextTags] is snapshotted from the template at
 * start time so completing the routine can build a quote query even if the template later
 * changes or the routine was ad-hoc.
 */
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: String?,
    val title: String,
    val description: String?,
    val contextTags: String,
    val status: RoutineStatus,
    val createdAt: Long,
    val completedAt: Long?,
)

/**
 * A snapshot of one template aspect, taken when the routine starts. Snapshotting (rather than
 * referencing the template live) means editing a template never rewrites a routine already
 * completed.
 */
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineId")],
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val sourceAspectId: String?,
    val category: String,
    val label: String,
    val isChecked: Boolean,
    @ColumnInfo(name = "sort_order") val order: Int,
)
