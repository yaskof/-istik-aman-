package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FortuneDatabase
import com.example.data.FortuneReading
import com.example.data.FortuneRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FortuneType
import com.example.ui.viewmodel.FortuneViewModel
import com.example.ui.viewmodel.FortuneViewModelFactory
import com.example.ui.viewmodel.ReadingUiState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val database = FortuneDatabase.getDatabase(context)
                val repository = FortuneRepository(database.fortuneDao())
                val viewModel: FortuneViewModel = viewModel(
                    factory = FortuneViewModelFactory(repository)
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        StarrySkyBackdrop()
                        MainAppContent(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun StarrySkyBackdrop() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    // Generate static positions for 45 stars
    val stars = remember {
        List(45) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 4.5f + 1.5f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07020E),
                        Color(0xFF110729),
                        Color(0xFF080312)
                    )
                )
            )
            .drawBehind {
                stars.forEach { (x, y, radius) ->
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = alphaAnim * (0.3f + 0.7f * Random.nextFloat())),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(x * size.width, y * size.height)
                    )
                }
            }
    )
}

@Composable
fun MainAppContent(viewModel: FortuneViewModel) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("Fal Bak", "Kehanet", "Mistik Arşiv")

    val uiState = viewModel.readingUiState

    // Auto-switch to Results tab when reading is successful
    LaunchedEffect(uiState) {
        if (uiState is ReadingUiState.Success) {
            activeTab = 1
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // App Header
        AppHeaderSection()

        // Material 3 Tab Row
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Serif,
                            fontSize = 15.sp
                        )
                    },
                    icon = {
                        val icon = when (index) {
                            0 -> Icons.Default.Star
                            1 -> Icons.Default.Favorite
                            else -> Icons.Default.List
                        }
                        Icon(icon, contentDescription = null, tint = if (activeTab == index) MaterialTheme.colorScheme.primary else Color.Gray)
                    },
                    modifier = Modifier.testTag("tab_$index")
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (activeTab) {
                0 -> ReadingInputFormScreen(viewModel = viewModel)
                1 -> ResultsScreen(viewModel = viewModel)
                2 -> ArchiveScreen(viewModel = viewModel)
            }

            // Global Loading Screen overlay
            if (uiState is ReadingUiState.Loading) {
                LoadingOverlayScreen()
            }
        }
    }
}

@Composable
fun AppHeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp).rotate(15f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "MİSTİK ŞAMAN",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp).rotate(-15f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingInputFormScreen(viewModel: FortuneViewModel) {
    val scrollState = rememberScrollState()
    val contextForPicker = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome and Intro Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Geleceğin Sırrını Keşfet",
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Şaman aurasıyla harmanlanmış geleneksel kahve falı, Tarot kartlarının gizemli rehberliği veya gök kubbenin kadim Yıldızname haritası... Bilgilerini gir, kozmik enerjini şamana bağla.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        // Section 1: User Profile Context
        Text(
            text = "🌟 RUHSAL KİMLİK",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp, start = 4.dp),
            letterSpacing = 1.sp
        )

        OutlinedTextField(
            value = viewModel.userName,
            onValueChange = { viewModel.userName = it },
            label = { Text("İsminiz") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("user_name_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            )
        )

        OutlinedTextField(
            value = viewModel.userAge,
            onValueChange = { viewModel.userAge = it },
            label = { Text("Yaşınız") },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("user_age_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            )
        )

        // Dropdown selections using standard Compose Rows/Chips for maximum stability & visual flair
        Text(
            text = "Cinsiyet / Zamirler",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val genders = listOf("Kadın", "Erkek", "Belirtmek İstemiyorum")
            genders.forEach { gender ->
                val selected = viewModel.userGender == gender
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.userGender = gender }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gender,
                        color = if (selected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Relationship & Career rows
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("İlişki Durumu", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable {
                            val statuses = listOf("Bekar", "İlişkisi var", "Kafası karışık", "Boşanmış/Dul")
                            val nextIndex = (statuses.indexOf(viewModel.relationshipStatus) + 1) % statuses.size
                            viewModel.relationshipStatus = statuses[nextIndex]
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(viewModel.relationshipStatus, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Kariyer / Eğitim", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable {
                            val careers = listOf("Öğrenci", "Çalışıyor", "İş Arıyor", "Girişimci")
                            val nextIndex = (careers.indexOf(viewModel.careerStatus) + 1) % careers.size
                            viewModel.careerStatus = careers[nextIndex]
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(viewModel.careerStatus, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Focus Area Selection
        Text(
            text = "Odaklanmak İstediğiniz Alan",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 6.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val focuses = listOf("Genel" to Icons.Default.Star, "Aşk" to Icons.Default.Favorite, "Para/Kariyer" to Icons.Default.Home, "Sağlık" to Icons.Default.Info)
            focuses.forEach { (focus, icon) ->
                val selected = viewModel.focusArea == focus
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.focusArea = focus },
                    label = { Text(focus, fontSize = 12.sp) },
                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.Black,
                        selectedLeadingIconColor = Color.Black,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        labelColor = Color.White,
                        iconColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Section 2: Fortune Type Choice
        Text(
            text = "🔮 KEHANET YOLU SEÇİMİ",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp, start = 4.dp),
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf(
                FortuneType.KAHVE to "☕ Kahve Falı",
                FortuneType.TAROT to "🃏 Tarot Falı",
                FortuneType.ASTROLOJI to "🌌 Astroloji"
            )
            types.forEach { (type, label) ->
                val selected = viewModel.fortuneType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                        .border(1.dp, if (selected) MaterialTheme.colorScheme.secondary else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.fortuneType = type }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Section 3: Dynamic input based on chosen type
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (viewModel.fortuneType) {
                    FortuneType.KAHVE -> {
                        Text(
                            text = "Fincan Sembolleri Seçimi",
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Kahve fincanınızda veya tabağınızda gözünüze çarpan işaretleri seçin:",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Visual grid of coffee symbols
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            viewModel.availableCoffeeSymbols.forEach { (symbol, desc) ->
                                val selected = viewModel.selectedCoffeeSymbols.contains(symbol)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        .clickable { viewModel.toggleCoffeeSymbol(symbol) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = symbol,
                                            color = if (selected) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Coffee Cup Photo Upload Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "☕ Kahve Fincanı Fotoğrafı (Telve Analizi)",
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = "Fincanınızın içini gösteren bir fotoğraf yükleyin veya şamanın kozmik odaklanması için hazır mistik desenlerden birini seçin:",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Launcher for image picker
                        val contextForPicker = LocalContext.current
                        val imagePickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            if (uri != null) {
                                viewModel.coffeePhotoUri = uri.toString()
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Image Preview or Placeholder
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!viewModel.coffeePhotoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = viewModel.coffeePhotoUri,
                                        contentDescription = "Fincan Fotoğrafı",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    // Remove button overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            .clickable { viewModel.coffeePhotoUri = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(12.dp))
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Ekle", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }

                            // Picker controls + Presets
                            Column(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Fotoğraf Yükle (Galeri)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Veya Mistik Desenlerden Seç:", color = Color.Gray, fontSize = 9.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val presets = listOf(
                                        "Girdap" to "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?q=80&w=300",
                                        "Kader" to "https://images.unsplash.com/photo-1541167760496-1628856ab772?q=80&w=300",
                                        "Şaman" to "https://images.unsplash.com/photo-1509042239860-f550ce710b93?q=80&w=300"
                                    )
                                    presets.forEach { (name, url) ->
                                        val isSelected = viewModel.coffeePhotoUri == url
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                .clickable { viewModel.coffeePhotoUri = url }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = viewModel.coffeeCustomNotes,
                            onValueChange = { viewModel.coffeeCustomNotes = it },
                            label = { Text("Fincan Detayları & Ekstra Sorular") },
                            placeholder = { Text("Örn: Dipte büyük bir yol ayrımı var...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                            )
                        )
                    }

                    FortuneType.TAROT -> {
                        Text(
                            text = "Tarot Kartlarınızı Seçin",
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "Kozmik enerjinizin kartlara yansıması için butona tıklayarak üç adet kader kartı çekin:",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (viewModel.drawnTarotCards.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // 3 facedown card placeholders
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .width(75.dp)
                                            .height(115.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(Color(0xFF2C164D), Color(0xFF0F071C))
                                                )
                                            )
                                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.drawTarotCards() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Kartları Çek (Kaderi Belirle)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    viewModel.drawnTarotCards.forEachIndexed { idx, (card, upright) ->
                                        Card(
                                            modifier = Modifier
                                                .width(90.dp)
                                                .height(145.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = when(idx) {
                                                        0 -> "AURA"
                                                        1 -> "YOL"
                                                        else -> "SONUÇ"
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    letterSpacing = 1.sp
                                                )

                                                Icon(
                                                    imageVector = if (upright) Icons.Default.Star else Icons.Default.Favorite,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )

                                                Text(
                                                    text = card.nameTr.substringBefore(" ("),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    textAlign = TextAlign.Center,
                                                    color = Color.White,
                                                    lineHeight = 11.sp
                                                )

                                                Text(
                                                    text = if (upright) "DÜZ" else "TERS",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (upright) Color.Green else Color.Red
                                                )
                                            }
                                        }
                                    }
                                }

                                TextButton(
                                    onClick = { viewModel.drawTarotCards() },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tekrar Çek", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    FortuneType.ASTROLOJI -> {
                        Text(
                            text = "Doğum Haritası Parametreleri",
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "Göksel gezegen transitlerinizi şaman rehberliğinde analiz edebilmek için doğum bilgilerinizi giriniz:",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = viewModel.birthDate,
                            onValueChange = { viewModel.birthDate = it },
                            label = { Text("Doğum Tarihi") },
                            placeholder = { Text("GG.AA.YYYY (Örn: 24.11.1998)") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                            )
                        )

                        OutlinedTextField(
                            value = viewModel.birthTime,
                            onValueChange = { viewModel.birthTime = it },
                            label = { Text("Doğum Saati (Opsiyonel)") },
                            placeholder = { Text("SS:DD (Örn: 14:30 veya Bilmiyorum)") },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                            )
                        )

                        OutlinedTextField(
                            value = viewModel.birthCity,
                            onValueChange = { viewModel.birthCity = it },
                            label = { Text("Doğum Şehri") },
                            placeholder = { Text("Örn: İzmir, Türkiye") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        // Display validation errors if any
        val uiState = viewModel.readingUiState
        if (uiState is ReadingUiState.Error && uiState.message.startsWith("Lütfen")) {
            Text(
                text = uiState.message,
                color = Color.Red,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Submit Button
        Button(
            onClick = { viewModel.readFortune(contextForPicker) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("read_fortune_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ŞAMANDAN KEHANETİ İSTE!",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ResultsScreen(viewModel: FortuneViewModel) {
    val uiState = viewModel.readingUiState
    val scrollState = rememberScrollState()

    if (uiState !is ReadingUiState.Success && viewModel.lastReadingResponse == null) {
        // Empty state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Kehanet Odası Boş",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Auranız şamanla henüz birleşmedi. İlk falınızı bakmak için 'Fal Bak' sekmesinden bir kehanet isteğinde bulunun.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
        return
    }

    val response = (uiState as? ReadingUiState.Success)?.response ?: viewModel.lastReadingResponse!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mystic Scroll Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ŞAMAN KEHANETİ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = response.meta.dominantSymbolOrCard.uppercase(),
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Aura: ${response.meta.energyLevel}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Yol: ${response.meta.fortuneType}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!viewModel.coffeePhotoUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AsyncImage(
                        model = viewModel.coffeePhotoUri,
                        contentDescription = "Kahve Fincanı Fotoğrafı",
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
        }

        // Sections
        val sectionsList = listOf(
            Triple(response.sections.giris.title, response.sections.giris.content, Icons.Default.Person),
            Triple(response.sections.askVeIliskiler.title, response.sections.askVeIliskiler.content, Icons.Default.Favorite),
            Triple(response.sections.isVeKariyer.title, response.sections.isVeKariyer.content, Icons.Default.Home),
            Triple(response.sections.gelecekVeIsaretler.title, response.sections.gelecekVeIsaretler.content, Icons.Default.Star),
            Triple(response.sections.samaniOgut.title, response.sections.samaniOgut.content, Icons.Default.Info)
        )

        sectionsList.forEach { (title, content, icon) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = content,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }

        // Section 4: Shaman Stats (Lucky numbers, Colors, Mantra)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "KOZMİK FREKANSLAR",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mistik Sayılar", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            response.mysticStats.luckyNumbers.forEach { num ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = num.toString(), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Uğurlu Renkler", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Text(
                            text = response.mysticStats.luckyColors.joinToString(" & "),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Günün Şamanik Mantrası",
                    color = MaterialTheme.colorScheme.secondary,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "\"${response.mysticStats.dailyMantra}\"",
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.startNewReading() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("reset_reading_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Yeni Bir Fal İste", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ArchiveScreen(viewModel: FortuneViewModel) {
    val history by viewModel.historyState.collectAsStateWithLifecycle()

    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Kozmik Arşiv Boş",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Henüz kaydedilmiş bir kehanet bulunmuyor. Aldığınız fallar otomatik olarak arşivinize kaydedilir.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KAYDEDİLEN KEHANETLER (${history.size})",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )

            TextButton(
                onClick = { viewModel.clearAllHistory() },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tümünü Sil", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(history) { reading ->
                ArchiveReadingCard(
                    reading = reading,
                    onViewClick = { viewModel.selectHistoryReading(reading) },
                    onDeleteClick = { viewModel.deleteHistoryReading(reading.id) }
                )
            }
        }
    }
}

@Composable
fun ArchiveReadingCard(
    reading: FortuneReading,
    onViewClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateStr = remember(reading.createdAt) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(reading.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (reading.fortuneType) {
                        "KAHVE" -> "☕"
                        "TAROT" -> "🃏"
                        else -> "🌌"
                    }
                    Text(icon, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reading.fortuneType,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = dateStr,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Karakter: ${reading.userName} (${reading.userAge}) - Odak: ${reading.focusArea}",
                color = Color.LightGray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ana Sembol: ${reading.dominantSymbol}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frekans: ${reading.energyLevel}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onViewClick() }
                    ) {
                        Text("Detayları Oku", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingOverlayScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "rotate_shaman"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_orb"
    )

    // Dynamic loading messages list
    val loadingMessages = listOf(
        "Kozmik frekanslar ayarlanıyor...",
        "Yıldız haritaları ve gök kubbe taranıyor...",
        "Şaman ruhlarla kadim bağ kuruluyor...",
        "Fincandaki işaretler yapay zeka ile çözümleniyor...",
        "Kaderinizin kartları fısıldıyor..."
    )
    var activeMessageIdx by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            activeMessageIdx = (activeMessageIdx + 1) % loadingMessages.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {}, // Prevent taps
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Glowing Shaman Mandala Circle
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .drawBehind {
                        // Background glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFB388FF).copy(alpha = 0.3f), Color.Transparent)
                            ),
                            radius = size.width * 0.75f * scale
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Animated rotating stroke
                Canvas(modifier = Modifier.size(100.dp).rotate(rotation)) {
                    drawCircle(
                        color = Color(0xFFFFD700),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(20f, 15f), 0f
                            )
                        )
                    )
                }

                // Inner pulsing core
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFB388FF))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "KAHİN AYİNİ BAŞLADI",
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Smooth crossfade-like effect for text
            Text(
                text = loadingMessages[activeMessageIdx],
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.height(40.dp)
            )
        }
    }
}

// Simple Layout helpers since standard FlowRow/FlowColumn are standard in newer Compose versions,
// but to ensure 100% backward compatibility on any older Compose runtime, we define a lightweight layout container:
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val layoutWidth = constraints.maxWidth
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var totalHeight = 0
        val spacingPx = 8.dp.roundToPx()

        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width > layoutWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + spacingPx
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val rowHeights = rows.map { row -> row.maxOfOrNull { it.height } ?: 0 }
        totalHeight = rowHeights.sum() + (rows.size - 1) * spacingPx

        layout(layoutWidth, totalHeight) {
            var currentY = 0
            rows.forEachIndexed { rowIndex, row ->
                var currentX = 0
                val rowHeight = rowHeights[rowIndex]
                row.forEach { placeable ->
                    placeable.placeRelative(currentX, currentY + (rowHeight - placeable.height) / 2)
                    currentX += placeable.width + spacingPx
                }
                currentY += rowHeight + spacingPx
            }
        }
    }
}

suspend fun delay(timeMillis: Long) {
    kotlinx.coroutines.delay(timeMillis)
}
