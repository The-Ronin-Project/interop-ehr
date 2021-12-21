package com.projectronin.interop.ehr.model.base

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.enums.DataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaseFHIRResourceTest {
    @Test
    fun `creates json object`() {
        val json = """{"id":1,"name":"Name"}"""
        val resource = SampleFHIRResource(json)

        // The jsonObject is not in scope here, so we're using a method to expose its details.
        assertEquals(json, resource.rawJson())
        assertEquals(json, resource.raw)
    }

    @Test
    fun `equals and hashCode work`() {
        val json = """{"id":1,"name":"Name"}"""
        val resource1 = SampleFHIRResource(json)
        val resource2 = SampleFHIRResource(json)
        val resource3 = SampleFHIRResource("{}")

        assertTrue(resource1 == resource1)
        assertFalse(resource1.equals(null))
        assertTrue(resource1 == resource2)
        assertTrue(resource1 != resource3)
        assertFalse(resource1.equals(json))
        assertTrue(resource1.hashCode() == resource2.hashCode())
    }
}

class SampleFHIRResource(
    private val rawString: String,
    override val dataSource: DataSource = DataSource.FHIR_R4,
    override val resourceType: ResourceType = ResourceType.PATIENT
) :
    FHIRResource(rawString) {
    fun rawJson() = jsonObject.toJsonString()
}
