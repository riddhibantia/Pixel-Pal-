package com.pixelpal.app.domain.model

import com.pixelpal.app.data.remote.GeminiAgentConnector
import com.pixelpal.app.data.remote.GenericHttpAgentConnector
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentConnectionLogicTest {

    @Test
    fun agentState_blankIsDisconnected() {
        assertEquals(AgentState.DISCONNECTED, AgentState.fromId(""))
        assertEquals(AgentState.DISCONNECTED, AgentState.fromId("   "))
    }

    @Test
    fun agentState_legacyAliases() {
        assertEquals(AgentState.ERROR, AgentState.fromId("FAILED"))
        assertEquals(AgentState.ERROR, AgentState.fromId("failed"))
        assertEquals(AgentState.IDLE, AgentState.fromId("STOPPED"))
    }

    @Test
    fun agentState_knownValuesCaseInsensitive() {
        assertEquals(AgentState.WORKING, AgentState.fromId("WORKING"))
        assertEquals(AgentState.ONLINE, AgentState.fromId("online"))
        assertEquals(AgentState.WAITING_FOR_INPUT, AgentState.fromId("waiting_for_input"))
    }

    @Test
    fun agentState_unknownNonBlankIsError() {
        // Typos must surface as errors (needsAttention), never silent "disconnected".
        assertEquals(AgentState.ERROR, AgentState.fromId("WORKNG"))
        assertEquals(AgentState.ERROR, AgentState.fromId("???"))
    }

    @Test
    fun providers_normalize() {
        assertEquals(AgentProviders.GENERIC, AgentProviders.normalize(""))
        assertEquals(AgentProviders.GENERIC, AgentProviders.normalize("unknown-thing"))
        assertEquals(AgentProviders.GEMINI, AgentProviders.normalize("gemini"))
        assertEquals(AgentProviders.GEMINI, AgentProviders.normalize("  GEMINI "))
        assertEquals(AgentProviders.WEBSOCKET, AgentProviders.normalize("websocket"))
        assertEquals(AgentProviders.WEBSOCKET, AgentProviders.normalize("ws"))
    }

    @Test
    fun providers_displayNames() {
        assertEquals("Generic HTTP", AgentProviders.displayName("generic"))
        assertEquals("Gemini AI", AgentProviders.displayName("gemini"))
        assertEquals("WebSocket", AgentProviders.displayName("ws"))
    }

    @Test
    fun connection_derivedFlags() {
        val gemini = AgentConnection(companionId = 1, provider = "gemini")
        assertTrue(gemini.isGemini)
        assertFalse(gemini.requiresEndpoint)

        val generic = AgentConnection(companionId = 1, provider = "", endpointUrl = "https://x/status")
        assertFalse(generic.isGemini)
        assertTrue(generic.requiresEndpoint)
    }

    @Test
    fun http_allowlist() {
        assertTrue(GenericHttpAgentConnector.isAllowedEndpoint("https://example.com/status"))
        assertTrue(GenericHttpAgentConnector.isAllowedEndpoint("http://127.0.0.1:8765/status"))
        assertTrue(GenericHttpAgentConnector.isAllowedEndpoint("http://10.0.2.2:8765/status"))
        assertTrue(GenericHttpAgentConnector.isAllowedEndpoint("http://localhost:8765/status"))
        assertFalse(GenericHttpAgentConnector.isAllowedEndpoint("http://example.com/status"))
        assertFalse(GenericHttpAgentConnector.isAllowedEndpoint(""))
    }

    @Test
    fun websocket_schemeDetection() {
        assertTrue(GenericHttpAgentConnector.isWebSocketUrl("ws://example.com/agent"))
        assertTrue(GenericHttpAgentConnector.isWebSocketUrl("wss://example.com/agent"))
        assertFalse(GenericHttpAgentConnector.isWebSocketUrl("https://example.com/status"))
        assertFalse(GenericHttpAgentConnector.isWebSocketUrl(""))
    }

    @Test
    fun gemini_keyValidation() {
        assertFalse(GeminiAgentConnector.isUsableKey(""))
        assertFalse(GeminiAgentConnector.isUsableKey("   "))
        assertFalse(GeminiAgentConnector.isUsableKey("YOUR_GEMINI_API_KEY_HERE"))
        assertFalse(GeminiAgentConnector.isUsableKey("short-key"))
        assertTrue(GeminiAgentConnector.isUsableKey("AIzaSyD-abcdefghijklmnopqrstuvwxyz123"))
    }
}
