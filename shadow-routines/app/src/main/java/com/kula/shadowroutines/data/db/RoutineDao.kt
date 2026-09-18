package com.kula.shadowroutines.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.kula.shadowroutines.data.db.entity.ChecklistItemEntity
import com.kula.shadowroutines.data.db.entity.RoutineEntity
import com.kula.shadowroutines.data.db.entity.RoutineStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Insert
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Insert
    suspend fun insertItems(items: List<ChecklistItemEntity>)

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeRoutine(id: Long): Flow<RoutineWithItems?>

    @Transaction
    @Query("SELECT * FROM routines WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: RoutineStatus): Flow<List<RoutineWithItems>>

    @Query("UPDATE checklist_items SET isChecked = :checked WHERE id = :itemId")
    suspend fun setItemChecked(itemId: Long, checked: Boolean)

    @Query("UPDATE routines SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: RoutineStatus, completedAt: Long?)

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutine(id: Long): RoutineEntity?

    @Query("SELECT COUNT(*) FROM routines WHERE status = :status")
    fun countByStatus(status: RoutineStatus): Flow<Int>
}
