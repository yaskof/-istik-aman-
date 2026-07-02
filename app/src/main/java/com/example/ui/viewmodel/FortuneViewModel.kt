package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FortuneReading
import com.example.data.FortuneRepository
import com.example.data.api.FortuneResponse
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.RetrofitClient
import com.example.BuildConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class FortuneType {
    KAHVE, TAROT, ASTROLOJI
}

sealed interface ReadingUiState {
    object Idle : ReadingUiState
    object Loading : ReadingUiState
    data class Success(val response: FortuneResponse) : ReadingUiState
    data class Error(val message: String) : ReadingUiState
}

data class TarotCard(
    val nameTr: String,
    val nameEn: String,
    val meaning: String,
    val description: String
)

class FortuneViewModel(private val repository: FortuneRepository) : ViewModel() {

    // Inputs state
    var userName by mutableStateOf("")
    var userAge by mutableStateOf("")
    var userGender by mutableStateOf("Kadın")
    var relationshipStatus by mutableStateOf("Bekar")
    var careerStatus by mutableStateOf("Öğrenci")
    var focusArea by mutableStateOf("Genel")
    var fortuneType by mutableStateOf(FortuneType.KAHVE)
    var coffeePhotoUri by mutableStateOf<String?>(null)

    // Coffee reading specific inputs
    val availableCoffeeSymbols = listOf(
        "Kuş" to "Haber, tez zamanda gelecek sevinç",
        "Yol" to "Yolculuk, beklenen bir kapının açılması",
        "Yılan" to "Sinsi insan, arkadan dönen işler",
        "Kalp" to "Yeni aşk, gönül ferahlığı, sevgi",
        "Balık" to "Kısmet, parasal bolluk, şans",
        "Göz" to "Nazar, kıskanç bakışlar, dikkat",
        "Anahtar" to "Yeni fırsat, kilitli kapıların açılması",
        "Yüzük" to "Bağlılık, evlilik, ciddi ilişki",
        "Yıldız" to "Parlak gelecek, başarı, takdir edilme",
        "Ay" to "Huzur, içsel huzur, duygusal kararlar",
        "Çapa" to "İstikrar, güvenli liman, kalıcı işler",
        "Kelebek" to "Kısa süreli büyük mutluluk, tatlı heyecan"
    )
    var selectedCoffeeSymbols by mutableStateOf(setOf<String>())
    var coffeeCustomNotes by mutableStateOf("")

    // Tarot reading specific inputs
    val tarotDeck = listOf(
        TarotCard("Mecnun (The Fool)", "The Fool", "Yeni başlangıçlar, saflık, risk alma", "Hayatında yepyeni ve cesur bir sayfa açılmak üzere."),
        TarotCard("Büyücü (The Magician)", "The Magician", "İrade, odaklanma, yaratıcılık, güç", "İçindeki potansiyeli harekete geçirme zamanı geldi."),
        TarotCard("Azize (The High Priestess)", "The High Priestess", "Sezgiler, gizem, bilinçaltı, sabır", "Sezgilerine güven, sırlar yakında aydınlanacak."),
        TarotCard("İmparatoriçe (The Empress)", "The Empress", "Bolluk, bereket, doğa, şefkat", "Hayatında verimli ve bereketli bir dönem başlıyor."),
        TarotCard("İmparator (The Emperor)", "The Emperor", "Otorite, düzen, güç, liderlik", "Disiplin ve kontrolün öne çıkacağı bir süreç."),
        TarotCard("Aşıklar (The Lovers)", "The Lovers", "Aşk, uyum, ilişkiler, seçimler", "Kalbin ile mantığın arasında önemli bir seçim kapıda."),
        TarotCard("Araba (The Chariot)", "The Chariot", "Zafer, irade gücü, kararlılık, seyahat", "Engelleri kararlılıkla aşarak zafere ulaşacaksın."),
        TarotCard("Adalet (Justice)", "The Justice", "Adalet, dürüstlük, karma, denge", "Hak ettiğin adalet tecelli edecek, dengeler kurulacak."),
        TarotCard("Ermiş (The Hermit)", "The Hermit", "Yalnızlık, içe dönüş, bilgelik, rehberlik", "Ruhsal bir arayış ve içsel dinlenme zamanı."),
        TarotCard("Kader Çarkı (Wheel of Fortune)", "Wheel of Fortune", "Şans, kader, dönüm noktası, değişim", "Şans rüzgarları yön değiştiriyor, kader çarkı senin için dönüyor."),
        TarotCard("Denge (Temperance)", "The Temperance", "Denge, sabır, uyum, şifa", "Farklı alanlarda uyumu yakalayacak ve şifa bulacaksın."),
        TarotCard("Yıldız (The Star)", "The Star", "Umut, inanç, ilham, yenilenme", "Karanlık gecenin ardından en parlak yıldızın doğuyor."),
        TarotCard("Ay (The Moon)", "The Moon", "Yanılsama, korku, belirsizlik, rüyalar", "Sislerin ardındaki gerçekleri görmeye çalış, rüyalarına dikkat et."),
        TarotCard("Güneş (The Sun)", "The Sun", "Başarı, mutluluk, canlılık, netlik", "Hayatın en parlak, en neşeli ve en net dönemi başlıyor."),
        TarotCard("Dünya (The World)", "The World", "Tamamlanma, başarı, bütünlük, seyahat", "Büyük bir döngü başarıyla tamamlanıyor ve ödülünü alıyorsun.")
    )
    var drawnTarotCards by mutableStateOf<List<Pair<TarotCard, Boolean>>>(emptyList()) // Boolean represents Upright (true) or Reversed (false)

    // Astrology specific inputs
    var birthDate by mutableStateOf("")
    var birthTime by mutableStateOf("")
    var birthCity by mutableStateOf("")

    // API Call & Reading State
    var readingUiState by mutableStateOf<ReadingUiState>(ReadingUiState.Idle)
    var lastReadingResponse by mutableStateOf<FortuneResponse?>(null)

    // History Flow
    val historyState: StateFlow<List<FortuneReading>> = repository.allReadings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleCoffeeSymbol(symbol: String) {
        selectedCoffeeSymbols = if (selectedCoffeeSymbols.contains(symbol)) {
            selectedCoffeeSymbols - symbol
        } else {
            selectedCoffeeSymbols + symbol
        }
    }

    fun drawTarotCards() {
        val shuffled = tarotDeck.shuffled()
        val cards = listOf(
            Pair(shuffled[0], Random.nextBoolean()),
            Pair(shuffled[1], Random.nextBoolean()),
            Pair(shuffled[2], Random.nextBoolean())
        )
        drawnTarotCards = cards
    }

    fun deleteHistoryReading(id: Long) {
        viewModelScope.launch {
            repository.deleteReadingById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun selectHistoryReading(reading: FortuneReading) {
        val meta = com.example.data.api.FortuneMeta(
            fortuneType = reading.fortuneType,
            energyLevel = reading.energyLevel,
            dominantSymbolOrCard = reading.dominantSymbol
        )
        val sections = com.example.data.api.FortuneSections(
            giris = com.example.data.api.FortuneSectionItem(reading.girisTitle, reading.girisContent),
            askVeIliskiler = com.example.data.api.FortuneSectionItem(reading.askTitle, reading.askContent),
            isVeKariyer = com.example.data.api.FortuneSectionItem(reading.isTitle, reading.isContent),
            gelecekVeIsaretler = com.example.data.api.FortuneSectionItem(reading.gelecekTitle, reading.gelecekContent),
            samaniOgut = com.example.data.api.FortuneSectionItem(reading.samaniTitle, reading.samaniContent)
        )
        val luckyNumsList = reading.luckyNumbers.split(",").mapNotNull { it.trim().toIntOrNull() }
        val luckyColorsList = reading.luckyColors.split(",").map { it.trim() }
        val stats = com.example.data.api.MysticStats(
            luckyNumbers = luckyNumsList,
            luckyColors = luckyColorsList,
            dailyMantra = reading.dailyMantra
        )
        val response = FortuneResponse(meta, sections, stats)
        lastReadingResponse = response
        coffeePhotoUri = reading.coffeePhotoUri
        readingUiState = ReadingUiState.Success(response)
    }

    fun startNewReading() {
        readingUiState = ReadingUiState.Idle
    }

    fun readFortune(context: android.content.Context) {
        // Validation
        if (userName.isBlank()) {
            readingUiState = ReadingUiState.Error("Lütfen isminizi giriniz.")
            return
        }
        val ageInt = userAge.toIntOrNull()
        if (ageInt == null || ageInt <= 0 || ageInt > 120) {
            readingUiState = ReadingUiState.Error("Lütfen geçerli bir yaş giriniz.")
            return
        }

        val inputData = when (fortuneType) {
            FortuneType.KAHVE -> {
                val symbolsStr = if (selectedCoffeeSymbols.isEmpty()) "Yok" else selectedCoffeeSymbols.joinToString(", ")
                val notes = if (coffeeCustomNotes.isBlank()) "Ekstra açıklama girilmedi." else coffeeCustomNotes
                "Fincanda seçilen semboller: $symbolsStr. Kullanıcı notları: $notes"
            }
            FortuneType.TAROT -> {
                if (drawnTarotCards.size < 3) {
                    readingUiState = ReadingUiState.Error("Lütfen önce 3 adet Tarot kartı çekin.")
                    return
                }
                drawnTarotCards.joinToString("; ") { (card, upright) ->
                    "${card.nameTr} (${if (upright) "Düz" else "Ters"})"
                }
            }
            FortuneType.ASTROLOJI -> {
                if (birthDate.isBlank() || birthCity.isBlank()) {
                    readingUiState = ReadingUiState.Error("Lütfen doğum tarihi ve doğum şehrini doldurun.")
                    return
                }
                val timeStr = if (birthTime.isBlank()) "Bilinmiyor" else birthTime
                "Doğum Tarihi: $birthDate, Doğum Saati: $timeStr, Doğum Yeri: $birthCity"
            }
        }

        readingUiState = ReadingUiState.Loading

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    readingUiState = ReadingUiState.Error("Gemini API anahtarı eksik! Lütfen AI Studio Secrets panelinden GEMINI_API_KEY anahtarını tanımlayın.")
                    return@launch
                }

                val prompt = constructPrompt(
                    name = userName,
                    age = ageInt,
                    gender = userGender,
                    relationship = relationshipStatus,
                    career = careerStatus,
                    focus = focusArea,
                    type = if (fortuneType == FortuneType.KAHVE) "Kahve Falı" else if (fortuneType == FortuneType.TAROT) "Tarot Falı" else "Astroloji Yıldızname",
                    inputData = inputData
                )

                val parts = mutableListOf<GeminiPart>()
                parts.add(GeminiPart(text = prompt))

                // Attach base64 image if exists for coffee fortune
                if (fortuneType == FortuneType.KAHVE && !coffeePhotoUri.isNullOrBlank()) {
                    val base64Str = getBase64FromUri(context, coffeePhotoUri!!)
                    if (base64Str != null) {
                        val mimeType = getMimeType(context, coffeePhotoUri!!) ?: "image/jpeg"
                        parts.add(GeminiPart(inlineData = com.example.data.api.GeminiInlineData(mimeType = mimeType, data = base64Str)))
                    }
                }

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = parts
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 1.0
                    )
                )

                val apiResponse = RetrofitClient.service.generateContent(apiKey, request)
                val rawText = apiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("Yapay zekadan boş bir yanıt alındı.")

                val cleanedJson = cleanJsonResponse(rawText)
                val fortuneResponse = RetrofitClient.moshi.adapter(FortuneResponse::class.java).fromJson(cleanedJson)
                    ?: throw Exception("Fal yanıtı çözümlenemedi. Lütfen tekrar deneyin.")

                // Save to local database
                val entity = FortuneReading(
                    userName = userName,
                    userAge = ageInt,
                    userGender = userGender,
                    relationshipStatus = relationshipStatus,
                    careerStatus = careerStatus,
                    focusArea = focusArea,
                    fortuneType = fortuneType.name,
                    fortuneInputData = inputData,
                    energyLevel = fortuneResponse.meta.energyLevel,
                    dominantSymbol = fortuneResponse.meta.dominantSymbolOrCard,
                    girisTitle = fortuneResponse.sections.giris.title,
                    girisContent = fortuneResponse.sections.giris.content,
                    askTitle = fortuneResponse.sections.askVeIliskiler.title,
                    askContent = fortuneResponse.sections.askVeIliskiler.content,
                    isTitle = fortuneResponse.sections.isVeKariyer.title,
                    isContent = fortuneResponse.sections.isVeKariyer.content,
                    gelecekTitle = fortuneResponse.sections.gelecekVeIsaretler.title,
                    gelecekContent = fortuneResponse.sections.gelecekVeIsaretler.content,
                    samaniTitle = fortuneResponse.sections.samaniOgut.title,
                    samaniContent = fortuneResponse.sections.samaniOgut.content,
                    luckyNumbers = fortuneResponse.mysticStats.luckyNumbers.joinToString(", "),
                    luckyColors = fortuneResponse.mysticStats.luckyColors.joinToString(", "),
                    dailyMantra = fortuneResponse.mysticStats.dailyMantra,
                    coffeePhotoUri = if (fortuneType == FortuneType.KAHVE) coffeePhotoUri else null
                )

                repository.insertReading(entity)

                lastReadingResponse = fortuneResponse
                readingUiState = ReadingUiState.Success(fortuneResponse)

            } catch (e: Exception) {
                readingUiState = ReadingUiState.Error("Fal bakılırken bir hata oluştu: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun getBase64FromUri(context: android.content.Context, uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getMimeType(context: android.content.Context, uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }
    }

    private fun constructPrompt(
        name: String,
        age: Int,
        gender: String,
        relationship: String,
        career: String,
        focus: String,
        type: String,
        inputData: String
    ): String {
        return """
Sana verilen kullanıcı profili ve fal giriş verilerine dayanarak "Mistik Şaman / Baş Falcı" rolünde, derin, sezgisel ve empati dolu bir fal yorumu yap.
Geleneksel ve modern kehanet dillerini harmanlayarak, kültürel göndermeler ve samimi deyimler kullan (örneğin "üç vakte kadar", "yüreğin kabarmış", "gözü olanın gözü çıksın").

KULLANICI PROFİLİ:
- İsim: $name
- Yaş: $age
- Cinsiyet / Zamirler: $gender
- İlişki Durumu: $relationship
- Kariyer / Eğitim Durumu: $career
- Odak Noktası: $focus

FAL TÜRÜ: $type
FAL GİRİŞ VERİSİ: $inputData

Lütfen aşağıdaki kurallara kesinlikle uy:
1. Kesinlikle tıbbi/sağlık teşhisi koyma. Sağlık konusuna sadece genel enerji seviyesi ve ruhsal yenilenme açılarından değin.
2. Kesinlikle ölüm, ağır kaza veya büyük felaket kehanetinde bulunma. Olumsuz işaretleri aşılması gereken engeller veya gezegen gerilemeleri olarak yumuşatarak sun.
3. Finansal veya hukuki kesin garantiler verme. Borsa tüyosu verme, hukuki davaların sonucunu kesin bildirme.
4. Psikolojik güvenliği asla ihlal etme. Kullanıcıyı arkadaşları veya partneri hakkında aşırı şüpheci ve paranoyak yapma.

Cevabını SADECE ve SADECE aşağıdaki JSON şemasına uygun olarak döndür. Markdown etiketleri (```json ... ``` gibi) ekleme, JSON dışında hiçbir giriş veya çıkış metni yazma.

JSON Şeması:
{
  "meta": {
    "fortune_type": "string",
    "energy_level": "string (Yüksek, Gizemli, Melankolik, Parlak vb.)",
    "dominant_symbol_or_card": "string"
  },
  "sections": {
    "giris": {
      "title": "Gizemli Karşılama",
      "content": "string (Kullanıcıya ismiyle hitap eden, aurasını/enerjisini yakalayan mistik bir karşılama cümlesi)"
    },
    "ask_ve_iliskiler": {
      "title": "Aşk ve Gönül İşleri",
      "content": "string (Kullanıcının ilişki durumuna ve odak noktasına göre özelleştirilmiş detaylı yorum)"
    },
    "is_ve_kariyer": {
      "title": "Kariyer ve Maddiyat",
      "content": "string (Kullanıcının kariyer durumuna göre özelleştirilmiş detaylı yorum)"
    },
    "gelecek_ve_isaretler": {
      "title": "Geleceğin Şifreleri ve Uyarılar",
      "content": "string (Spesifik semboller, harfler veya zaman dilimleri. Örn: 'İçinde B veya S harfi olan biri', 'Önümüzdeki Salı günü...')"
    },
    "samani_ogut": {
      "title": "Mistik Tavsiye",
      "content": "string (Enerjiyi dengelemek için güçlendirici, uygulanabilir bir tavsiye)"
    }
  },
  "mystic_stats": {
    "lucky_numbers": [sayı, sayı, sayı],
    "lucky_colors": ["string", "string"],
    "daily_mantra": "string (Kısa, güçlü bir olumlama cümlesi)"
  }
}
""".trimIndent()
    }

    private fun cleanJsonResponse(rawResponse: String): String {
        var cleaned = rawResponse.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substringAfter("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substringBeforeLast("```")
        }
        return cleaned.trim()
    }
}

class FortuneViewModelFactory(private val repository: FortuneRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FortuneViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FortuneViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
