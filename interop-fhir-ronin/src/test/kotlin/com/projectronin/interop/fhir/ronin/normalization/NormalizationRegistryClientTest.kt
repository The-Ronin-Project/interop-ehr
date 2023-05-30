package com.projectronin.interop.fhir.ronin.normalization

import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ConceptMap
import com.projectronin.interop.fhir.r4.resource.ValueSet
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class NormalizationRegistryClientTest {
    private val ociClient = mockk<OCIClient>()
    private val registryPath = "/DataNormalizationRegistry/v2/registry.json"
    private val client = NormalizationRegistryClient(ociClient, registryPath)

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @BeforeEach
    fun setUp() {
        mockkObject(JacksonUtil)
    }

    @AfterEach
    fun tearDown() {
        client.itemLastUpdated.clear()
        client.conceptMapCache.invalidateAll()
        client.valueSetCache.invalidateAll()
        unmockkAll()
    }

    @Test
    fun `getConceptMapping with no matching registry`() {
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
    fun `getConceptMapping pulls new registry and maps`() {
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "12345",
                filename = "file1.json",
                concept_map_name = "AppointmentStatus-tenant",
                concept_map_uuid = "cm-111",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = "test",
                profile_url = null
            ),
            NormalizationRegistryItem(
                data_element = "Patient.telecom.use",
                registry_uuid = "67890",
                filename = "file2.json",
                concept_map_name = "PatientTelecomUse-tenant",
                concept_map_uuid = "cm-222",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ext2",
                resource_type = "Patient",
                tenant_id = "test",
                profile_url = "PatientProfile1"
            )
        )
        val mockkMap1 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem"
                    every { targetVersion?.value } returns "targetVersion"
                    every { source?.value } returns "sourceSystem"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueA"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueA"
                                    every { display?.value } returns "targetDisplayA"
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueB"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueB"
                                    every { display?.value } returns "targetDisplayB"
                                }
                            )
                        }
                    )
                }
            )
        }
        val mockkMap2 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem2"
                    every { targetVersion?.value } returns "targetVersion2"
                    every { source?.value } returns "sourceSystem2"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValue2"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValue2"
                                    every { display?.value } returns "targetDisplay2"
                                }
                            )
                        }
                    )
                }
            )
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        val coding1 = Coding(code = Code(value = "sourceValueA"), system = Uri(value = "sourceSystem"))
        val mapping1 =
            client.getConceptMapping(
                tenant,
                "Appointment.status",
                coding1
            )!!
        assertEquals(mapping1.first.code!!.value, "targetValueA")
        assertEquals(mapping1.first.system!!.value, "targetSystem")
        assertEquals(mapping1.second.url!!.value, "ext1")
        assertEquals(mapping1.second.value!!.value, coding1)
        val coding2 = Coding(code = Code(value = "sourceValue2"), system = Uri(value = "sourceSystem2"))
        val mapping2 =
            client.getConceptMapping(
                tenant,
                "Patient.telecom.use",
                coding2,
                "PatientProfile1"
            )!!
        assertEquals(mapping2.first.code!!.value, "targetValue2")
        assertEquals(mapping2.first.system!!.value, "targetSystem2")
        assertEquals(mapping2.second.url!!.value, "ext2")
        assertEquals(mapping2.second.value!!.value, coding2)
    }

    @Test
    fun `getConceptMappingForEnum with no matching registry`() {
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
        val registry1 = ConceptMapItem(
            source_extension_url = "ext1",
            map = emptyMap()
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Patient.telecom.system",
            "test"
        )
        client.conceptMapCache.put(key, registry1)
        client.itemLastUpdated[key] = LocalDateTime.now()

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
    fun `getConceptMappingForEnum with invalid code provided - successfully maps to valid code`() {
        val registry1 = ConceptMapItem(
            source_extension_url = "ext1",
            map = mapOf(
                SourceKey(
                    "MyPhone",
                    "http://projectronin.io/fhir/CodeSystem/ContactPointSystem"
                ) to TargetValue("phone", "http://hl7.org/fhir/contact-point-system", "Phone", "1")
            )
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Patient.telecom.system",
            "test"
        )
        client.conceptMapCache.put(key, registry1)
        client.itemLastUpdated[key] = LocalDateTime.now()
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
            Coding(
                system = Uri("http://hl7.org/fhir/contact-point-system"),
                code = Code("phone"),
                display = "Phone".asFHIR(),
                version = "1".asFHIR()
            ),
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
        val registry1 = ConceptMapItem(
            source_extension_url = "ext1",
            map = mapOf(
                SourceKey(
                    "MyPhone",
                    "http://projectronin.io/fhir/CodeSystem/ContactPointSystem"
                ) to TargetValue("phone", "http://hl7.org/fhir/contact-point-system", "Phone", "1")
            )
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Patient.telecom.system",
            "test"
        )
        client.conceptMapCache.put(key, registry1)
        client.itemLastUpdated[key] = LocalDateTime.now()
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
            Coding(
                system = Uri("http://hl7.org/fhir/contact-point-system"),
                code = Code("phone"),
                display = "Phone".asFHIR(),
                version = "1".asFHIR()
            ),
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
        val registry1 = ConceptMapItem(
            source_extension_url = "ext1",
            map = mapOf(
                SourceKey(
                    "MyPhone",
                    "http://projectronin.io/fhir/CodeSystem/ContactPointSystem"
                ) to TargetValue("phone", "http://hl7.org/fhir/contact-point-system", "Phone", "1")
            )
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Patient.telecom.system",
            "test",
            "specialPatient"
        )
        client.conceptMapCache.put(key, registry1)
        client.itemLastUpdated[key] = LocalDateTime.now()
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
            Coding(
                system = Uri("http://hl7.org/fhir/contact-point-system"),
                code = Code("phone"),
                display = "Phone".asFHIR(),
                version = "1".asFHIR()
            ),
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
        val mapping =
            client.getValueSet(
                "Patient.telecom.system",
                "specialAppointment"
            )
        assertTrue(mapping.isEmpty())
    }

    @Test
    fun `getValueSet pulls registry and returns set`() {
        val vsTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "01234",
                filename = "file3.json",
                value_set_name = "AppointmentStatus",
                value_set_uuid = "vs-333",
                registry_entry_type = "value_set",
                version = "1",
                resource_type = "Appointment",
                profile_url = "specialAppointment"
            ),
            NormalizationRegistryItem(
                data_element = "Patient.telecom.use",
                registry_uuid = "56789",
                filename = "file4.json",
                value_set_name = "PatientTelecomUse",
                value_set_uuid = "vs-4444",
                registry_entry_type = "value_set",
                version = "1",
                resource_type = "Patient",
                profile_url = "specialPatient"
            )
        )
        val mockkSet1 = mockk<ValueSet> {
            every { expansion?.contains } returns listOf(
                mockk {
                    every { system?.value.toString() } returns "system1"
                    every { version?.value.toString() } returns "version1"
                    every { code?.value.toString() } returns "code1"
                    every { display?.value.toString() } returns "display1"
                }
            )
        }

        val mockkSet2 = mockk<ValueSet> {
            every { expansion?.contains } returns listOf(
                mockk {
                    every { system?.value.toString() } returns "system2"
                    every { version?.value.toString() } returns "version2"
                    every { code?.value.toString() } returns "code2"
                    every { display?.value.toString() } returns "display2"
                }
            )
        }
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns vsTestRegistry
        every { ociClient.getObjectFromINFX("file3.json") } returns "setJson1"
        every { JacksonUtil.readJsonObject("setJson1", ValueSet::class) } returns mockkSet1
        every { ociClient.getObjectFromINFX("file4.json") } returns "setJson2"
        every { JacksonUtil.readJsonObject("setJson2", ValueSet::class) } returns mockkSet2

        val valueSet1 = client.getValueSet("Appointment.status", "specialAppointment")
        val expectedCoding1 = Coding(
            system = Uri(value = "system1"),
            code = Code(value = "code1"),
            display = FHIRString(value = "display1"),
            version = FHIRString(value = "version1")
        )
        assertEquals(valueSet1, listOf(expectedCoding1))

        val valueSet2 = client.getValueSet("Patient.telecom.use", "specialPatient")
        val expectedCoding2 = Coding(
            system = Uri(value = "system2"),
            code = Code(value = "code2"),
            display = FHIRString(value = "display2"),
            version = FHIRString(value = "version2")
        )
        assertEquals(valueSet2, listOf(expectedCoding2))
    }

    @Test
    fun `getValueSet with special profile match`() {
        val registry1 = ValueSetItem(
            set = listOf(TargetValue("code1", "system1", "display1", "version1"))
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ValueSet,
            "Patient.telecom.system",
            null,
            "specialPatient"
        )
        client.valueSetCache.put(key, registry1)
        client.itemLastUpdated[key] = LocalDateTime.now()

        val mapping =
            client.getValueSet(
                "Patient.telecom.system",
                "specialPatient"
            )
        assertEquals(1, mapping.size)
        assertEquals(Code("code1"), mapping[0].code)
    }

    @Test
    fun `universal getRequiredValueSet with profile match`() {
        val registry1 = ValueSetItem(
            set = listOf(TargetValue("code1", "system1", "display1", "version1"))
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ValueSet,
            "Patient.telecom.system",
            null,
            "specialPatient"
        )
        client.valueSetCache.put(key, registry1)
        client.itemLastUpdated[key] = LocalDateTime.now()
        val actualValueSet =
            client.getRequiredValueSet(
                "Patient.telecom.system",
                "specialPatient"
            )
        assertEquals(1, actualValueSet.size)
        assertEquals(Code("code1"), actualValueSet[0].code)
    }

    @Test
    fun `ensure getRequiredValueSet fails when value set is not found`() {
        every { JacksonUtil.readJsonList(any(), NormalizationRegistryItem::class) } returns listOf()

        val exception = assertThrows<MissingNormalizationContentException> {
            client.getRequiredValueSet("Patient.telecom.system", "specialPatient")
        }
        assertEquals("Required value set for specialPatient and Patient.telecom.system not found", exception.message)
    }
}
