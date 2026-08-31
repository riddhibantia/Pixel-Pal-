package com.pixelpal.app.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.content
import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct Google Gemini AI integration for conversational intelligence,
 * real-time token streaming, and dynamic companion personality reactions.
 */
@Singleton
class GeminiAgentConnector @Inject constructor() : AgentConnector {

    private var generativeModel: GenerativeModel? = null

    init {
        val defaultKey = com.pixelpal.app.BuildConfig.GEMINI_API_KEY
        if (defaultKey.isNotBlank()) {
            initialize(defaultKey)
        }
    }

    /**
     * Initializes the Gemini Generative Model with the provided API key and model type.
     */
    fun initialize(apiKey: String, modelName: String = "gemini-1.5-flash") {
        if (apiKey.isBlank()) {
            generativeModel = null
            return
        }
        generativeModel = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        )
    }

    /**
     * Standard status check adhering to [AgentConnector].
     */
    override suspend fun checkNow(endpointUrl: String): AgentCheckResult {
        val model = generativeModel
            ?: return AgentCheckResult(AgentState.DISCONNECTED, "Gemini API key not configured")

        return withContext(Dispatchers.IO) {
            try {
                val prompt = "Respond in 5 words with a status check as a digital pet companion."
                val response = model.generateContent(prompt)
                val text = response.text?.trim().orEmpty()
                AgentCheckResult(
                    state = AgentState.ONLINE,
                    message = text.ifBlank { "Ready to assist!" }
                )
            } catch (e: Exception) {
                Timber.e(e, "Gemini status check failed")
                AgentCheckResult(AgentState.ERROR, e.localizedMessage ?: "Gemini API error")
            }
        }
    }

    /**
     * Streams real-time token chunks from Gemini for interactive typewriter responses.
     */
    fun streamChatResponse(
        prompt: String,
        companionName: String,
        personality: String,
        bondLevel: Int
    ): Flow<String> = flow {
        val model = generativeModel
        if (model == null) {
            emit("Error: Gemini is not initialized. Please configure your API key.")
            return@flow
        }

        try {
            val systemInstruction = """
                You are $companionName, a loyal virtual companion with a $personality personality.
                Bond Level: $bondLevel.
                Respond in character. Keep responses concise, warm, helpful, and under 3 sentences.
            """.trimIndent()

            val fullPrompt = "$systemInstruction\n\nUser: $prompt\n$companionName:"
            val responseStream = model.generateContentStream(fullPrompt)
            responseStream.collect { chunk: GenerateContentResponse ->
                chunk.text?.let { emit(it) }
            }
        } catch (e: Exception) {
            Timber.e(e, "Gemini stream error")
            emit("\n[Connection error: ${e.localizedMessage ?: "Unknown error"}]")
        }
    }.flowOn(Dispatchers.IO)
}
