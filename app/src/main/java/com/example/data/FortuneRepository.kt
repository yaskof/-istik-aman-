package com.example.data

import kotlinx.coroutines.flow.Flow

class FortuneRepository(private val fortuneDao: FortuneDao) {
    val allReadings: Flow<List<FortuneReading>> = fortuneDao.getAllReadings()

    suspend fun insertReading(reading: FortuneReading): Long {
        return fortuneDao.insertReading(reading)
    }

    suspend fun deleteReadingById(id: Long) {
        fortuneDao.deleteReadingById(id)
    }

    suspend fun deleteAll() {
        fortuneDao.deleteAllReadings()
    }
}
