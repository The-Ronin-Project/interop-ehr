package com.projectronin.interop.ehr.epic

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.epic.apporchard.model.PatientFlag
import com.projectronin.interop.ehr.epic.apporchard.model.SetPatientFlagRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SetPatientFlagResponse
import com.projectronin.interop.ehr.epic.apporchard.model.exceptions.AppOrchardError
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EpicOnboardFlagServiceTest {
    private lateinit var onboardFlagService: EpicOnboardFlagService
    private lateinit var epicClient: EpicClient
    private lateinit var identifierService: EpicIdentifierService
    private lateinit var ehrDataAuthorityClient: EHRDataAuthorityClient
    private lateinit var httpResponse: HttpResponse
    private lateinit var ehrResponse: EHRResponse
    private lateinit var tenant: Tenant

    @BeforeEach
    fun initTest() {
        epicClient = mockk()
        httpResponse = mockk()
        ehrResponse = EHRResponse(httpResponse, "12345")
        identifierService = mockk()
        ehrDataAuthorityClient = mockk()
        onboardFlagService = EpicOnboardFlagService(epicClient, identifierService, ehrDataAuthorityClient)
        tenant = createTestTenant(
            "https://example.org",
            patientOnboardedFlagId = "flagType",
            mrnSystem = "oid.123",
            mrnTypeText = "MRN",
            ehrUserId = "ehrUserId"
        )
    }

    @Test
    fun `bad tenant throws error`() {
        tenant = createTestTenant(
            "https://example.org",
            patientOnboardedFlagId = null
        )
        assertThrows<IllegalStateException> {
            onboardFlagService.setOnboardedFlag(tenant, "123")
        }
    }

    @Test
    fun `service setsFlag`() {
        val setPatientResponse = SetPatientFlagResponse(success = true)
        val patientMRN = mockk<Identifier> {
            every { value } returns FHIRString("123")
        }
        val patient = mockk<Patient> {
            every { identifier } returns listOf(patientMRN)
        }
        coEvery { ehrDataAuthorityClient.getResourceAs<Patient>("mnemonic", "Patient", "fhirId".localize(tenant)) } returns patient
        every { identifierService.getMRNIdentifier(tenant, listOf(patientMRN)) } returns patientMRN
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<SetPatientFlagResponse>() } returns setPatientResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2011/Billing/Patient/SetPatientFlag/Billing/Patient/Flag",
                SetPatientFlagRequest(PatientFlag(type = "flagType")),
                mapOf(
                    "PatientID" to "123",
                    "PatientIDType" to "MRN",
                    "UserID" to "ehrUserId",
                    "UserIDType" to "External"
                )
            )
        } returns ehrResponse

        assertTrue(onboardFlagService.setOnboardedFlag(tenant, "fhirId"))
    }

    @Test
    fun `service errors when epic returns error`() {
        val setPatientResponse = SetPatientFlagResponse(error = "Wow, I should really be an http status")
        val patientMRN = mockk<Identifier> {
            every { value } returns FHIRString("123")
        }
        val patient = mockk<Patient> {
            every { identifier } returns listOf(patientMRN)
        }
        coEvery { ehrDataAuthorityClient.getResourceAs<Patient>("mnemonic", "Patient", "fhirId".localize(tenant)) } returns patient
        every { identifierService.getMRNIdentifier(tenant, listOf(patientMRN)) } returns patientMRN
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<SetPatientFlagResponse>() } returns setPatientResponse
        coEvery {
            epicClient.post(
                tenant,
                "/api/epic/2011/Billing/Patient/SetPatientFlag/Billing/Patient/Flag",
                SetPatientFlagRequest(PatientFlag(type = "flagType")),
                mapOf(
                    "PatientID" to "123",
                    "PatientIDType" to "MRN",
                    "UserID" to "ehrUserId",
                    "UserIDType" to "External"
                )
            )
        } returns ehrResponse

        val error = assertThrows<AppOrchardError> { (onboardFlagService.setOnboardedFlag(tenant, "fhirId")) }
        assertEquals("Wow, I should really be an http status", error.message)
    }

    @Test
    fun `service errors when EHRDA returns no patient`() {
        coEvery { ehrDataAuthorityClient.getResourceAs<Patient>("mnemonic", "Patient", "fhirId".localize(tenant)) } returns null

        val error = assertThrows<VendorIdentifierNotFoundException> { (onboardFlagService.setOnboardedFlag(tenant, "fhirId")) }
        assertEquals("No Patient found for fhirId", error.message)
    }
}
