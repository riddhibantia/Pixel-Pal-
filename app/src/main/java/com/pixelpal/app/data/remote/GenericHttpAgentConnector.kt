package com.pixelpal.app.data.remote

import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic connector that polls a user-configured JSON status endpoint.
 * HTTPS is enforced; cleartext only for loopback (127.0.0.1/10.0.2.2/localhost)
 * which is allowed by network_security_config.
 */
@Singleton
class GenericHttpAgentConnector @Inject constructor(
    private val client: OkHttpClient
) : AgentConnector {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun checkNow(endpointUrl: String): AgentCheckResult {
        if (endpointUrl.isBlank()) {
            return AgentCheckResult(AgentState.DISCONNECTED, "No endpoint configured")
        }
        if (!isAllowedEndpoint(endpointUrl)) {
            return AgentCheckResult(AgentState.ERROR, "HTTPS required (cleartext only for localhost)")
        }
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(endpointUrl).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        AgentCheckResult(AgentState.ERROR, "HTTP ${response.code}")
                    } else {
                        val body = response.body?.string().orEmpty()
                        parseEnvelope(body)
                            // Malformed JSON is a server/config bug, not "offline".
                            ?: AgentCheckResult(AgentState.ERROR, "Unrecognized response — expected {status, message?, currentTask?, progress?}")
                    }
                }
            } catch (e: IOException) {
                Timber.d(e, "Agent status check failed")
                AgentCheckResult(AgentState.OFFLINE, e.message ?: "Network error")
            }
        }
    }

    companion object {
        /** Shared allowlist: HTTPS everywhere, cleartext only for loopback dev. */
        fun isAllowedEndpoint(url: String): Boolean {
            val lower = url.trim().lowercase()
            if (lower.startsWith("https://")) return true
            // Allow cleartext only for local dev
            return lower.startsWith("http://127.0.0.1") ||
                lower.startsWith("http://10.0.2.2") ||
                lower.startsWith("http://localhost")
        }

        fun isWebSocketUrl(url: String): Boolean {
            val lower = url.trim().lowercase()
            return lower.startsWith("ws://") || lower.startsWith("wss://")
        }
    }

    private fun isAllowedScheme(url: String): Boolean = isAllowedEndpoint(url)

    private fun parseEnvelope(body: String): AgentCheckResult? {
        return try {
            val envelope = json.decodeFromString<AgentEnvelope>(body)
            val progress = envelope.progress?.takeIf { it in 0..100 }
            AgentCheckResult(
                state = AgentState.fromId(envelope.status),
                message = envelope.message,
                currentTask = envelope.currentTask?.takeIf { it.isNotBlank() },
                progress = progress
            )
        } catch (e: Exception) {
            Timber.d(e, "Failed to parse agent envelope")
            null
        }
    }

    @Serializable
    private data class AgentEnvelope(
        val status: String = "",
        val message: String? = null,
        val currentTask: String? = null,
        val progress: Int? = null
    )
}