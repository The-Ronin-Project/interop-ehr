package com.projectronin.interop.fhir.ronin.conceptmap

import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConceptMapClientTest {
    private val ociClient = mockk<OCIClient>()
    private val client = ConceptMapClient(ociClient)

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @BeforeEach
    fun setUp() {
        mockkObject(ConceptMapCache)

        every { ConceptMapCache.reloadNeeded(any()) } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getConceptMappingForEnum with no matching registry`() {
        val registry1 = ConceptMapRegistry(
            data_element = "Appointment.status",
            registry_uuid = "12345",
            filename = "file1.json",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Appointment",
            tenant_id = "test"
        )
        every { ConceptMapCache.getCurrentRegistry() } returns listOf(registry1)

        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "phone")
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient",
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class
            )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMappingForEnum with valid code provided`() {
        val registry1 = ConceptMapRegistry(
            data_element = "Patient.telecom.system",
            registry_uuid = "12345",
            filename = "file1.json",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Patient",
            tenant_id = "test"
        )
        every { ConceptMapCache.getCurrentRegistry() } returns listOf(registry1)

        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "phone")
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient",
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class
            )
        mapping!!
        assertEquals(coding, mapping.first)
        assertEquals(
            Extension(
                url = Uri("ext1"),
                value = DynamicValue(DynamicValueType.CODING, value = coding)
            ),
            mapping.second
        )
    }

    @Test
    fun `getConceptMappingForEnum with invalid code provided`() {
        val registry1 = ConceptMapRegistry(
            data_element = "Patient.telecom.system",
            registry_uuid = "12345",
            filename = "file1.json",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Patient",
            tenant_id = "test",
            map = mapOf(
                SourceKey(
                    "MyPhone",
                    "http://projectronin.io/fhir/CodeSystem/ContactPointSystem"
                ) to TargetValue("phone", "http://hl7.org/fhir/contact-point-system")
            )
        )
        every { ConceptMapCache.getCurrentRegistry() } returns listOf(registry1)

        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "MyPhone")
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient",
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class
            )
        mapping!!
        assertEquals(
            Coding(system = Uri("http://hl7.org/fhir/contact-point-system"), code = Code("phone")),
            mapping.first
        )
        assertEquals(
            Extension(
                url = Uri("ext1"),
                value = DynamicValue(DynamicValueType.CODING, value = coding)
            ),
            mapping.second
        )
    }
}
