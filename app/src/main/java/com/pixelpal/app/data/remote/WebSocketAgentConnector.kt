package com.pixelpal.app.data.remote

import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time streaming WebSocket connector for live AI agent interactions and progress notifications.
 * Reuses the shared OkHttp pool (newBuilder) with an infinite read timeout for long-lived sockets.
 */
@Singleton
class WebSocketAgentConnector @Inject constructor(
    sharedClient: OkHttpClient
) {

    private val client: OkHttpClient = sharedClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Infinite timeout for long-lived WebSockets
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private var activeWebSocket: WebSocket? = null

    /**
     * Connects to a WebSocket endpoint and streams real-time updates as a Kotlin Flow.
     */
    fun connectAndStream(url: String): Flow<AgentCheckResult> = callbackFlow {
        val request = Request.Builder().url(url).build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket connected: $url")
                trySend(AgentCheckResult(AgentState.ONLINE, "Connected"))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val envelope = json.decodeFromString<WsAgentMessage>(text)
                    trySend(
                        AgentCheckResult(
                            state = AgentState.fromId(envelope.status),
                            message = envelope.message,
                            currentTask = envelope.currentTask,
                            progress = envelope.progress
                        )
                    )
                } catch (e: Exception) {
                    Timber.d(e, "Unrecognized WebSocket message: $text")
                    trySend(AgentCheckResult(AgentState.ONLINE, text))
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closing: $code / $reason")
                trySend(AgentCheckResult(AgentState.DISCONNECTED, reason))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "WebSocket error")
                trySend(AgentCheckResult(AgentState.ERROR, t.localizedMessage ?: "WebSocket error"))
            }
        }

        val ws = client.newWebSocket(request, listener)
        activeWebSocket = ws

        awaitClose {
            ws.close(1000, "Client closed connection")
            if (activeWebSocket == ws) activeWebSocket = null
        }
    }

    /**
     * Sends a message over the active WebSocket channel.
     */
    fun sendMessage(message: String): Boolean {
        return activeWebSocket?.send(message) ?: false
    }

    /** True while a socket opened by [connectAndStream] is still alive. */
    val isConnected: Boolean get() = activeWebSocket != null

    /** Closes the active socket, if any (e.g. on provider switch / disconnect). */
    fun disconnect(code: Int = 1000, reason: String = "Client closed connection") {
        try {
            activeWebSocket?.close(code, reason)
        } catch (_: Exception) {
        } finally {
            activeWebSocket = null
        }
    }

    /**
     * One-shot poll for ws:// / wss:// endpoints so periodic WorkManager checks
     * and "Check now" work without holding a Flow open. Waits for the first
     * meaningful message; falls back to ONLINE when the socket opens but the
     * server sends nothing further within the timeout.
     */
    suspend fun checkOnce(url: String, timeoutMs: Long = 12_000L): AgentCheckResult {
        if (!GenericHttpAgentConnector.isWebSocketUrl(url)) {
            return AgentCheckResult(AgentState.ERROR, "WebSocket URL must start with ws:// or wss://")
        }
        return try {
            val live = withTimeoutOrNull(timeoutMs) {
                connectAndStream(url).first { r ->
                    !(r.state == AgentState.ONLINE && r.message == "Connected")
                }
            }
            live ?: AgentCheckResult(AgentState.ONLINE, "Connected (no status yet)")
        } catch (e: Exception) {
            Timber.e(e, "WebSocket check failed")
            AgentCheckResult(AgentState.ERROR, e.localizedMessage ?: "WebSocket error")
        } finally {
            disconnect()
        }
    }

    @Serializable
    private data class WsAgentMessage(
        val status: String = "",
        val message: String? = null,
        val currentTask: String? = null,
        val progress: Int? = null
    )
}
