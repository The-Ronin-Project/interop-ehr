package com.projectronin.interop.fhir.ronin.normalization

import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ConceptMap
import com.projectronin.interop.fhir.r4.resource.ValueSet
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.LocationStatus
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NormalizationRegistryCacheTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "tenant"
    }
    private val ociClient = mockk<OCIClient>()
    private val registryFile = "DataNormalizationRegistry/v2/registry.json"
    private val normClient = NormalizationRegistryClient(ociClient, registryFile)
    private val cmTestRegistry = listOf(
        NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "12345",
            filename = "file1.json",
            concept_map_name = "AppointmentStatus-tenant",
            concept_map_uuid = "cm-111",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Appointment",
            tenant_id = "tenant",
            profile_url = null
        ),
        NormalizationRegistryItem(
            data_element = "Patient.telecom.use",
            registry_uuid = "67890",
            filename = "file2.json",
            concept_map_name = "PatientTelecomUse-tenant",
            concept_map_uuid = "cm-222",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Patient",
            tenant_id = "tenant",
            profile_url = null
        )
    )

    private val vsTestRegistry = listOf(
        NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "01234",
            filename = "file3.json",
            value_set_name = "AppointmentStatus",
            value_set_uuid = "vs-333",
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
            version = "1",
            resource_type = "Patient",
            profile_url = "specialPatient"
        )
    )

    private val mixedTestRegistry = listOf(
        NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "01234",
            filename = "file3.json",
            value_set_name = "AppointmentStatus",
            value_set_uuid = "vs-333",
            version = "1",
            resource_type = "Appointment",
            tenant_id = null,
            profile_url = null
        ),
        NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "12345",
            filename = "file1.json",
            concept_map_name = "AppointmentStatus-tenant",
            concept_map_uuid = "cm-111",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Appointment",
            tenant_id = "tenant",
            profile_url = null
        ),
        NormalizationRegistryItem(
            data_element = "Patient.telecom.use",
            registry_uuid = "56789",
            filename = "file4.json",
            value_set_name = "PatientTelecomUse",
            value_set_uuid = "vs-4444",
            version = "1",
            resource_type = "Patient",
            tenant_id = null,
            profile_url = "specialPatient"
        ),
        NormalizationRegistryItem(
            data_element = "Patient.telecom.use",
            registry_uuid = "67890",
            filename = "file2.json",
            concept_map_name = "PatientTelecomUse-tenant",
            concept_map_uuid = "cm-222",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Patient",
            tenant_id = "tenant",
            profile_url = "specialPatient"
        )
    )

    private val tenant1 = mockk<Tenant> {
        every { mnemonic } returns "tenant1"
    }
    private val tenant2 = mockk<Tenant> {
        every { mnemonic } returns "tenant2"
    }
    private val mixedTenantRegistry = listOf(
        NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "01234",
            filename = "file3.json",
            value_set_name = "AppointmentStatus-tenant2",
            value_set_uuid = "vs-3333",
            version = "1",
            resource_type = "Appointment",
            tenant_id = "tenant2",
            profile_url = null
        ),
        NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "12345",
            filename = "file1.json",
            value_set_name = "AppointmentStatus-tenant1",
            value_set_uuid = "vs-1111",
            version = "1",
            resource_type = "Appointment",
            tenant_id = "tenant1",
            profile_url = null
        ),
        NormalizationRegistryItem(
            data_element = "Patient.telecom.use",
            registry_uuid = "56789",
            filename = "file4.json",
            value_set_name = "PatientTelecomUse-tenant2",
            value_set_uuid = "vs-4444",
            version = "1",
            resource_type = "Patient",
            tenant_id = "tenant2",
            profile_url = "specialPatient"
        ),
        NormalizationRegistryItem(
            data_element = "Patient.telecom.use",
            registry_uuid = "67890",
            filename = "file2.json",
            value_set_name = "PatientTelecomUse-tenant1",
            value_set_uuid = "vs-2222",
            version = "1",
            resource_type = "Patient",
            tenant_id = "tenant1",
            profile_url = "specialPatient"
        )
    )

    @Test
    fun `reload works correctly for ConceptMap`() {
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
        every { ociClient.getObjectFromINFX(registryFile) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2

        mockkObject(NormalizationRegistryCache)
        normClient.reload(tenant.mnemonic)
        var actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("12345", actual[0].registry_uuid)
        assertEquals("67890", actual[1].registry_uuid)

        // adding a new entry gets added to cache after reload
        val testRegistry2 = cmTestRegistry + NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "newUUID",
            filename = "newFile",
            concept_map_name = "AppointmentStatusNew",
            concept_map_uuid = "cm-555",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Appointment",
            tenant_id = "tenant"
        )
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns testRegistry2
        every { ociClient.getObjectFromINFX("newFile") } returns "mapJson3"
        every { JacksonUtil.readJsonObject("mapJson3", ConceptMap::class) } returns mockkMap1
        normClient.reload(tenant.mnemonic)
        actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(3, actual.size)
        assertEquals("12345", actual[0].registry_uuid)
        assertEquals("67890", actual[1].registry_uuid)
        assertEquals("newUUID", actual[2].registry_uuid)
        // --
        // reload if version changes, also make sure if entries are deleted we remove them from cache
        val testRegistry3 = listOf(
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "12345",
                filename = "file2.json",
                concept_map_name = "AppointmentStatus-tenant",
                concept_map_uuid = "cm-666",
                version = "2",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = "tenant"
            ),
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "nullTenantUUID",
                filename = "universal.json",
                concept_map_name = "AppointmentStatus",
                concept_map_uuid = "cm-777",
                version = "1",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = null
            )
        )
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns testRegistry3
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        every { ociClient.getObjectFromINFX("universal.json") } returns "universalJson"
        every { JacksonUtil.readJsonObject("universalJson", ConceptMap::class) } returns mockkMap2
        normClient.reload(tenant.mnemonic)
        actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("12345", actual[0].registry_uuid)
        assertEquals("nullTenantUUID", actual[1].registry_uuid)
        assertEquals("2", actual[0].version)
        assertEquals(
            mapOf(
                SourceKey("sourceValue2", "sourceSystem2")
                    to TargetValue("targetValue2", "targetSystem2", "targetDisplay2", "targetVersion2")
            ),
            actual[0].map
        )
        // --
        // don't reload if version hasn't changed
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        normClient.reload(tenant.mnemonic)
        actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("12345", actual[0].registry_uuid)
        assertEquals("2", actual[0].version)
        assertEquals(
            mapOf(
                SourceKey("sourceValue2", "sourceSystem2")
                    to TargetValue("targetValue2", "targetSystem2", "targetDisplay2", "targetVersion2")
            ),
            actual[0].map
        )
        // --
        // reload if version changes with different tenant_id
        val testRegistry4 = listOf(
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "12345",
                filename = "file2.json",
                concept_map_name = "AppointmentStatus-tenant",
                concept_map_uuid = "cm-666",
                version = "3",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = "newtest"
            ),
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "nullTenantUUID",
                filename = "universal.json",
                concept_map_name = "AppointmentStatus",
                concept_map_uuid = "cm-777",
                version = "1",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = null
            )
        )
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns testRegistry4
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        every { ociClient.getObjectFromINFX("universal.json") } returns "universalJson"
        every { JacksonUtil.readJsonObject("universalJson", ConceptMap::class) } returns mockkMap2
        normClient.reload(tenant.mnemonic)
        actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("12345", actual[0].registry_uuid)
        assertEquals("nullTenantUUID", actual[1].registry_uuid)
        assertEquals("3", actual[0].version)
        assertEquals(
            mapOf(
                SourceKey("sourceValue2", "sourceSystem2")
                    to TargetValue("targetValue2", "targetSystem2", "targetDisplay2", "targetVersion2")
            ),
            actual[0].map
        )
        // --
        unmockkObject(JacksonUtil)
        unmockkObject(NormalizationRegistryCache)
    }

    @Test
    fun `getConceptMappingForEnum works correctly`() {
        mockkObject(NormalizationRegistryCache)
        cmTestRegistry[0].map = mapOf(
            SourceKey("sourceValue1", "sourceSystem1")
                to TargetValue("targetValue1", "targetSystem1", "targetDisplay1", "targetVersion1")
        )
        cmTestRegistry[1].map = mapOf(
            SourceKey("sourceValue2", "sourceSystem2")
                to TargetValue("targetValue2", "targetSystem2", "targetDisplay2", "targetVersion2")
        )
        val sourceCoding = Coding(code = Code("sourceValue1"), system = Uri("sourceSystem1"))
        NormalizationRegistryCache.setNewRegistry(cmTestRegistry, tenant.mnemonic)
        // calling the client here tests the 'reload' line of code
        val mapped = normClient.getConceptMappingForEnum(
            tenant,
            "Appointment.status",
            sourceCoding,
            AppointmentStatus::class
        )
        assertEquals(
            Coding(
                code = Code("targetValue1"),
                system = Uri("targetSystem1"),
                display = "targetDisplay1".asFHIR(),
                version = "targetVersion1".asFHIR()
            ),
            mapped?.first
        )
        assertEquals("ext1", mapped?.second?.url?.value)
        assertEquals(sourceCoding, mapped?.second?.value?.value)

        cmTestRegistry.forEach { it.map = null } // reset
        unmockkObject(NormalizationRegistryCache)
    }

    @Test
    fun `null logic works for ConceptMap`() {
        val badTenant = mockk<Tenant> {
            every { mnemonic } returns "badTenant"
        }
        mockkObject(NormalizationRegistryCache)
        NormalizationRegistryCache.setNewRegistry(cmTestRegistry, tenant.mnemonic)
        NormalizationRegistryCache.setNewRegistry(cmTestRegistry, badTenant.mnemonic) // avoid reloads
        val sourceCoding = Coding(code = Code("sourceValue1"), system = Uri("sourceSystem1"))
        // bad tenant
        assertNull(
            normClient.getConceptMappingForEnum(
                badTenant,
                "Appointment.status",
                sourceCoding,
                AppointmentStatus::class
            )
        )
        // bad resource type
        assertNull(
            normClient.getConceptMappingForEnum(
                tenant,
                "Location.status",
                sourceCoding,
                LocationStatus::class
            )
        )
        // bad element type
        assertNull(
            normClient.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.use",
                sourceCoding,
                ContactPointUse::class
            )
        )
        // everything good but registry has no map
        assertNull(
            normClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                sourceCoding,
                AppointmentStatus::class
            )
        )
        // everything good, map exists, but look up value isn't mapped
        cmTestRegistry[0].map = mapOf(
            SourceKey("sourceValuebad", "sourceSystembad")
                to TargetValue("targetValue1", "targetSystem1", "targetDisplay1", "targetVersion1")
        )
        assertNull(
            normClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                sourceCoding,
                AppointmentStatus::class
            )
        )

        unmockkObject(NormalizationRegistryCache)
    }

    @Test
    fun `try catch works for ConceptMap`() {
        mockkObject(JacksonUtil)
        every { JacksonUtil.readJsonObject(any(), ConceptMap::class) } throws Exception("bad")
        assertEquals(emptyMap<SourceKey, TargetValue>(), normClient.getConceptMapData("name"))

        every { JacksonUtil.readJsonList(any(), NormalizationRegistryItem::class) } throws Exception("bad")
        assertEquals(emptyList<NormalizationRegistryItem>(), normClient.getNewRegistry())
        unmockkObject(JacksonUtil)
    }

    @Test
    fun `getConceptMap works correctly with tenant-specific CodeSystems`() {
        mockkObject(NormalizationRegistryCache)
        cmTestRegistry[0].map = mapOf(
            SourceKey("sourceValue1", "http://projectronin.io/fhir/CodeSystem/AppointmentStatus")
                to TargetValue("targetValue1", "targetSystem1", "targetDisplay1", "targetVersion1")
        )
        cmTestRegistry[1].map = mapOf(
            SourceKey("sourceValue2", "sourceSystem2")
                to TargetValue("targetValue2", "targetSystem2", "targetDisplay2", "targetVersion1")
        )
        val sourceCoding = Coding(
            code = Code("sourceValue1"),
            system = RoninConceptMap.CODE_SYSTEMS.toUri(tenant, "AppointmentStatus")
        )
        NormalizationRegistryCache.setNewRegistry(cmTestRegistry, tenant.mnemonic)
        // calling the client here tests the 'reload' line of code
        val mapped = normClient.getConceptMappingForEnum(
            tenant,
            "Appointment.status",
            sourceCoding,
            AppointmentStatus::class
        )
        assertEquals(
            Coding(
                code = Code("targetValue1"),
                system = Uri("targetSystem1"),
                display = "targetDisplay1".asFHIR(),
                version = "targetVersion1".asFHIR()
            ),
            mapped?.first
        )
        assertEquals("ext1", mapped?.second?.url?.value)
        assertEquals(sourceCoding, mapped?.second?.value?.value)

        cmTestRegistry.forEach { it.map = null } // reset
        unmockkObject(NormalizationRegistryCache)
    }

    @Test
    fun `getConceptMap works correctly with registry that has both ConceptMap and ValueSet`() {
        mockkObject(NormalizationRegistryCache)
        mixedTestRegistry[1].map = mapOf(
            SourceKey("sourceValue1", "http://projectronin.io/fhir/CodeSystem/AppointmentStatus")
                to TargetValue("targetValue1", "targetSystem1", "targetDisplay1", "targetVersion1")
        )
        mixedTestRegistry[3].map = mapOf(
            SourceKey("sourceValue2", "http://projectronin.io/fhir/CodeSystem/ContactPointUse")
                to TargetValue("targetValue2", "targetSystem2", "targetDisplay2", "targetVersion2")
        )
        val sourceCodingAppointment = Coding(
            code = Code("sourceValue1"),
            system = RoninConceptMap.CODE_SYSTEMS.toUri(tenant, "AppointmentStatus")
        )
        NormalizationRegistryCache.setNewRegistry(mixedTestRegistry, tenant.mnemonic)

        val mappedAppointment = normClient.getConceptMappingForEnum(
            tenant,
            "Appointment.status",
            sourceCodingAppointment,
            AppointmentStatus::class
        )
        assertEquals(
            Coding(
                code = Code("targetValue1"),
                system = Uri("targetSystem1"),
                display = "targetDisplay1".asFHIR(),
                version = "targetVersion1".asFHIR()
            ),
            mappedAppointment?.first
        )
        assertEquals("ext1", mappedAppointment?.second?.url?.value)
        assertEquals(sourceCodingAppointment, mappedAppointment?.second?.value?.value)

        val sourceCodingPatient = Coding(
            code = Code("sourceValue2"),
            system = RoninConceptMap.CODE_SYSTEMS.toUri(tenant, "ContactPointUse")
        )
        val mappedUse = normClient.getConceptMappingForEnum(
            tenant,
            "Patient.telecom.use",
            sourceCodingPatient,
            ContactPointUse::class,
            "specialPatient"
        )
        assertEquals(
            Coding(
                code = Code("targetValue2"),
                system = Uri("targetSystem2"),
                display = "targetDisplay2".asFHIR(),
                version = "targetVersion2".asFHIR()
            ),
            mappedUse?.first
        )
        assertEquals("ext1", mappedUse?.second?.url?.value)
        assertEquals(sourceCodingPatient, mappedUse?.second?.value?.value)

        mixedTestRegistry.forEach { it.map = null } // reset
        unmockkObject(NormalizationRegistryCache)
    }

    @Test
    fun `reload works correctly for ValueSet`() {
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
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryFile) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns vsTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "setJson1"
        every { JacksonUtil.readJsonObject("setJson1", ValueSet::class) } returns mockkSet1
        every { ociClient.getObjectFromINFX("file2.json") } returns "setJson2"
        every { JacksonUtil.readJsonObject("setJson2", ValueSet::class) } returns mockkSet2

        mockkObject(NormalizationRegistryCache)
        normClient.reload(tenant.mnemonic)
        var actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("01234", actual[0].registry_uuid)
        assertEquals("56789", actual[1].registry_uuid)

        // adding a new entry gets added to cache after reload
        val testRegistry2 = vsTestRegistry + NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "newUUID",
            filename = "newFile",
            value_set_name = "AppointmentStatusNew",
            value_set_uuid = "vs-555",
            version = "1",
            resource_type = "Appointment",
            tenant_id = "tenant"
        )

        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns testRegistry2
        every { ociClient.getObjectFromINFX("newFile") } returns "setJson3"
        every { JacksonUtil.readJsonObject("setJson3", ValueSet::class) } returns mockkSet1
        normClient.reload(tenant.mnemonic)
        actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(3, actual.size)
        assertEquals("01234", actual[0].registry_uuid)
        assertEquals("56789", actual[1].registry_uuid)
        assertEquals("newUUID", actual[2].registry_uuid)
        // --
        // reload if version changes, also make sure if entries are deleted we remove them from cache
        val testRegistry3 = listOf(
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "01234",
                filename = "file2.json",
                value_set_name = "AppointmentStatus-tenant",
                value_set_uuid = "vs-666",
                version = "2",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = "tenant"
            ),
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "nullTenantUUID",
                filename = "universal.json",
                value_set_name = "AppointmentStatus",
                value_set_uuid = "vs-777",
                version = "1",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = null
            )
        )
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns testRegistry3
        every { ociClient.getObjectFromINFX("file2.json") } returns "setJson2"
        every { JacksonUtil.readJsonObject("setJson2", ValueSet::class) } returns mockkSet2
        every { ociClient.getObjectFromINFX("universal.json") } returns "universalJson"
        every { JacksonUtil.readJsonObject("universalJson", ValueSet::class) } returns mockkSet2
        normClient.reload(tenant.mnemonic)
        actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("01234", actual[0].registry_uuid)
        assertEquals("nullTenantUUID", actual[1].registry_uuid)
        assertEquals("2", actual[0].version)
        assertEquals(
            listOf(TargetValue("code2", "system2", "display2", "version2")),
            actual[0].set
        )
        // --
        // don't reload if version hasn't changed
        every { JacksonUtil.readJsonObject("setJson1", ValueSet::class) } returns mockkSet1
        normClient.reload(tenant.mnemonic)
        actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("01234", actual[0].registry_uuid)
        assertEquals("2", actual[0].version)
        assertEquals(
            listOf(TargetValue("code2", "system2", "display2", "version2")),
            actual[0].set
        )
        // --
        // reload if version changes with different tenant_id
        val testRegistry4 = listOf(
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "01234",
                filename = "file2.json",
                value_set_name = "AppointmentStatus-tenant",
                value_set_uuid = "vs-666",
                version = "3",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = "tenant"
            ),
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "nullTenantUUID",
                filename = "universal.json",
                concept_map_name = "AppointmentStatus",
                concept_map_uuid = "cm-777",
                version = "1",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = null
            )
        )
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns testRegistry4
        every { ociClient.getObjectFromINFX("file2.json") } returns "setJson2"
        every { JacksonUtil.readJsonObject("setJson2", ValueSet::class) } returns mockkSet2
        every { ociClient.getObjectFromINFX("universal.json") } returns "universalJson"
        every { JacksonUtil.readJsonObject("universalJson", ValueSet::class) } returns mockkSet2
        normClient.reload(tenant.mnemonic)
        actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("01234", actual[0].registry_uuid)
        assertEquals("nullTenantUUID", actual[1].registry_uuid)
        assertEquals("3", actual[0].version)
        assertEquals(
            listOf(TargetValue("code2", "system2", "display2", "version2")),
            actual[0].set
        )
        // --
        unmockkObject(JacksonUtil)
        unmockkObject(NormalizationRegistryCache)
    }

    @Test
    fun `getValueSet works correctly`() {
        mockkObject(NormalizationRegistryCache)
        vsTestRegistry[0].set = listOf(
            TargetValue("code1", "system1", "display1", "version1")
        )
        vsTestRegistry[1].set = listOf(
            TargetValue("code2", "system2", "display2", "version2")
        )
        NormalizationRegistryCache.setNewRegistry(vsTestRegistry, tenant.mnemonic)
        // calling the client here tests the 'reload' line of code
        val set = normClient.getValueSet(
            tenant,
            "Appointment.status",
            "specialAppointment"
        )
        assertEquals(
            Coding(
                code = Code("code1"),
                system = Uri("system1"),
                display = "display1".asFHIR(),
                version = "version1".asFHIR()
            ),
            set.first()
        )

        vsTestRegistry.forEach { it.set = null } // reset
        unmockkObject(NormalizationRegistryCache)
    }

    @Test
    fun `null logic works for ValueSet`() {
        val badTenant = mockk<Tenant> {
            every { mnemonic } returns "badTenant"
        }
        mockkObject(NormalizationRegistryCache)
        NormalizationRegistryCache.setNewRegistry(vsTestRegistry, tenant.mnemonic)
        NormalizationRegistryCache.setNewRegistry(vsTestRegistry, badTenant.mnemonic) // avoid reloads
        val sourceCoding = Coding(code = Code("sourceValue1"), system = Uri("sourceSystem1"))
        // bad tenant
        assertNull(
            normClient.getConceptMappingForEnum(
                badTenant,
                "Appointment.status",
                sourceCoding,
                AppointmentStatus::class
            )
        )
        // bad resource type
        assertNull(
            normClient.getConceptMappingForEnum(
                tenant,
                "Location.status",
                sourceCoding,
                LocationStatus::class
            )
        )
        // bad element type
        assertNull(
            normClient.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.use",
                sourceCoding,
                ContactPointUse::class
            )
        )
        // everything good but registry has no set
        assertNull(
            normClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                sourceCoding,
                AppointmentStatus::class
            )
        )
        // everything good, set exists, but look up value isn't set
        vsTestRegistry[0].set = listOf(
            TargetValue("targetValue1", "targetSystem1", "targetDisplay1", "targetVersion1")
        )
        assertNull(
            normClient.getConceptMappingForEnum(
                tenant,
                "Appointment.status",
                sourceCoding,
                AppointmentStatus::class
            )
        )

        unmockkObject(NormalizationRegistryCache)
    }

    @Test
    fun `try catch works for ValueSet`() {
        mockkObject(JacksonUtil)
        every { JacksonUtil.readJsonObject(any(), ConceptMap::class) } throws Exception("bad")
        assertEquals(emptyMap<SourceKey, TargetValue>(), normClient.getConceptMapData("name"))

        every { JacksonUtil.readJsonList(any(), NormalizationRegistryItem::class) } throws Exception("bad")
        assertEquals(emptyList<NormalizationRegistryItem>(), normClient.getNewRegistry())
        unmockkObject(JacksonUtil)
    }

    @Test
    fun `getValueSet with nulls`() {
        // --
        // null display?.value
        val mockkSet1 = mockk<ValueSet> {
            every { expansion?.contains } returns listOf(
                mockk {
                    every { system?.value } returns "system1"
                    every { version?.value } returns "version1"
                    every { code?.value } returns "code1"
                    every { display?.value } returns null
                }
            )
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX("file1.json") } returns "setJson1"
        every { JacksonUtil.readJsonObject("setJson1", ValueSet::class) } returns mockkSet1
        mockkObject(NormalizationRegistryCache)
        assertEquals(emptyList<TargetValue>(), normClient.getValueSetData("file1.json"))
        // --
        // null display
        val mockkSet2 = mockk<ValueSet> {
            every { expansion?.contains } returns listOf(
                mockk {
                    every { system?.value } returns "system1"
                    every { version?.value } returns "version1"
                    every { code?.value } returns "code1"
                    every { display } returns null
                }
            )
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX("file1.json") } returns "setJson1"
        every { JacksonUtil.readJsonObject("setJson1", ValueSet::class) } returns mockkSet2
        mockkObject(NormalizationRegistryCache)
        assertEquals(emptyList<TargetValue>(), normClient.getValueSetData("file1.json"))
        // --
        // empty expansion
        val mockkSet4 = mockk<ValueSet> {
            every { expansion } returns null
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryFile) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns vsTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "setJson1"
        every { JacksonUtil.readJsonObject("setJson1", ValueSet::class) } returns mockkSet4
        assertEquals(emptyList<TargetValue>(), normClient.getValueSetData("file1.json"))
        // --
        // empty contains
        val mockkSet5 = mockk<ValueSet> {
            every { expansion?.contains } returns emptyList()
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryFile) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns vsTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "setJson1"
        every { JacksonUtil.readJsonObject("setJson1", ValueSet::class) } returns mockkSet5
        assertEquals(emptyList<TargetValue>(), normClient.getValueSetData("file1.json"))
        // --
        unmockkObject(NormalizationRegistryCache)
        unmockkObject(JacksonUtil)
    }

    @Test
    fun `getValueSet works correctly with registry that has both ConceptMap and ValueSet`() {
        mockkObject(NormalizationRegistryCache)
        mixedTestRegistry[0].set = listOf(
            TargetValue("code1", "system1", "display1", "version1")
        )
        mixedTestRegistry[2].set = listOf(
            TargetValue("code2", "system2", "display2", "version2")
        )
        NormalizationRegistryCache.setNewRegistry(mixedTestRegistry, tenant.mnemonic)

        val setAppointment = normClient.getValueSet(
            tenant,
            "Appointment.status",
            "specialAppointment"
        )
        assertEquals(
            Coding(
                code = Code("code1"),
                system = Uri("system1"),
                display = "display1".asFHIR(),
                version = "version1".asFHIR()
            ),
            setAppointment.first()
        )
        val setPatient = normClient.getValueSet(
            tenant,
            "Patient.telecom.use",
            "specialPatient"
        )
        assertEquals(
            Coding(
                code = Code("code2"),
                system = Uri("system2"),
                display = "display2".asFHIR(),
                version = "version2".asFHIR()
            ),
            setPatient.first()
        )

        vsTestRegistry.forEach { it.set = null } // reset
        unmockkObject(NormalizationRegistryCache)
    }

    @Test
    fun `getValueSet works correctly with registry that has ValueSets for 2 tenants`() {
        mockkObject(NormalizationRegistryCache)
        mixedTenantRegistry[0].set = listOf(
            TargetValue("targetValue0", "targetSystem0", "targetDisplay0", "targetVersion0")
        )
        mixedTenantRegistry[1].set = listOf(
            TargetValue("targetValue1", "targetSystem1", "targetDisplay1", "targetVersion1")
        )
        mixedTenantRegistry[2].set = listOf(
            TargetValue("targetValue2", "targetSystem2", "targetDisplay2", "targetVersion2")
        )
        mixedTenantRegistry[3].set = listOf(
            TargetValue("targetValue3", "targetSystem3", "targetDisplay3", "targetVersion3")
        )

        NormalizationRegistryCache.setNewRegistry(mixedTenantRegistry, "tenant1")

        val mappingA = normClient.getValueSet(
            tenant1,
            "Appointment.status"
        )
        assertEquals(
            Coding(
                code = Code("targetValue1"),
                system = Uri("targetSystem1"),
                display = "targetDisplay1".asFHIR(),
                version = "targetVersion1".asFHIR()
            ),
            mappingA[0]
        )

        val mappingB = normClient.getValueSet(
            tenant1,
            "Patient.telecom.use",
            "specialPatient"
        )
        assertEquals(
            Coding(
                code = Code("targetValue3"),
                system = Uri("targetSystem3"),
                display = "targetDisplay3".asFHIR(),
                version = "targetVersion3".asFHIR()
            ),
            mappingB[0]
        )

        NormalizationRegistryCache.setNewRegistry(mixedTenantRegistry, "tenant2")

        val mappingC = normClient.getValueSet(
            tenant2,
            "Appointment.status"
        )
        assertEquals(
            Coding(
                code = Code("targetValue0"),
                system = Uri("targetSystem0"),
                display = "targetDisplay0".asFHIR(),
                version = "targetVersion0".asFHIR()
            ),
            mappingC[0]
        )

        val mappingD = normClient.getValueSet(
            tenant2,
            "Patient.telecom.use",
            "specialPatient"
        )
        assertEquals(
            Coding(
                code = Code("targetValue2"),
                system = Uri("targetSystem2"),
                display = "targetDisplay2".asFHIR(),
                version = "targetVersion2".asFHIR()
            ),
            mappingD[0]
        )

        mixedTenantRegistry.forEach { it.set = null } // reset
        unmockkObject(NormalizationRegistryCache)
    }

    @Test
    fun `maps and sets are set on first read`() {
        mockkObject(NormalizationRegistryCache)
        mockkObject(JacksonUtil)
        val client2 = NormalizationRegistryClient(ociClient, registryFile)
        val mockkSet = mockk<ValueSet> {
            every { expansion?.contains } returns listOf(
                mockk {
                    every { system?.value.toString() } returns "system1"
                    every { version?.value.toString() } returns "version1"
                    every { code?.value.toString() } returns "code1"
                    every { display?.value.toString() } returns "display1"
                }
            )
        }

        val mockConeptMap = mockk<ConceptMap> {
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
        val testRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "unique",
                filename = "fake1.json",
                concept_map_name = "ok",
                concept_map_uuid = "cm",
                version = "1",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = "tenant",
                profile_url = null
            ),
            NormalizationRegistryItem(
                data_element = "Observation.coding",
                registry_uuid = "unique2",
                filename = "fake2.json",
                value_set_name = "a valueset",
                value_set_uuid = "vs",
                version = "1",
                source_extension_url = "ext1",
                resource_type = "Observation",
                tenant_id = "tenant",
                profile_url = null
            )
        )

        every { ociClient.getObjectFromINFX("DataNormalizationRegistry/v2/registry.json") } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns testRegistry
        every { ociClient.getObjectFromINFX("fake1.json") } returns "mockConceptMap"
        every { ociClient.getObjectFromINFX("fake2.json") } returns "mockValueSet"
        every { JacksonUtil.readJsonObject("mockConceptMap", ConceptMap::class) } returns mockConeptMap
        every { JacksonUtil.readJsonObject("mockValueSet", ValueSet::class) } returns mockkSet

        client2.reload(tenant.mnemonic)

        val actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("unique", actual[0].registry_uuid)
        assertEquals("unique2", actual[1].registry_uuid)
        assertTrue(actual[0].map?.isNotEmpty() == true) // map was set
        assertTrue(actual[0].set.isNullOrEmpty())
        assertTrue(actual[1].set?.isNotEmpty() == true) // set was set
        assertTrue(actual[1].map.isNullOrEmpty())

        unmockkObject(JacksonUtil)
        unmockkObject(NormalizationRegistryCache)
    }

    @Test
    fun `reload works correctly when we've added a new tenant object`() {
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
        every { ociClient.getObjectFromINFX(registryFile) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2

        mockkObject(NormalizationRegistryCache)
        normClient.reload(tenant.mnemonic)
        var actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("12345", actual[0].registry_uuid)
        assertEquals("67890", actual[1].registry_uuid)

        // adding a new entry gets added to cache after reload
        val testRegistry2 = cmTestRegistry + NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "newUUID",
            filename = "newFile",
            concept_map_name = "AppointmentStatusNew",
            concept_map_uuid = "cm-555",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Appointment",
            tenant_id = "tenant"
        ) + NormalizationRegistryItem(
            data_element = "Appointment.status",
            registry_uuid = "newUUID2",
            filename = "newFile",
            concept_map_name = "AppointmentStatusNew",
            concept_map_uuid = "cm-555",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Appointment",
            tenant_id = "tenantNotAskedFor"
        )

        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns testRegistry2
        every { ociClient.getObjectFromINFX("newFile") } returns "mapJson3"
        every { JacksonUtil.readJsonObject("mapJson3", ConceptMap::class) } returns mockkMap1
        normClient.reload(tenant.mnemonic)
        actual = NormalizationRegistryCache.getCurrentRegistry()
        assertEquals(3, actual.size)
        assertEquals("12345", actual[0].registry_uuid)
        assertEquals("67890", actual[1].registry_uuid)
        assertEquals("newUUID", actual[2].registry_uuid)
        unmockkObject(JacksonUtil)
        unmockkObject(NormalizationRegistryCache)
    }
}
