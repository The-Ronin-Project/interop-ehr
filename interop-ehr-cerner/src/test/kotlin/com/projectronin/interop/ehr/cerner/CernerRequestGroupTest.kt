package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.RequestGroupService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CernerRequestGroupTest {
    private val cernerClient: CernerClient = mockk()
    private val service: RequestGroupService = CernerRequestGroupService(cernerClient)
    private val tenant = createTestTenant()

    @Test
    fun `getRequestGroupByFHIRId returns empty-map`() {
        val response = service.getRequestGroupByFHIRId(tenant, listOf("12345678", "87654321"))
        assertEquals(response.size, 0)
    }
}
