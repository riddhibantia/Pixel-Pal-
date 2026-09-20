package com.pixelpal.app.data.remote

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ApprovalEnvelopeTest {

    private val connector = GenericHttpAgentConnector(OkHttpClient())

    @Test
    fun envelope_withApproval_parsesPendingApproval() {
        val body = """
            {"status":"WORKING","currentTask":"cleanup","progress":50,
             "pendingApproval":{"id":"a1","action":"delete 40 files","detail":"tmp only"}}
        """.trimIndent()
        val result = GenericHttpAgentConnector.parseForTest(connector, body)
        assertNotNull(result)
        assertEquals("WORKING", result!!.state.id)
        assertEquals("a1", result.pendingApproval?.id)
        assertEquals("delete 40 files", result.pendingApproval?.action)
        assertEquals("tmp only", result.pendingApproval?.detail)
    }

    @Test
    fun envelope_withoutApproval_hasNullPendingApproval() {
        val body = """{"status":"IDLE","message":"all quiet"}"""
        val result = GenericHttpAgentConnector.parseForTest(connector, body)
        assertNotNull(result)
        assertNull(result!!.pendingApproval)
    }

    @Test
    fun envelope_withBlankApprovalId_dropsApproval() {
        val body = """{"status":"WORKING","pendingApproval":{"id":"","action":"rm -rf"}}"""
        val result = GenericHttpAgentConnector.parseForTest(connector, body)
        assertNotNull(result)
        assertNull(result!!.pendingApproval)
    }
}
