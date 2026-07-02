package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FortuneDao {
    @Query("SELECT * FROM fortune_readings ORDER BY createdAt DESC")
    fun getAllReadings(): Flow<List<FortuneReading>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: FortuneReading): Long

    @Query("DELETE FROM fortune_readings WHERE id = :id")
    suspend fun deleteReadingById(id: Long)

    @Query("DELETE FROM fortune_readings")
    suspend fun deleteAllReadings()
}
