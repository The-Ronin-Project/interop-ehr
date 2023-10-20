package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicServiceRequestServiceTest {
    private val epicClient = mockk<EpicClient>()
    private val service = EpicServiceRequestService(epicClient, 10)
    private val tenant = createTestTenant()
    private val validServiceRequestBundle = readResource<Bundle>("/ExampleServiceRequestBundle.json")

    @Test
    fun `getById - works`() {
        val serviceRequest = mockk<ServiceRequest>(relaxed = true) {
            every { id!!.value } returns "12345"
        }
        val mockedResponse: EHRResponse = mockk()
        coEvery { mockedResponse.body(any()) } returns serviceRequest

        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/ServiceRequest/12345"
            )
        } returns mockedResponse

        val response = service.getByID(tenant, "12345")

        assertEquals(serviceRequest.id, response.id)
    }

    @Test
    fun `getByPatient - works`() {
        val httpResponse = mockk<HttpResponse> {
            every { status } returns HttpStatusCode.OK
        }
        coEvery { httpResponse.body<Bundle>() } returns validServiceRequestBundle

        val ehrResponse = EHRResponse(httpResponse, "12345")
        coEvery {
            epicClient.get(
                tenant,
                "/api/FHIR/R4/ServiceRequest",
                mapOf(
                    "patient" to "patty",
                    "_count" to 50
                )
            )
        } returns ehrResponse

        val response = service.getServiceRequestsForPatient(tenant, "patty")

        assertEquals(14, response.size)
    }
}
