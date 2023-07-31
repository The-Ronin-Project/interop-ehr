package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.http.FhirJson
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Binary
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.http.ContentType
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class CernerBinaryServiceTest {
    private val client: CernerClient = mockk()
    private val tenant: Tenant = mockk()
    private val service: CernerBinaryService = CernerBinaryService(client)

    @Test
    fun `getByID override works`() {
        val mockedResponse: EHRResponse = mockk()
        coEvery { mockedResponse.body(any()) } returns mockk<Binary>()

        coEvery {
            client.get(
                tenant,
                "/Binary/12345",
                acceptTypeOverride = ContentType.Application.FhirJson,
                disableRetry = true
            )
        } returns mockedResponse

        val response = service.getByID(tenant, "12345")
        assertNotNull(response)
    }
}
