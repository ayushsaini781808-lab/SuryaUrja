package com.example.data.remote.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "tools") val tools: List<GeminiTool>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    @Json(name = "googleSearch") val googleSearch: Map<String, String>? = null,
    @Json(name = "googleMaps") val googleMaps: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

/**
 * Chat message model surfaced to UI
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val hasSearchGrounding: Boolean = false,
    val hasMapsGrounding: Boolean = false,
    val isError: Boolean = false
)

enum class MessageSender {
    USER,
    GEMINI_ASSISTANT
}

enum class GeminiModel(val modelId: String, val displayName: String, val description: String) {
    FLASH("gemini-3.5-flash", "Gemini 3.5 Flash", "General tasks & Grounding"),
    PRO("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Complex reasoning & Derating math"),
    FLASH_LITE("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", "Fast inference & Quick triage")
}

enum class ChatbotRole(
    val title: String,
    val iconEmoji: String,
    val description: String,
    val systemPrompt: String
) {
    GRID_DISPATCH(
        title = "Grid Dispatch Engineer",
        iconEmoji = "⚡",
        description = "PV yield, peak shaving, and battery storage dispatch",
        systemPrompt = "You are a Senior Solar Energy & Grid Dispatch Engineer for SolarCast. Your role is to advise operators on photovoltaic output, day-ahead dispatch schedules, peak shaving, battery energy storage system (BESS) charging windows, and grid feeder capacity limits. Use technical, precise language with actionable recommendations."
    ),
    ML_ARCHITECT(
        title = "ML Forecasting Architect",
        iconEmoji = "🔬",
        description = "CNN-LSTM spatiotemporal layers and ENN uncertainty",
        systemPrompt = "You are the Machine Learning Architect behind SolarCast's hybrid forecasting engine. You specialize in explaining the 1D/2D CNN feature extractors, bidirectional LSTM temporal gates, and Ensemble Neural Network (ENN) 95% confidence intervals (R² = 0.9991). Explain mathematical equations, loss functions, and data calibration clearly."
    ),
    SITE_ANALYST(
        title = "Geospatial & Site Analyst",
        iconEmoji = "📍",
        description = "Irradiance, clear-sky indexing, and location analytics",
        systemPrompt = "You are a Geospatial Solar Analyst and Meteorological Specialist. You advise on solar zenith angles, Global Horizontal Irradiance (GHI), Direct Normal Irradiance (DNI), panel tilt/azimuth optimization, nearby substations, and geographic weather impact on PV generation."
    )
}
