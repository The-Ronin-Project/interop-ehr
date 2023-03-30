package com.projectronin.interop.fhir.ronin.normalization

import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
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

class NormalizationRegistryClientTest {
    private val ociClient = mockk<OCIClient>()
    private val registryPath = "/DataNormalizationRegistry/v2/registry.json"
    private val client = NormalizationRegistryClient(ociClient, registryPath)

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @BeforeEach
    fun setUp() {
        mockkObject(NormalizationRegistryCache)

        every { NormalizationRegistryCache.reloadNeeded(any()) } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getConceptMapping with no matching registry`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "12345",
            filename = "file1.json",
            concept_map_name = "AppointmentStatusTenant",
            concept_map_uuid = "cm-009",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Appointment",
            tenant_id = "test",
            profile_url = null
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "phone")
        val mapping =
            client.getConceptMapping(
                tenant,
                "Patient.telecom.system",
                coding
            )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMappingForEnum with no matching registry`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "12345",
            filename = "file1.json",
            concept_map_name = "AppointmentStatusTenant",
            concept_map_uuid = "cm-009",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Appointment",
            tenant_id = "test",
            profile_url = null
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "phone")
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class
            )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMappingForEnum with valid code provided - code is unchanged`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Patient.telecom.system",
            registry_uuid = "12345",
            filename = "file1.json",
            concept_map_name = "PatientTelecomSystemTenant",
            concept_map_uuid = "cm-010",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Patient",
            tenant_id = "test",
            profile_url = null
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "phone")
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
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
    fun `getConceptMapping with invalid code provided - successfully maps to valid code`() {
        val registry1 = mockk<NormalizationRegistryItem> {
            every { registry_uuid } returns "12345"
            every { tenant_id } returns null
            every { data_element } returns "Patient.telecom.system"
            every { profile_url } returns null
            every { concept_map_uuid } returns "cm-010"
            every { filename } returns "file1.json"
            every { source_extension_url } returns "ext1"
            every { version } returns "1"
            every { map } returns mapOf(
                SourceKey(
                    "MyPhone",
                    "http://projectronin.io/fhir/CodeSystem/ContactPointSystem"
                ) to TargetValue("phone", "http://hl7.org/fhir/contact-point-system", "Phone", "1")
            )
        }
        mockkObject(JacksonUtil)
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns listOf(registry1)

        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "MyPhone")
        val mapping =
            client.getConceptMapping(
                tenant,
                "Patient.telecom.system",
                coding
            )
        mapping!!
        assertEquals(
            Coding(system = Uri("http://hl7.org/fhir/contact-point-system"), code = Code("phone"), display = "Phone".asFHIR(), version = "1".asFHIR()),
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

    @Test
    fun `getConceptMappingForEnum with invalid code provided - successfully maps to valid code`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Patient.telecom.system",
            registry_uuid = "12345",
            filename = "file1.json",
            concept_map_name = "PatientTelecomSystemTenant",
            concept_map_uuid = "cm-010",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Patient",
            tenant_id = "test",
            profile_url = null,
            map = mapOf(
                SourceKey(
                    "MyPhone",
                    "http://projectronin.io/fhir/CodeSystem/ContactPointSystem"
                ) to TargetValue("phone", "http://hl7.org/fhir/contact-point-system", "Phone", "1")
            )
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "MyPhone")
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class
            )
        mapping!!
        assertEquals(
            Coding(system = Uri("http://hl7.org/fhir/contact-point-system"), code = Code("phone"), display = "Phone".asFHIR(), version = "1".asFHIR()),
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

    @Test
    fun `getConceptMappingForEnum with universal tenant match`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Patient.telecom.system",
            registry_uuid = "12345",
            filename = "file1.json",
            concept_map_name = "PatientTelecomSystemTenant",
            concept_map_uuid = "cm-010",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Patient",
            map = mapOf(
                SourceKey(
                    "MyPhone",
                    "http://projectronin.io/fhir/CodeSystem/ContactPointSystem"
                ) to TargetValue("phone", "http://hl7.org/fhir/contact-point-system", "Phone", "1")
            )
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "MyPhone")
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class
            )
        mapping!!
        assertEquals(
            Coding(system = Uri("http://hl7.org/fhir/contact-point-system"), code = Code("phone"), display = "Phone".asFHIR(), version = "1".asFHIR()),
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

    @Test
    fun `getConceptMappingForEnum with universal profile match`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Patient.telecom.system",
            registry_uuid = "12345",
            filename = "file1.json",
            concept_map_name = "PatientTelecomSystemTenant",
            concept_map_uuid = "cm-010",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Patient",
            tenant_id = "test",
            map = mapOf(
                SourceKey(
                    "MyPhone",
                    "http://projectronin.io/fhir/CodeSystem/ContactPointSystem"
                ) to TargetValue("phone", "http://hl7.org/fhir/contact-point-system", "Phone", "1")
            )
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "MyPhone")
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class
            )
        mapping!!
        assertEquals(
            Coding(system = Uri("http://hl7.org/fhir/contact-point-system"), code = Code("phone"), display = "Phone".asFHIR(), version = "1".asFHIR()),
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

    @Test
    fun `getConceptMappingForEnum with profile match`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Patient.telecom.system",
            registry_uuid = "12345",
            filename = "file1.json",
            concept_map_name = "PatientTelecomSystemTenant",
            concept_map_uuid = "cm-010",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Patient",
            tenant_id = "test",
            profile_url = "specialPatient",
            map = mapOf(
                SourceKey(
                    "MyPhone",
                    "http://projectronin.io/fhir/CodeSystem/ContactPointSystem"
                ) to TargetValue("phone", "http://hl7.org/fhir/contact-point-system", "Phone", "1")
            )
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "MyPhone")
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class,
                "specialPatient"
            )
        mapping!!
        assertEquals(
            Coding(system = Uri("http://hl7.org/fhir/contact-point-system"), code = Code("phone"), display = "Phone".asFHIR(), version = "1".asFHIR()),
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

    @Test
    fun `getValueSet with no matching registry`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "12345",
            filename = "file1.json",
            value_set_name = "AppointmentStatusTenant",
            value_set_uuid = "cm-009",
            version = "1",
            resource_type = "Appointment",
            tenant_id = "test",
            profile_url = "specialAppointment"
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val mapping =
            client.getValueSet(
                tenant,
                "Patient.telecom.system",
                "specialAppointment"
            )
        assertNull(mapping)
    }

    @Test
    fun `getValueSet with tenant`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Patient.telecom.system",
            registry_uuid = "12345",
            filename = "file1.json",
            value_set_name = "PatientTelecomSystemTenant",
            value_set_uuid = "cm-010",
            version = "1",
            resource_type = "Patient",
            tenant_id = "test",
            profile_url = "specialAppointment",
            set = listOf(TargetValue("code1", "system1", "display1", "version1"))
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val mapping =
            client.getValueSet(
                tenant,
                "Patient.telecom.system",
                "specialAppointment"
            )
        mapping!!
        assertEquals(1, mapping.size)
        assertEquals(Code("code1"), mapping[0].code)
    }

    @Test
    fun `getValueSet with universal tenant match`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Patient.telecom.system",
            registry_uuid = "12345",
            filename = "file1.json",
            value_set_name = "PatientTelecomSystem",
            value_set_uuid = "cm-010",
            version = "1",
            resource_type = "Patient",
            profile_url = "specialAppointment",
            set = listOf(TargetValue("code1", "system1", "display1", "version1"))
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val mapping =
            client.getValueSet(
                tenant,
                "Patient.telecom.system",
                "specialAppointment"
            )
        mapping!!
        assertEquals(1, mapping.size)
        assertEquals(Code("code1"), mapping[0].code)
    }

    @Test
    fun `getValueSet with universal profile match`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Patient.telecom.system",
            registry_uuid = "12345",
            filename = "file1.json",
            value_set_name = "PatientTelecomSystem",
            value_set_uuid = "cm-010",
            version = "1",
            resource_type = "Patient",
            tenant_id = "test",
            set = listOf(TargetValue("code1", "system1", "display1", "version1"))
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val mapping =
            client.getValueSet(
                tenant,
                "Patient.telecom.system"
                // default profile_url is null
            )
        mapping!!
        assertEquals(1, mapping.size)
        assertEquals(Code("code1"), mapping[0].code)
    }

    @Test
    fun `getValueSet with special profile match`() {
        val registry1 = NormalizationRegistryItem(
            data_element = "Patient.telecom.system",
            registry_uuid = "12345",
            filename = "file1.json",
            value_set_name = "PatientTelecomSystem",
            value_set_uuid = "cm-010",
            version = "1",
            resource_type = "Patient",
            tenant_id = "test",
            profile_url = "specialPatient",
            set = listOf(TargetValue("code1", "system1", "display1", "version1"))
        )
        every { NormalizationRegistryCache.getCurrentRegistry() } returns listOf(registry1)

        val mapping =
            client.getValueSet(
                tenant,
                "Patient.telecom.system",
                "specialPatient"
            )
        mapping!!
        assertEquals(1, mapping.size)
        assertEquals(Code("code1"), mapping[0].code)
    }
}
