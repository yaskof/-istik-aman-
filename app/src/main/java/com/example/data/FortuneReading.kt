package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fortune_readings")
data class FortuneReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userName: String,
    val userAge: Int,
    val userGender: String,
    val relationshipStatus: String,
    val careerStatus: String,
    val focusArea: String,
    val fortuneType: String,
    val fortuneInputData: String,
    val createdAt: Long = System.currentTimeMillis(),
    val energyLevel: String,
    val dominantSymbol: String,
    val girisTitle: String,
    val girisContent: String,
    val askTitle: String,
    val askContent: String,
    val isTitle: String,
    val isContent: String,
    val gelecekTitle: String,
    val gelecekContent: String,
    val samaniTitle: String,
    val samaniContent: String,
    val luckyNumbers: String, // Comma-separated numbers e.g. "3, 7, 12"
    val luckyColors: String,  // Comma-separated colors e.g. "Mor, Altın"
    val dailyMantra: String,
    val coffeePhotoUri: String? = null
)
