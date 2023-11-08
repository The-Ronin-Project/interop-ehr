package com.projectronin.interop.ehr.decorator

import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DecoratorPatientServiceTest {
    private val tenant = mockk<Tenant>()

    private val patientService = mockk<PatientService>()
    private val identifierService = mockk<IdentifierService> {
        every { getMRNIdentifier(tenant, any()) } returns mockk {
            every { value } returns FHIRString("mrn")
        }
    }

    private val mrnIdentifier = Identifier(
        system = CodeSystem.RONIN_MRN.uri,
        value = FHIRString("mrn"),
        type = CodeableConcepts.RONIN_MRN
    )

    private val decorator = DecoratorPatientService(patientService, identifierService)

    @Test
    fun `handles decorating when finding MRN throws an exception`() {
        val identifiers = listOf(Identifier(system = Uri("http://example.org/not-mrn"), value = FHIRString("value")))
        val patient1234 = Patient(
            id = Id("1234"),
            identifier = identifiers
        )

        every { identifierService.getMRNIdentifier(tenant, identifiers) } throws IllegalStateException()
        every { patientService.getByID(tenant, "1234") } returns patient1234

        val response = decorator.getByID(tenant, "1234")

        val expectedPatient = Patient(
            id = Id("1234"),
            identifier = identifiers + Identifier(
                system = CodeSystem.RONIN_FHIR_ID.uri,
                value = FHIRString("1234"),
                type = CodeableConcepts.RONIN_FHIR_ID
            )
        )
        assertEquals(expectedPatient, response)
    }

    @Test
    fun `handles decorating when MRN is found`() {
        val identifiers = listOf(Identifier(system = Uri("http://example.org/mrn"), value = FHIRString("value")))
        val patient1234 = Patient(
            id = Id("1234"),
            identifier = identifiers
        )

        every { patientService.getByID(tenant, "1234") } returns patient1234

        val response = decorator.getByID(tenant, "1234")

        val expectedPatient = Patient(
            id = Id("1234"),
            identifier = identifiers + Identifier(
                system = CodeSystem.RONIN_FHIR_ID.uri,
                value = FHIRString("1234"),
                type = CodeableConcepts.RONIN_FHIR_ID
            ) + mrnIdentifier
        )
        assertEquals(expectedPatient, response)
    }

    @Test
    fun `handles decorating when ID is null`() {
        val identifiers = listOf(Identifier(system = Uri("http://example.org/mrn"), value = FHIRString("value")))
        val patient1234 = Patient(
            id = null,
            identifier = identifiers
        )

        every { patientService.getByID(tenant, "1234") } returns patient1234

        val response = decorator.getByID(tenant, "1234")

        val expectedPatient = Patient(
            id = null,
            identifier = identifiers + mrnIdentifier
        )
        assertEquals(expectedPatient, response)
    }

    @Test
    fun `decorates Patients returned by findPatient`() {
        val patient = Patient()

        val birthDate = LocalDate.now()
        every { patientService.findPatient(tenant, birthDate, "given", "family") } returns listOf(patient)

        val response = decorator.findPatient(tenant, birthDate, "given", "family")

        val expectedPatient = Patient(identifier = listOf(mrnIdentifier))
        assertEquals(listOf(expectedPatient), response)
    }

    @Test
    fun `decorates Patients returned by findPatientsById`() {
        val patient = Patient()

        val identifier = Identifier(system = Uri("http://example.org/mrn"), value = FHIRString("value"))
        every { patientService.findPatientsById(tenant, mapOf("1234" to identifier)) } returns mapOf("1234" to patient)

        val response = decorator.findPatientsById(tenant, mapOf("1234" to identifier))

        val expectedPatient = Patient(identifier = listOf(mrnIdentifier))
        assertEquals(mapOf("1234" to expectedPatient), response)
    }

    @Test
    fun `decorates Patients returned by getPatient`() {
        val patient = Patient()
        every { patientService.getPatient(tenant, "1234") } returns patient

        val response = decorator.getPatient(tenant, "1234")

        val expectedPatient = Patient(identifier = listOf(mrnIdentifier))
        assertEquals(expectedPatient, response)
    }

    @Test
    fun `decorates Patients returned by getByID`() {
        val patient = Patient()
        every { patientService.getByID(tenant, "1234") } returns patient

        val response = decorator.getByID(tenant, "1234")

        val expectedPatient = Patient(identifier = listOf(mrnIdentifier))
        assertEquals(expectedPatient, response)
    }

    @Test
    fun `decorates Patients returned by getByIDs`() {
        val patient = Patient()
        every { patientService.getByIDs(tenant, listOf("1234")) } returns mapOf("1234" to patient)

        val response = decorator.getByIDs(tenant, listOf("1234"))

        val expectedPatient = Patient(identifier = listOf(mrnIdentifier))
        assertEquals(mapOf("1234" to expectedPatient), response)
    }

    @Test
    fun `passes through calls to fhirResourceType`() {
        every { patientService.fhirResourceType } returns Patient::class.java
        assertEquals(Patient::class.java, decorator.fhirResourceType)
    }

    @Test
    fun `passes through calls to getPatientFHIRId`() {
        every { patientService.getPatientFHIRId(tenant, "1234") } returns "5678"

        val response = decorator.getPatientFHIRId(tenant, "1234")
        assertEquals("5678", response)
    }
}
