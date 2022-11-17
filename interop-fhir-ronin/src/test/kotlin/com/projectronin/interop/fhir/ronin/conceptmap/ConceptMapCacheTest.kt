package com.projectronin.interop.fhir.ronin.conceptmap

import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ConceptMap
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class ConceptMapCacheTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "tenant"
    }
    private val ociClient = mockk<OCIClient>()
    private val cmClient = ConceptMapClient(ociClient)
    private val testRegistry = listOf(
        ConceptMapRegistry(
            data_element = "Appointment.status",
            registry_uuid = "12345",
            filename = "file1.json",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Appointment",
            tenant_id = "tenant"
        ),
        ConceptMapRegistry(
            data_element = "Patient.telecom.use",
            registry_uuid = "67890",
            filename = "file2.json",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Patient",
            tenant_id = "tenant"
        )
    )

    @Test
    fun `reload works correctly`() {
        val mockkMap1 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem"
                    every { source?.value } returns "sourceSystem"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueA"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueA"
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueB"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueB"
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
                    every { source?.value } returns "sourceSystem2"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValue2"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValue2"
                                }
                            )
                        }
                    )
                }
            )
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX("DataNormalizationRegistry/v1/registry.json") } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", ConceptMapRegistry::class) } returns testRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2

        mockkObject(ConceptMapCache)
        cmClient.reload(tenant)
        var actual = ConceptMapCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("12345", actual[0].registry_uuid)
        assertEquals("67890", actual[1].registry_uuid)

        // adding a new entry gets added to cache after reload
        val testRegistry2 = testRegistry + ConceptMapRegistry(
            data_element = "Appointment.status",
            registry_uuid = "newUUID",
            filename = "newFile",
            version = "1",
            source_extension_url = "ext1",
            resource_type = "Appointment",
            tenant_id = "tenant"
        )
        every { JacksonUtil.readJsonList("registryJson", ConceptMapRegistry::class) } returns testRegistry2
        every { ociClient.getObjectFromINFX("newFile") } returns "mapJson3"
        every { JacksonUtil.readJsonObject("mapJson3", ConceptMap::class) } returns mockkMap1
        cmClient.reload(tenant)
        actual = ConceptMapCache.getCurrentRegistry()
        assertEquals(3, actual.size)
        assertEquals("12345", actual[0].registry_uuid)
        assertEquals("67890", actual[1].registry_uuid)
        assertEquals("newUUID", actual[2].registry_uuid)
        // --
        // reload if version changes, also make sure if entries are deleted we remove them from cache
        val testRegistry3 = listOf(
            ConceptMapRegistry(
                data_element = "Appointment.status",
                registry_uuid = "12345",
                filename = "file2.json",
                version = "2",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = "tenant"
            ),
            ConceptMapRegistry(
                data_element = "Appointment.status",
                registry_uuid = "nullTenantUUID",
                filename = "universal.json",
                version = "1",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = null
            )
        )
        every { JacksonUtil.readJsonList("registryJson", ConceptMapRegistry::class) } returns testRegistry3
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        every { ociClient.getObjectFromINFX("universal.json") } returns "universalJson"
        every { JacksonUtil.readJsonObject("universalJson", ConceptMap::class) } returns mockkMap2
        cmClient.reload(tenant)
        actual = ConceptMapCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("12345", actual[0].registry_uuid)
        assertEquals("nullTenantUUID", actual[1].registry_uuid)
        assertEquals("2", actual[0].version)
        assertEquals(
            mapOf(
                SourceKey("sourceValue2", "sourceSystem2")
                    to TargetValue("targetValue2", "targetSystem2")
            ),
            actual[0].map
        )
        // --
        // don't reload if version hasn't changed
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        cmClient.reload(tenant)
        actual = ConceptMapCache.getCurrentRegistry()
        assertEquals(2, actual.size)
        assertEquals("12345", actual[0].registry_uuid)
        assertEquals("2", actual[0].version)
        assertEquals(
            mapOf(
                SourceKey("sourceValue2", "sourceSystem2")
                    to TargetValue("targetValue2", "targetSystem2")
            ),
            actual[0].map
        )
        // --
        unmockkObject(JacksonUtil)
        unmockkObject(ConceptMapCache)
    }

    @Test
    fun `getConceptMap works correctly`() {
        mockkObject(ConceptMapCache)
        testRegistry[0].map = mapOf(
            SourceKey("sourceValue1", "sourceSystem1")
                to TargetValue("targetValue1", "targetSystem1")
        )
        testRegistry[1].map = mapOf(
            SourceKey("sourceValue2", "sourceSystem2")
                to TargetValue("targetValue2", "targetSystem2")
        )
        val sourceCoding = Coding(code = Code("sourceValue1"), system = Uri("sourceSystem1"))
        ConceptMapCache.setNewRegistry(testRegistry, tenant)
        // calling the client here tests the 'reload' line of code
        val mapped = cmClient.getConceptMapping(
            tenant,
            "Appointment",
            "Appointment.status",
            sourceCoding
        )
        assertEquals(Coding(code = Code("targetValue1"), system = Uri("targetSystem1")), mapped?.first)
        assertEquals("ext1", mapped?.second?.url?.value)
        assertEquals(sourceCoding, mapped?.second?.value?.value)

        testRegistry.forEach { it.map = null } // reset
        unmockkObject(ConceptMapCache)
    }

    @Test
    fun `null logic works`() {
        val badTenant = mockk<Tenant> {
            every { mnemonic } returns "badTenant"
        }
        mockkObject(ConceptMapCache)
        ConceptMapCache.setNewRegistry(testRegistry, tenant)
        ConceptMapCache.setNewRegistry(testRegistry, badTenant) // avoid reloads
        val sourceCoding = Coding(code = Code("sourceValue1"), system = Uri("sourceSystem1"))
        // bad tenant
        assertNull(cmClient.getConceptMapping(badTenant, "Appointment", "Appointment.status", sourceCoding))
        // bad resource type
        assertNull(cmClient.getConceptMapping(tenant, "Location", "Location.status", sourceCoding))
        // bad element type
        assertNull(cmClient.getConceptMapping(tenant, "Appointment", "Appointment.telecom.use", sourceCoding))
        // everything good but registry has no map
        assertNull(cmClient.getConceptMapping(tenant, "Appointment", "Appointment.status", sourceCoding))
        // everything good, map exists, but look up value isn't mapped
        testRegistry[0].map = mapOf(
            SourceKey("sourceValuebad", "sourceSystembad")
                to TargetValue("targetValue1", "targetSystem1")
        )
        assertNull(cmClient.getConceptMapping(tenant, "Appointment", "Appointment.status", sourceCoding))

        unmockkObject(ConceptMapCache)
    }

    @Test
    fun `try catch works`() {
        mockkObject(JacksonUtil)
        every { JacksonUtil.readJsonObject(any(), ConceptMap::class) } throws Exception("bad")
        assertEquals(emptyMap<SourceKey, TargetValue>(), cmClient.getConceptMap("name"))

        every { JacksonUtil.readJsonList(any(), ConceptMapRegistry::class) } throws Exception("bad")
        assertEquals(emptyList<ConceptMapRegistry>(), cmClient.getNewRegistry())
    }
}
