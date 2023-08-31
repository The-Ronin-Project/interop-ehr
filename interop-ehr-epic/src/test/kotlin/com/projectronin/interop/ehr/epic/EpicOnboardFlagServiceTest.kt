package com.projectronin.interop.ehr.epic

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.epic.apporchard.model.SetSmartDataValuesRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SetSmartDataValuesResult
import com.projectronin.interop.ehr.epic.apporchard.model.SmartDataValue
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
    private lateinit var epicPatientService: EpicPatientService
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
        epicPatientService = mockk()
        onboardFlagService = EpicOnboardFlagService(epicClient, identifierService, ehrDataAuthorityClient, epicPatientService)
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
        val setPatientResponse = SetSmartDataValuesResult(success = true)
        val patientMRN = mockk<Identifier> {
            every { value } returns FHIRString("123")
        }
        val patient = mockk<Patient> {
            every { identifier } returns listOf(patientMRN)
        }
        coEvery {
            ehrDataAuthorityClient.getResourceAs<Patient>(
                "mnemonic",
                "Patient",
                "fhirId".localize(tenant)
            )
        } returns patient
        every { identifierService.getMRNIdentifier(tenant, listOf(patientMRN)) } returns patientMRN
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<SetSmartDataValuesResult>() } returns setPatientResponse
        coEvery {
            epicClient.put(
                tenant,
                "/api/epic/2013/Clinical/Utility/SETSMARTDATAVALUES/SmartData/Values",
                SetSmartDataValuesRequest(
                    id = "123",
                    idType = "MRN",
                    userID = "ehrUserId",
                    smartDataValues = listOf(
                        SmartDataValue(
                            comments = listOf("Patient has been onboarded in Ronin."),
                            values = "true",
                            smartDataID = "flagType",
                            smartDataIDType = "SDI"
                        )
                    )
                )
            )
        } returns ehrResponse

        assertTrue(onboardFlagService.setOnboardedFlag(tenant, "fhirId"))
    }

    @Test
    fun `service errors when epic returns error`() {
        val setPatientResponse = SetSmartDataValuesResult(success = false)
        val patientMRN = mockk<Identifier> {
            every { value } returns FHIRString("123")
        }
        val patient = mockk<Patient> {
            every { identifier } returns listOf(patientMRN)
        }
        coEvery {
            ehrDataAuthorityClient.getResourceAs<Patient>(
                "mnemonic",
                "Patient",
                "fhirId".localize(tenant)
            )
        } returns patient
        every { identifierService.getMRNIdentifier(tenant, listOf(patientMRN)) } returns patientMRN
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<SetSmartDataValuesResult>() } returns setPatientResponse
        coEvery {
            epicClient.put(
                tenant,
                "/api/epic/2013/Clinical/Utility/SETSMARTDATAVALUES/SmartData/Values",
                SetSmartDataValuesRequest(
                    id = "123",
                    idType = "MRN",
                    userID = "ehrUserId",
                    smartDataValues = listOf(
                        SmartDataValue(
                            comments = listOf("Patient has been onboarded in Ronin."),
                            values = "true",
                            smartDataID = "flagType",
                            smartDataIDType = "SDI"
                        )
                    )
                )
            )
        } returns ehrResponse

        val error = assertThrows<AppOrchardError> { (onboardFlagService.setOnboardedFlag(tenant, "fhirId")) }
    }

    @Test
    fun `service errors when EHRDA returns no patient`() {
        coEvery {
            ehrDataAuthorityClient.getResourceAs<Patient>(
                "mnemonic",
                "Patient",
                "fhirId".localize(tenant)
            )
        } returns null

        coEvery { epicPatientService.getPatient(any(), any()) } throws Exception()
        val error =
            assertThrows<VendorIdentifierNotFoundException> { (onboardFlagService.setOnboardedFlag(tenant, "fhirId")) }
        assertEquals("No Patient found for fhirId", error.message)
    }

    @Test
    fun `service gets patient from Epic when EHRDA returns no patient`() {
        coEvery {
            ehrDataAuthorityClient.getResourceAs<Patient>(
                "mnemonic",
                "Patient",
                "fhirId".localize(tenant)
            )
        } returns null

        val setPatientResponse = SetSmartDataValuesResult(success = true)
        val patientMRN = mockk<Identifier> {
            every { value } returns FHIRString("123")
        }
        val patient = mockk<Patient> {
            every { identifier } returns listOf(patientMRN)
        }
        coEvery { epicPatientService.getPatient(any(), any()) } returns patient
        every { identifierService.getMRNIdentifier(tenant, listOf(patientMRN)) } returns patientMRN
        every { httpResponse.status } returns HttpStatusCode.OK
        coEvery { httpResponse.body<SetSmartDataValuesResult>() } returns setPatientResponse
        coEvery {
            epicClient.put(
                tenant,
                "/api/epic/2013/Clinical/Utility/SETSMARTDATAVALUES/SmartData/Values",
                SetSmartDataValuesRequest(
                    id = "123",
                    idType = "MRN",
                    userID = "ehrUserId",
                    smartDataValues = listOf(
                        SmartDataValue(
                            comments = listOf("Patient has been onboarded in Ronin."),
                            values = listOf("onboarded"),
                            smartDataID = "flagType",
                            smartDataIDType = "SDI"
                        )
                    )
                )
            )
        } returns ehrResponse

        assertTrue(onboardFlagService.setOnboardedFlag(tenant, "fhirId"))
    }
}
