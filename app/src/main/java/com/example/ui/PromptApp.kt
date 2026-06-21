package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.SavedPrompt
import com.example.viewmodel.PromptViewModel
import com.example.viewmodel.UiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom Cosmic Slate / Architectural Blueprint Palette
val BlueprintBg = Color(0xFF070B14)
val BlueprintGrid = Color(0x0F00E5FF)
val CardBg = Color(0xFF101625)
val CardBorderColor = Color(0xFF1E283F)
val NeonCyan = Color(0xFF00E5FF)
val NeonBlue = Color(0xFF4D72FF)
val NeonPurple = Color(0xFF8B5CF6)
val OrangeGold = Color(0xFFFFB300)
val DarkTextSecondary = Color(0xFF8F9BB3)
val WhitePure = Color(0xFFF3F6FC)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromptApp(
    viewModel: PromptViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Build Prompt, 1 = Analyze Image, 2 = Saved blue prints
    var activeDetailPrompt by remember { mutableStateOf<SavedPrompt?>(null) }

    // Collect Room database flow as State
    val savedPrompts by viewModel.allPrompts.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BlueprintBg),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Real-time Blueprint header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Spark",
                                tint = NeonCyan,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = "PROMPT ARCHITECT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp,
                                    color = NeonCyan
                                )
                            )
                        }
                        Text(
                            text = "DRAFTING / ANALYZING ENGINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // Blueprint Status Indicator (Visual Touch)
                    Box(
                        modifier = Modifier
                            .background(Color(0x1A00E5FF), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SYS_ONLINE v3.5",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Segmented Control / Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C101C), RoundedCornerShape(10.dp))
                        .border(1.dp, CardBorderColor, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Generate", "Analyze Image", "My Collection").forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        val bgBrush = if (isSelected) {
                            Brush.horizontalGradient(listOf(NeonBlue, NeonPurple))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgBrush)
                                .clickable { selectedTab = index }
                                .testTag("tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else DarkTextSecondary,
                                    fontFamily = FontFamily.SansSerif
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Drawing the blueprint grid
                .drawBehind {
                    val strokeWidth = 1f
                    val step = 44.dp.toPx()
                    // Draw vertical guide lines
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            BlueprintGrid,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = strokeWidth
                        )
                        x += step
                    }
                    // Draw horizontal guide lines
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            BlueprintGrid,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth
                        )
                        y += step
                    }
                }
        ) {
            when (selectedTab) {
                0 -> PromptBuilderView(viewModel, context)
                1 -> ImageAnalyzerView(viewModel, context)
                2 -> SavedCollectionLibraryView(
                    savedPrompts = savedPrompts,
                    onDelete = { viewModel.deletePrompt(it) },
                    onSelect = { activeDetailPrompt = it }
                )
            }

            // Prompt Detail Modal Overlay
            activeDetailPrompt?.let { savedPrompt ->
                PromptDetailDialog(
                    prompt = savedPrompt,
                    onDismiss = { activeDetailPrompt = null },
                    onCopy = { text ->
                        copyToClipboard(context, text)
                    }
                )
            }
        }
    }
}

// ==========================================
// SCREEN 1: PROMPT BUILDER / GENERATOR
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromptBuilderView(viewModel: PromptViewModel, context: Context) {
    val keywords by viewModel.inputKeywords.collectAsStateWithLifecycle()
    val isStyle by viewModel.selectedStyle.collectAsStateWithLifecycle()
    val isLighting by viewModel.selectedLighting.collectAsStateWithLifecycle()
    val isComposition by viewModel.selectedComposition.collectAsStateWithLifecycle()
    val isAspect by viewModel.aspectStyle.collectAsStateWithLifecycle()
    val generateState by viewModel.generateState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        item {
            Text(
                text = "1. DEFINE YOUR SUBJECT OR BASE IDEA",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Stylized Dark Box Input field
            OutlinedTextField(
                value = keywords,
                onValueChange = { viewModel.onKeywordsChanged(it) },
                placeholder = {
                    Text(
                        text = "Describe your creative visual idea... (e.g. 'cyberpunk cat drinking boba under glowing holographic neon streetlamps')",
                        color = Color(0xFF5E6D8A),
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFF090D18), RoundedCornerShape(12.dp))
                    .testTag("keywords_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WhitePure,
                    unfocusedTextColor = WhitePure,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CardBorderColor,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Presets Selection Block
            Text(
                text = "2. CHOOSE ARTISTIC PARAMETERS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // STYLE DIALS Picker
            StyleSelectorDialRow(
                title = "Art Style",
                items = viewModel.styles,
                selectedValue = isStyle,
                onSelected = { viewModel.onStyleSelected(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // LIGHTING DIALS Picker
            StyleSelectorDialRow(
                title = "Lighting Setup",
                items = viewModel.lightings,
                selectedValue = isLighting,
                onSelected = { viewModel.onLightingSelected(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // COMPOSITION DIALS Picker
            StyleSelectorDialRow(
                title = "Composition / Camera",
                items = viewModel.compositions,
                selectedValue = isComposition,
                onSelected = { viewModel.onCompositionSelected(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ASPECT RATIO SELECTION Row
            Text(
                text = "Aspect Ratio (Dimensions)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = DarkTextSecondary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.aspectRatios) { ratio ->
                    val selected = isAspect == ratio
                    val borderB = if (selected) NeonCyan else CardBorderColor
                    val backgroundB = if (selected) Color(0x2400E5FF) else Color(0xFF090D18)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(backgroundB)
                            .border(1.dp, borderB, RoundedCornerShape(8.dp))
                            .clickable { viewModel.onAspectSelected(ratio) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ratio,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selected) NeonCyan else WhitePure,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Generating / Run Button
            Button(
                onClick = { viewModel.buildCustomPrompt() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_prompt_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(12.dp),
                enabled = generateState !is UiState.Loading
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(NeonBlue, NeonPurple))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (generateState is UiState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ARCHITECT DETAILS",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Output blueprint rendering area
            AnimatedVisibility(
                visible = generateState !is UiState.Idle,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                when (val state = generateState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBg, RoundedCornerShape(14.dp))
                                .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "CALCULATING BLUEPRINT VARIABLES...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Instructing LLM model to compile beautiful visual details",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DarkTextSecondary
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    is UiState.Success -> {
                        BlueprintOutputDocCard(savedPrompt = state.data, context = context)
                    }

                    is UiState.Error -> {
                        PromptErrorNoticeCard(message = state.message, onRetry = { viewModel.buildCustomPrompt() })
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun StyleSelectorDialRow(
    title: String,
    items: List<String>,
    selectedValue: String,
    onSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = DarkTextSecondary,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { item ->
                val selected = selectedValue == item
                val borderB = if (selected) NeonCyan else CardBorderColor
                val backgroundB = if (selected) Color(0x1F00E5FF) else Color(0xFF090D18)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(backgroundB)
                        .border(1.dp, borderB, RoundedCornerShape(30.dp))
                        .clickable { onSelected(item) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) NeonCyan else WhitePure
                        )
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: METADATA & IMAGE ANALYZER
// ==========================================

@Composable
fun ImageAnalyzerView(viewModel: PromptViewModel, context: Context) {
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val analyzeState by viewModel.analyzeState.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectImage(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        item {
            Text(
                text = "REVERSE ENGINEER AI ART blueprint",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "Select any AI-generated image or artwork from your device. Gemini will scan and analyze the lighting, medium, camera composition, and structure to write an accurate generator prompt & a step-by-step guide explaining how to reproduce the artwork.",
                fontSize = 12.sp,
                color = DarkTextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Dotted Image Upload Drafting Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0A0E1A))
                    .drawBehind {
                        val stroke = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        drawRoundRect(
                            color = CardBorderColor,
                            style = stroke
                        )
                    }
                    .clickable {
                        filePickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                    .testTag("image_upload_target"),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Art To Analyze",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Overlapping banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x80000000))
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ready for visual blueprint reverse engineering",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "TAP TO REPLACE",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload Icon",
                            tint = NeonCyan,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 12.dp)
                        )
                        Text(
                            text = "LOAD IMAGE FILE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = WhitePure,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = "Supports local gallery selection & image uploads",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = DarkTextSecondary
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = { viewModel.analyzeImage() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("analyze_image_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(12.dp),
                enabled = analyzeState !is UiState.Loading && selectedImageUri != null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (selectedImageUri != null) {
                                Brush.horizontalGradient(listOf(Color(0xFF00C853), Color(0xFF00E5FF)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF1B233A), Color(0xFF1B233A)))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (analyzeState is UiState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = if (selectedImageUri != null) Color.White else DarkTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REVERSE ENGINEER BLUEPRINT",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (selectedImageUri != null) Color.White else DarkTextSecondary,
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Analysis outcome display
            AnimatedVisibility(
                visible = analyzeState !is UiState.Idle,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                when (val state = analyzeState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBg, RoundedCornerShape(14.dp))
                                .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "SCANNING AESTHETICS & LIGHTING...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Running deep visual Gemini modal scanning metrics",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DarkTextSecondary
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    is UiState.Success -> {
                        BlueprintOutputDocCard(savedPrompt = state.data, context = context)
                    }

                    is UiState.Error -> {
                        PromptErrorNoticeCard(message = state.message, onRetry = { viewModel.analyzeImage() })
                    }
                    else -> {}
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: SAVED COLLECTION LIBRARY
// ==========================================

@Composable
fun SavedCollectionLibraryView(
    savedPrompts: List<SavedPrompt>,
    onDelete: (Int) -> Unit,
    onSelect: (SavedPrompt) -> Unit
) {
    if (savedPrompts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Empty collection",
                    tint = DarkTextSecondary,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "NO BLUEPRINTS ARCHIVED YET",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Architect custom prompt blueprints or reverse-engineer active image templates to populate your catalog.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = DarkTextSecondary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "ARCHIVED PROMPT ARCHITECTURE (${savedPrompts.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(savedPrompts) { blueprint ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(blueprint) }
                        .testTag("saved_card_${blueprint.id}"),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, CardBorderColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Image Thumbnail (if analyzed from Uri)
                        if (blueprint.imagePath != null) {
                            val f = File(blueprint.imagePath)
                            if (f.exists()) {
                                AsyncImage(
                                    model = f,
                                    contentDescription = "Analyzed Artwork Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
                                )
                            } else {
                                EmptyThumbnailBox()
                            }
                        } else {
                            // Custom Generated Icon Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x1F8B5CF6))
                                    .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = blueprint.title.uppercase(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (blueprint.isAnalyzed) Color(0xFF00E5FF) else NeonPurple,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = formatTimestamp(blueprint.timestamp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DarkTextSecondary,
                                        fontSize = 9.sp
                                    )
                                )
                            }

                            Text(
                                text = blueprint.prompt,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = WhitePure,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            // Tag Indicators row
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VisualMiniLabel(text = blueprint.style)
                                VisualMiniLabel(text = blueprint.lighting)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Delete Call Action
                        IconButton(
                            onClick = { onDelete(blueprint.id) },
                            modifier = Modifier.testTag("delete_blueprint_${blueprint.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Draft",
                                tint = Color(0xFFE57373),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyThumbnailBox() {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF090D18))
            .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = DarkTextSecondary
        )
    }
}

@Composable
fun VisualMiniLabel(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFF0C101C), RoundedCornerShape(4.dp))
            .border(1.dp, CardBorderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                color = DarkTextSecondary,
                fontFamily = FontFamily.SansSerif
            )
        )
    }
}

// ==========================================
// CORE COMPONENT: DRAFT / BLUEPRINT DOC CARD
// ==========================================

@Composable
fun BlueprintOutputDocCard(savedPrompt: SavedPrompt, context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("blueprint_document_card"),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, NeonCyan),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Blueprint Header Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = NeonCyan,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = "ARCHITECTURAL BLUEPRINT:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0x2E00C853), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SAVED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reconstructed Full Prompt
            Text(
                text = "IMAGE GENERATOR PROMPT",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = DarkTextSecondary,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(Color(0xFF080D18), RoundedCornerShape(10.dp))
                    .border(1.dp, CardBorderColor, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = savedPrompt.prompt,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = WhitePure,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.testTag("blueprint_prompt_text")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            copyToClipboard(context, savedPrompt.prompt)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x2EFFB300)),
                        border = BorderStroke(1.dp, OrangeGold),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .height(32.dp)
                            .testTag("copy_prompt_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = OrangeGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Copy Prompt",
                                color = OrangeGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Negative details
            if (savedPrompt.negativePrompt.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "NEGATIVE PROMPT SUGGESTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DarkTextSecondary,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = savedPrompt.negativePrompt,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE57373),
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meta tags row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = "AESTHETIC STYLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DarkTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = savedPrompt.style,
                            color = WhitePure,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = "LIGHTING TYPE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DarkTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = savedPrompt.lighting,
                            color = WhitePure,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = CardBorderColor, thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            // RECREATION STEP BY STEP GUIDE
            Text(
                text = "STEP-BY-STEP RECREATION GUIDE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NeonCyan,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            )

            Text(
                text = savedPrompt.stepByStepGuide,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = WhitePure,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("blueprint_guide_text")
            )
        }
    }
}

// ==========================================
// CORE COMPONENT: BLUEPRINT DETAILED MODAL DIALOG
// ==========================================

@Composable
fun PromptDetailDialog(
    prompt: SavedPrompt,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, if (prompt.isAnalyzed) NeonCyan else NeonPurple),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (prompt.isAnalyzed) NeonCyan else NeonPurple,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 4.dp)
                        )
                        Text(
                            text = if (prompt.isAnalyzed) "VISUAL ANALYSIS" else "BUILT BLUEPRINT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (prompt.isAnalyzed) NeonCyan else NeonPurple,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Blueprint details",
                            tint = DarkTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Blueprint document details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Original/Analyzed Image Preview in detail dialog
                    if (prompt.imagePath != null) {
                        val f = File(prompt.imagePath)
                        if (f.exists()) {
                            AsyncImage(
                                model = f,
                                contentDescription = "Original Analyzed Picture Source",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Reconstructed Prompt Body
                    Text(
                        text = "GENERATOR PROMPT DESIGN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DarkTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(Color(0xFF080D18), RoundedCornerShape(10.dp))
                            .border(1.dp, CardBorderColor, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = prompt.prompt,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = WhitePure,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { onCopy(prompt.prompt) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x2EFFB300)),
                                border = BorderStroke(1.dp, OrangeGold),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .height(32.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = OrangeGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Copy Prompt",
                                        color = OrangeGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (prompt.negativePrompt.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NEGATIVE ELEMENTS SUGGESTED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DarkTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = prompt.negativePrompt,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE57373),
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STYLE MEDIUM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DarkTextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = prompt.style,
                                color = WhitePure,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIGHTING ENGINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DarkTextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = prompt.lighting,
                                color = WhitePure,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "CAMERA COMPOSITION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DarkTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = prompt.composition,
                            color = WhitePure,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = CardBorderColor)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Recreation Guide
                    Text(
                        text = "STEP-BY-STEP RECREATION MANUAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    )

                    Text(
                        text = prompt.stepByStepGuide,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = WhitePure,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283F)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "CLOSE BLUEPRINT RECORD",
                        color = WhitePure,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// ERROR AND NOTIFICATION SYSTEM CARD
// ==========================================

@Composable
fun PromptErrorNoticeCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1318)),
        border = BorderStroke(1.dp, Color(0xFFE57373)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Error detail",
                    tint = Color(0xFFEF5350)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENGINE TRANSLATION ERROR",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFFEF5350),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFEF9A9A),
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Retry Calculation", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// STATIC HELPERS
// ==========================================

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("AI Prompt Blueprint", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Prompt Blueprint copied to Clipboard!", Toast.LENGTH_SHORT).show()
}

private fun formatTimestamp(timeMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.format(Date(timeMs))
    } catch (e: Exception) {
        "Just now"
    }
}
