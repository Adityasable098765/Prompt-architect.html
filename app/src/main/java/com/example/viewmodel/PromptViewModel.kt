package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.ImageAnalysisResult
import com.example.api.InlineData
import com.example.api.Part
import com.example.api.PromptGenerationResult
import com.example.api.RetrofitClient
import com.example.data.PromptRepository
import com.example.data.SavedPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class PromptViewModel(
    private val repository: PromptRepository,
    private val appContext: Context
) : ViewModel() {

    private val tag = "PromptViewModel"

    val allPrompts: StateFlow<List<SavedPrompt>> = repository.allPrompts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val generateState = MutableStateFlow<UiState<SavedPrompt>>(UiState.Idle)
    val analyzeState = MutableStateFlow<UiState<SavedPrompt>>(UiState.Idle)

    // Form inputs for Custom Creator
    val inputKeywords = MutableStateFlow("")
    val selectedStyle = MutableStateFlow("Cinematic")
    val selectedLighting = MutableStateFlow("Golden Hour")
    val selectedComposition = MutableStateFlow("Dynamic Medium Shot")
    val aspectStyle = MutableStateFlow("16:9")

    // Image Picker Selection
    val selectedImageUri = MutableStateFlow<Uri?>(null)

    // Preset options
    val styles = listOf(
        "Cinematic", "Photographic", "3D Render", "Anime / Manga", 
        "Cyberpunk Concept", "Surrealism", "Oil Painting", "Vector Minimalism", "Fantasy Illustration"
    )
    val lightings = listOf(
        "Golden Hour", "Cyberpunk Neon", "Dramatic Chiaroscuro", 
        "Volumetric Sunlight", "Misty Moonlight", "High-Key Studio", "Soft Ambient"
    )
    val compositions = listOf(
        "Dynamic Medium Shot", "Extreme Close-Up", "Epic Wide-Angle", 
        "Bird's Eye Aerial", "Low-Angle Hero Shot", "Rule of Thirds Portrait"
    )
    val aspectRatios = listOf("1:1", "16:9", "4:5", "2:3", "9:16")

    fun onKeywordsChanged(text: String) {
        inputKeywords.value = text
    }

    fun onStyleSelected(style: String) {
        selectedStyle.value = style
    }

    fun onLightingSelected(lighting: String) {
        selectedLighting.value = lighting
    }

    fun onCompositionSelected(composition: String) {
        selectedComposition.value = composition
    }

    fun onAspectSelected(ratio: String) {
        aspectStyle.value = ratio
    }

    fun selectImage(uri: Uri?) {
        selectedImageUri.value = uri
        analyzeState.value = UiState.Idle
    }

    fun clearStates() {
        generateState.value = UiState.Idle
        analyzeState.value = UiState.Idle
    }

    fun deletePrompt(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    /**
     * Builds a custom prompt based on selected parameters + keywords using Gemini 3.5-flash
     */
    fun buildCustomPrompt() {
        val keywords = inputKeywords.value.trim()
        if (keywords.isEmpty()) {
            generateState.value = UiState.Error("Please enter some description keywords or creative idea first!")
            return
        }

        generateState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    generateState.value = UiState.Error(
                        "Gemini API key is not configured. Please add your GEMINI_API_KEY into the Secrets panel in Google AI Studio to unlock full capabilities."
                    )
                    return@launch
                }

                val promptPrompt = """
                    Act as an Elite AI Image Prompt Engineer.
                    Take the following user request and expand it into a high-quality, professional image prompt for generation tools (like Midjourney, DALL-E, or Stable Diffusion).
                    
                    Inputs:
                    - Subject/Scene Details: $keywords
                    - Target Style Preset: ${selectedStyle.value}
                    - Lighting Setup: ${selectedLighting.value}
                    - Camera/Composition: ${selectedComposition.value}
                    - Aspect Ratio: ${aspectStyle.value}

                    Generate a detailed prompt, a negative prompt, a listing of core visual keywords/highlights, and a structured step-by-step guide on how the user can generate in Midjourney/DALL-E.
                    You MUST return the output strictly in a valid JSON format with NO markdown wrapper outside the JSON object.
                    
                    Expected JSON schema:
                    {
                      "optimizedPrompt": "Full highly detailed prompt string starting with style tags",
                      "negativePrompt": "Comma separated negative prompt suggestions",
                      "featuresHighlight": "Three main highlights of the prompt, separated by bullet points",
                      "stepByStepGuide": "A short markdown format list of 4 steps to customize, render and postprocess"
                    }
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptPrompt)))),
                    generationConfig = GenerationConfig(
                        temperature = 0.7f,
                        responseMimeType = "application/json"
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (textResponse != null) {
                    val cleanedJson = sanitizeJsonText(textResponse)
                    val adapter = RetrofitClient.moshi.adapter(PromptGenerationResult::class.java)
                    val result = withContext(Dispatchers.Default) {
                        adapter.fromJson(cleanedJson)
                    }

                    if (result != null) {
                        val savedPrompt = SavedPrompt(
                            title = "Build: " + keywords.take(20) + if (keywords.length > 20) "..." else "",
                            prompt = "${result.optimizedPrompt} --ar ${aspectStyle.value}",
                            negativePrompt = result.negativePrompt,
                            style = selectedStyle.value,
                            lighting = selectedLighting.value,
                            composition = selectedComposition.value,
                            stepByStepGuide = result.stepByStepGuide + "\n\n**Visual Highlights:**\n" + result.featuresHighlight,
                            isAnalyzed = false
                        )
                        val id = repository.insert(savedPrompt)
                        generateState.value = UiState.Success(savedPrompt.copy(id = id.toInt()))
                    } else {
                        generateState.value = UiState.Error("Failed to decode response structure. Text raw response: $textResponse")
                    }
                } else {
                    generateState.value = UiState.Error("No output generated from Gemini. Please try again.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error building custom prompt", e)
                generateState.value = UiState.Error(e.message ?: "An unknown network error occurred.")
            }
        }
    }

    /**
     * Uploads and Analyzes an existing AI Image to reverse engineer its prompt and get a recreation guide
     */
    fun analyzeImage() {
        val uri = selectedImageUri.value
        if (uri == null) {
            analyzeState.value = UiState.Error("Please select or capture an image first!")
            return
        }

        analyzeState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    analyzeState.value = UiState.Error(
                        "Gemini API key is not configured. Please add your GEMINI_API_KEY into the Secrets panel in Google AI Studio to unlock full capabilities."
                    )
                    return@launch
                }

                // Load and compress Bitmap
                val bitmap = loadAndCompressBitmap(uri)
                if (bitmap == null) {
                    analyzeState.value = UiState.Error("Failed to load or parse the selected image file.")
                    return@launch
                }

                // Copy image to safe local cache to keep persistent reference
                val localPath = withContext(Dispatchers.IO) {
                    repository.saveImageToInternalStorage(appContext, uri)
                }

                val base64Image = withContext(Dispatchers.Default) {
                    bitmap.toBase64()
                }

                val promptPrompt = """
                    You are a world-class AI Image Reverse Engineer and Prompt Architect.
                    Inspect this uploaded image with absolute precision.
                    
                    Reconstruct and reverse engineer the detailed prompt, the underlying style, lighting elements, negative prompts, camera shots, and construct a complete step-by-step guide explaining exactly how a creator can generate this specific aesthetic from scratch.
                    You MUST return the output strictly of JSON with NO markdown wrappers.
                    
                    Expected JSON schema:
                    {
                      "prompt": "Highly descriptive, precise, optimized prompt to recreate this image exactly",
                      "style": "The primary artistic aesthetic, style, or medium (e.g., Photographic, Cinematic, Watercolor, Cyberpunk, 3D Render)",
                      "lighting": "Detailed analysis of lighting and brightness elements (e.g., volumetric rays, neon neon shadows, rim lights)",
                      "composition": "The camera focal depth, shot size, and camera angle used",
                      "negativePrompt": "Extracted negative prompts or things to exclude to look like this image",
                      "stepByStepGuide": "A clear, descriptive step-by-step guide (Markdown formatted) explaining how to construct visual assets like this in Midjourney, DALL-E, or Stable Diffusion"
                    }
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = promptPrompt),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        temperature = 0.4f,
                        responseMimeType = "application/json"
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (textResponse != null) {
                    val cleanedJson = sanitizeJsonText(textResponse)
                    val adapter = RetrofitClient.moshi.adapter(ImageAnalysisResult::class.java)
                    val result = withContext(Dispatchers.Default) {
                        adapter.fromJson(cleanedJson)
                    }

                    if (result != null) {
                        val savedPrompt = SavedPrompt(
                            title = "Analyze: " + result.style + " " + result.prompt.split(" ").take(2).joinToString(" "),
                            prompt = result.prompt,
                            negativePrompt = result.negativePrompt,
                            style = result.style,
                            lighting = result.lighting,
                            composition = result.composition,
                            stepByStepGuide = result.stepByStepGuide,
                            imagePath = localPath,
                            isAnalyzed = true
                        )
                        val id = repository.insert(savedPrompt)
                        analyzeState.value = UiState.Success(savedPrompt.copy(id = id.toInt()))
                    } else {
                        analyzeState.value = UiState.Error("Failed to parse image analysis model response structure. text response: $textResponse")
                    }
                } else {
                    analyzeState.value = UiState.Error("No analysis generated from the visual model. Please try again.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error analyzing image", e)
                analyzeState.value = UiState.Error(e.message ?: "An unknown network or multimodal analysis error occurred.")
            }
        }
    }

    private suspend fun loadAndCompressBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = appContext.contentResolver
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, options)
            }

            // Downsample high-resolution images to match Gemini's optimal input envelope and save bandwidth
            var scale = 1
            val maxDimension = 1024
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                scale = Math.max(options.outHeight / maxDimension, options.outWidth / maxDimension)
            }

            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, loadOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun sanitizeJsonText(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}

class PromptViewModelFactory(
    private val repository: PromptRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PromptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PromptViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
