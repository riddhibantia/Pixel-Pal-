package com.pixelpal.app.data.remote

import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentConfig
import com.pixelpal.app.domain.model.AgentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic connector that polls a user-configured JSON status endpoint.
 *
 * The endpoint is expected to return an envelope like:
 *   { "status": "WORKING", "message": "processing request #42" }
 *
 * No API keys or credentials are ever sent or stored. Network failures map to
 * [AgentState.OFFLINE]; non-2xx responses map to [AgentState.FAILED].
 */
@Singleton
class GenericHttpAgentConnector @Inject constructor() : AgentConnector {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun checkStatus(config: AgentConfig): AgentCheckResult {
        if (config.endpointUrl.isBlank()) {
            return AgentCheckResult(AgentState.OFFLINE, "No endpoint configured")
        }
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(config.endpointUrl).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        AgentCheckResult(AgentState.FAILED, "HTTP ${response.code}")
                    } else {
                        val body = response.body?.string().orEmpty()
                        parseEnvelope(body)
                            ?: AgentCheckResult(AgentState.OFFLINE, "Unrecognized response")
                    }
                }
            } catch (e: IOException) {
                Timber.d(e, "Agent status check failed for ${config.companionId}")
                AgentCheckResult(AgentState.OFFLINE, e.message ?: "Network error")
            }
        }
    }

    private fun parseEnvelope(body: String): AgentCheckResult? {
        return try {
            val envelope = json.decodeFromString<AgentEnvelope>(body)
            AgentCheckResult(
                state = AgentState.fromId(envelope.status),
                message = envelope.message
            )
        } catch (e: Exception) {
            Timber.d(e, "Failed to parse agent envelope")
            null
        }
    }

    @Serializable
    private data class AgentEnvelope(
        val status: String = "",
        val message: String? = null
    )
}