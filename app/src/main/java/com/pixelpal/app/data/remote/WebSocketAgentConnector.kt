package com.pixelpal.app.data.remote

import com.pixelpal.app.domain.model.AgentCheckResult
import com.pixelpal.app.domain.model.AgentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
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
 */
@Singleton
class WebSocketAgentConnector @Inject constructor() {

    private val client = OkHttpClient.Builder()
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

    @Serializable
    private data class WsAgentMessage(
        val status: String = "",
        val message: String? = null,
        val currentTask: String? = null,
        val progress: Int? = null
    )
}
