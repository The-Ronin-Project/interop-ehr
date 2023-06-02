package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.factory.EHRFactory
import com.projectronin.interop.fhir.generators.datatypes.IdentifierGenerator
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.ronin.element.RoninContactPoint
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.RoninPatient
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.test.data.generator.collection.ListDataGenerator
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
class RoninPatientTest {
    private lateinit var roninContactPoint: RoninContactPoint
    private lateinit var normalizer: Normalizer
    private lateinit var localizer: Localizer
    private lateinit var roninPatient: RoninPatient

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val goodRoninMrn = Identifier(
        type = CodeableConcepts.RONIN_MRN,
        system = CodeSystem.RONIN_MRN.uri,
        value = "An MRN".asFHIR()
    )
    private val otherMrn = Identifier(
        system = Uri("testsystem"),
        value = "tomato".asFHIR()
    )
    private val roninFhir = Identifier(
        system = CodeSystem.RONIN_FHIR_ID.uri,
        value = "fhirId".asFHIR(),
        type = CodeableConcepts.RONIN_FHIR_ID
    )
    private val roninTenant = Identifier(
        system = CodeSystem.RONIN_TENANT.uri,
        value = "tenantId".asFHIR(),
        type = CodeableConcepts.RONIN_TENANT
    )

    private val identifierList = listOf(
        Identifier(
            type = CodeableConcept(
                text = "MRN".asFHIR()
            ),
            system = Uri("mrnSystem"),
            value = "An MRN".asFHIR()
        )
    )

    private val mockIdentifierService = mockk<IdentifierService> {
        every { getMRNIdentifier(tenant, identifierList) } returns identifierList[0]
        every { getMRNIdentifier(tenant, emptyList()) } throws VendorIdentifierNotFoundException()
    }

    @BeforeEach
    fun setup() {
        roninContactPoint = mockk {
            every { validateRonin(any(), LocationContext(Patient::class), any()) } answers { thirdArg() }
            every { validateUSCore(any(), LocationContext(Patient::class), any()) } answers { thirdArg() }
        }
        normalizer = mockk {
            every { normalize(any(), tenant) } answers { firstArg() }
        }
        localizer = mockk {
            every { localize(any(), tenant) } answers { firstArg() }
        }
        val ehrFactory = mockk<EHRFactory> {
            every { getVendorFactory(tenant) } returns mockk {
                every { identifierService } returns mockIdentifierService
            }
        }
        roninPatient = RoninPatient(ehrFactory, roninContactPoint, normalizer, localizer)
    }

    @Test
    fun `generates valid basic RoninPatient`() {
        val roninPatient1 = rcdmPatient("test") {}
        val validation = roninPatient.validate(roninPatient1, null).hasErrors()
        assertEquals(validation, false)
        assertNotNull(roninPatient1.meta)
        assertNotNull(roninPatient1.identifier)
        assertEquals(4, roninPatient1.identifier.size)
        assertNotNull(roninPatient1.name)
        assertNotNull(roninPatient1.telecom)
    }

    @Test
    fun `generates valid RoninPatient with existing correct MRN`() {
        val roninPatient1 = rcdmPatient("test") {
            identifier of listOf(goodRoninMrn)
        }
        val validation = roninPatient.validate(roninPatient1, null).hasErrors()
        assertEquals(validation, false)
        val mrn = roninPatient1.identifier.find { it.system == CodeSystem.RONIN_MRN.uri }
        assertEquals("An MRN".asFHIR(), mrn!!.value)
    }

    @Test
    fun `generates valid RoninPatient with existing list of MRNs`() {
        val roninPatient1 = rcdmPatient("test") {
            identifier of listOf(goodRoninMrn, otherMrn)
        }
        val validation = roninPatient.validate(roninPatient1, null).hasErrors()
        assertEquals(validation, false)
        assertEquals(5, roninPatient1.identifier.size)
        val mrn1 = roninPatient1.identifier.find { it.system == CodeSystem.RONIN_MRN.uri }
        assertEquals("An MRN".asFHIR(), mrn1!!.value)
        val mrn2 = roninPatient1.identifier.find { it.system == Uri("testsystem") }
        assertEquals("tomato".asFHIR(), mrn2!!.value)
    }

    @Test
    fun `generates valid RoninPatient with MRN and Ronin Tenant Id`() {
        val roninPatient1 = rcdmPatient("test") {
            identifier of listOf(otherMrn, roninTenant)
        }
        val validation = roninPatient.validate(roninPatient1, null).hasErrors()
        assertEquals(validation, false)
        assertEquals(5, roninPatient1.identifier.size)
        val tenant = roninPatient1.identifier.find { it.system == CodeSystem.RONIN_TENANT.uri }
        assertEquals("tenantId".asFHIR(), tenant!!.value)
    }

    @Test
    fun `generates valid RoninPatient with MRN and Ronin Fhir Id`() {
        val roninPatient1 = rcdmPatient("test") {
            identifier of listOf(otherMrn, roninFhir)
        }
        val validation = roninPatient.validate(roninPatient1, null).hasErrors()
        assertEquals(validation, false)
        assertEquals(5, roninPatient1.identifier.size)
        val fhir = roninPatient1.identifier.find { it.system == CodeSystem.RONIN_FHIR_ID.uri }
        assertEquals("fhirId".asFHIR(), fhir!!.value)
    }

    @Test
    fun `generates valid RoninPatient with other MRNs`() {
        val roninPatient1 = rcdmPatient("test") {
            identifier of listOf(otherMrn)
        }
        val validation = roninPatient.validate(roninPatient1, null).hasErrors()
        assertEquals(validation, false)
        assertEquals(5, roninPatient1.identifier.size)
        val mrn = roninPatient1.identifier.find { it.system == Uri("testsystem") }
        assertEquals("tomato".asFHIR(), mrn!!.value)
    }

    @Test
    fun `generates valid RoninPatient with tenant and bad contact point drops contact point`() {
        val contactPoint = ContactPoint(
            value = "123-456-7890".asFHIR()
        )
        val contactPoint2 = ContactPoint(
            value = "123-456-7890".asFHIR(),
            system = Code(ContactPointSystem.PHONE.code),
            use = Code(ContactPointUse.HOME.code)
        )
        val roninPatient1 = rcdmPatient("test") {
            telecom of listOf(contactPoint, contactPoint2)
            gender of Code("female")
            maritalStatus of CodeableConcept(text = "single".asFHIR())
        }
        val validation = roninPatient.validate(roninPatient1, null).hasErrors()

        assertEquals(validation, false)
        assertEquals(roninPatient1.gender!!.value, "female")
        assertEquals(roninPatient1.maritalStatus!!.text!!.value, "single")
        assertEquals(1, roninPatient1.telecom.size)
        assertEquals("123-456-7890", roninPatient1.telecom[0].value?.value)
    }

    @Test
    fun `generates valid RoninPatient with other names`() {
        val testName = HumanName(
            family = "family".asFHIR(),
            given = listOf("given".asFHIR())
        )
        val roninPatient1 = rcdmPatient("test") {
            name of listOf(testName)
        }
        val validation = roninPatient.validate(roninPatient1, null).hasErrors()
        assertEquals(validation, false)
        assertEquals("official", roninPatient1.name[1].use?.value.toString())
    }

    @Test
    fun `MRN generator with partial MRN, will cause another MRN to be generated`() {
        val testMrn = Identifier(
            type = CodeableConcepts.RONIN_MRN,
            system = Uri("testsystem"),
            value = "An MRN".asFHIR()
        )
        val testMrn2 = Identifier(
            type = CodeableConcept(text = "test".asFHIR()),
            system = CodeSystem.RONIN_MRN.uri,
            value = "An MRN".asFHIR()
        )
        val mrnList = ListDataGenerator(0, IdentifierGenerator()).plus(testMrn).plus(testMrn2)
        val roninMrn = rcdmMrn(mrnList)
        assertEquals(3, roninMrn.size)
        assertEquals(CodeSystem.RONIN_MRN.uri, roninMrn[2].system)
        assertEquals(CodeableConcepts.RONIN_MRN, roninMrn[2].type)
    }
}
