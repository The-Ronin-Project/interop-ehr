package com.projectronin.interop.ehr.model.base

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.enums.DataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JSONResourceTest {
    @Test
    fun `creates json object`() {
        val resourceObject = "Bundle String"
        val resource = SampleJSONResource(resourceObject)

        assertEquals("\"$resourceObject\"", resource.raw)
        assertEquals(resourceObject, resource.resource)
    }

    @Test
    fun `equals and hashCode work`() {
        val resourceObject = "Bundle String"
        val resource1 = SampleJSONResource(resourceObject)
        val resource2 = SampleJSONResource(resourceObject)
        val resource3 = SampleJSONResource("Other")

        assertTrue(resource1 == resource1)
        assertFalse(resource1.equals(null))
        assertTrue(resource1 == resource2)
        assertTrue(resource1 != resource3)
        assertFalse(resource1.equals(resourceObject))
        assertTrue(resource1.hashCode() == resource2.hashCode())
    }
}

class SampleJSONResource(
    resource: Any,
    override val dataSource: DataSource = DataSource.FHIR_R4,
    override val resourceType: ResourceType = ResourceType.PATIENT
) : JSONResource(resource)
