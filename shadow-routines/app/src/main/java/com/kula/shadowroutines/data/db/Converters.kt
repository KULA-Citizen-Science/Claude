package com.kula.shadowroutines.data.db

import androidx.room.TypeConverter
import com.kula.shadowroutines.data.db.entity.RoutineStatus

class Converters {
    @TypeConverter
    fun statusToString(status: RoutineStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): RoutineStatus = RoutineStatus.valueOf(value)
}
