package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.stu3.resource.STU3Bundle
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EpicMedicationStatementTest {
    private lateinit var tenant: Tenant
    private lateinit var epicClient: EpicClient
    private lateinit var medicationStatementService: EpicMedicationStatementService
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private lateinit var pagingHttpResponse: HttpResponse
    private val searchUrlPart = "/api/FHIR/STU3/MedicationStatement"

    @BeforeEach
    fun setup() {
        tenant = createTestTenant(
            "clientId",
            "https://example.org",
            "testPrivateKey",
            "tenantId"
        )
        epicClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
        pagingHttpResponse = mockk()
        medicationStatementService = EpicMedicationStatementService(epicClient)
    }

    @Test
    fun `some medication statements are returned`() {
        val validMedicationStatementSearch = readResource<STU3Bundle>("/STU3MedicationStatementBundle.json")
        val validMedicationStatement1 = readResource<MedicationStatement>("/ExampleMedicationStatement1.json")
        val validMedicationStatement2 = readResource<MedicationStatement>("/ExampleMedicationStatement2.json")
        val validMedicationStatement3 = readResource<MedicationStatement>("/ExampleMedicationStatement3.json")
        val validMedicationStatementList = listOf(
            validMedicationStatement1,
            validMedicationStatement2,
            validMedicationStatement3
        )
        val patientFhirId = "abc"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns validMedicationStatementSearch
        coEvery {
            epicClient.get(
                tenant,
                searchUrlPart,
                mapOf("patient" to patientFhirId, "_count" to 50)
            )
        } returns ehrResponse

        val medicationStatements = medicationStatementService.getMedicationStatementsByPatientFHIRId(
            tenant,
            patientFhirId
        )

        assertEquals(validMedicationStatementList, medicationStatements)
    }

    @Test
    fun `0 medication statements are returned`() {
        val emptyMedicationStatementSearch = readResource<STU3Bundle>("/STU3BundleEmpty.json")
        val patientFhirId = "abc"

        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<STU3Bundle>() } returns emptyMedicationStatementSearch
        coEvery {
            epicClient.get(
                tenant,
                searchUrlPart,
                mapOf("patient" to patientFhirId, "_count" to 50)
            )
        } returns ehrResponse

        val medicationStatements = medicationStatementService.getMedicationStatementsByPatientFHIRId(
            tenant,
            patientFhirId
        )

        assertEquals(emptyList<MedicationStatement>(), medicationStatements)
    }
}
