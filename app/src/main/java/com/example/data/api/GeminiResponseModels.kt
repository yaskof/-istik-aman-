package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FortuneResponse(
    @Json(name = "meta") val meta: FortuneMeta,
    @Json(name = "sections") val sections: FortuneSections,
    @Json(name = "mystic_stats") val mysticStats: MysticStats
)

@JsonClass(generateAdapter = true)
data class FortuneMeta(
    @Json(name = "fortune_type") val fortuneType: String,
    @Json(name = "energy_level") val energyLevel: String,
    @Json(name = "dominant_symbol_or_card") val dominantSymbolOrCard: String
)

@JsonClass(generateAdapter = true)
data class FortuneSections(
    @Json(name = "giris") val giris: FortuneSectionItem,
    @Json(name = "ask_ve_iliskiler") val askVeIliskiler: FortuneSectionItem,
    @Json(name = "is_ve_kariyer") val isVeKariyer: FortuneSectionItem,
    @Json(name = "gelecek_ve_isaretler") val gelecekVeIsaretler: FortuneSectionItem,
    @Json(name = "samani_ogut") val samaniOgut: FortuneSectionItem
)

@JsonClass(generateAdapter = true)
data class FortuneSectionItem(
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class MysticStats(
    @Json(name = "lucky_numbers") val luckyNumbers: List<Int>,
    @Json(name = "lucky_colors") val luckyColors: List<String>,
    @Json(name = "daily_mantra") val dailyMantra: String
)
