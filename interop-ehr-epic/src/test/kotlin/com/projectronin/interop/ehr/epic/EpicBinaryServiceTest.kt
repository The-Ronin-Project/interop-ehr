package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.http.FhirJson
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Binary
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.http.ContentType
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class EpicBinaryServiceTest {
    private val client: EpicClient = mockk()
    private val tenant: Tenant = mockk()
    private val service: EpicBinaryService = EpicBinaryService(client, 2)

    @Test
    fun `getByID override works`() {
        val mockedResponse: EHRResponse = mockk()
        coEvery { mockedResponse.body(any()) } returns mockk<Binary>()

        coEvery {
            client.get(
                tenant,
                "/api/FHIR/R4/Binary/12345",
                acceptTypeOverride = ContentType.Application.FhirJson,
                disableRetry = true,
                timeoutOverride = 2.seconds
            )
        } returns mockedResponse

        val response = service.getByID(tenant, "12345")
        assertNotNull(response)
    }
}
