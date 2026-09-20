package com.pixelpal.app.data.remote

import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentState
import com.pixelpal.app.domain.model.PendingApproval
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
 * and private LAN (RFC1918) for self-hosted agent pairing, as allowed by
 * network_security_config.
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
            return AgentCheckResult(AgentState.ERROR, "HTTPS required (cleartext only for localhost or private LAN)")
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
        /**
         * Shared allowlist: HTTPS everywhere; cleartext only for loopback dev
         * and private LAN (RFC1918) so a self-hosted agent on your own
         * network can pair over plain HTTP like other companion-node apps.
         * Never paired over public cleartext HTTP.
         */
        fun isAllowedEndpoint(url: String): Boolean {
            val lower = url.trim().lowercase()
            if (lower.startsWith("https://")) return true
            // Allow cleartext only for local dev
            return lower.startsWith("http://127.0.0.1") ||
                lower.startsWith("http://10.0.2.2") ||
                lower.startsWith("http://localhost") ||
                isPrivateLanHttp(lower)
        }

        /** http:// + RFC1918 (10/8, 172.16/12, 192.168/16), optional :port. */
        fun isPrivateLanHttp(lowerTrimmedUrl: String): Boolean {
            if (!lowerTrimmedUrl.startsWith("http://")) return false
            val host = lowerTrimmedUrl.removePrefix("http://")
                .substringBefore("/")
                .substringBefore("@")
                .substringAfterLast("@")
                .substringBefore(":")
            if (host == "localhost") return true
            val parts = host.split(".")
            if (parts.size != 4) return false
            val octets = parts.map { it.toIntOrNull() ?: return false }
            if (octets.any { it !in 0..255 }) return false
            return when {
                octets[0] == 10 -> true
                octets[0] == 172 && octets[1] in 16..31 -> true
                octets[0] == 192 && octets[1] == 168 -> true
                else -> false
            }
        }

        fun isWebSocketUrl(url: String): Boolean {
            val lower = url.trim().lowercase()
            return lower.startsWith("ws://") || lower.startsWith("wss://")
        }

        /**
         * Test seam: parses a raw envelope without network. Visible for unit
         * tests in the same module (app/src/test/.../data/remote/).
         */
        internal fun parseForTest(connector: GenericHttpAgentConnector, body: String): AgentCheckResult? =
            connector.parseEnvelope(body)
    }

    private fun isAllowedScheme(url: String): Boolean = isAllowedEndpoint(url)

    private fun parseEnvelope(body: String): AgentCheckResult? {
        return try {
            val envelope = json.decodeFromString<AgentEnvelope>(body)
            val progress = envelope.progress?.takeIf { it in 0..100 }
            val approval = envelope.pendingApproval?.takeIf { it.id.isNotBlank() && it.action.isNotBlank() }
                ?.let { PendingApproval(it.id, it.action, it.detail?.takeIf { d -> d.isNotBlank() }) }
            AgentCheckResult(
                state = AgentState.fromId(envelope.status),
                message = envelope.message,
                currentTask = envelope.currentTask?.takeIf { it.isNotBlank() },
                progress = progress,
                pendingApproval = approval
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
        val progress: Int? = null,
        val pendingApproval: PendingApproval? = null
    )
}