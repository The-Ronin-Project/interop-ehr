package com.projectronin.interop.ehr.model.base

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaseFHIRBundleTest {
    @Test
    fun `creates json object`() {
        val json = """{"id":1,"name":"Name"}"""
        val bundle = SampleFHIRBundle(json)

        // The jsonObject is not in scope here, so we're using a method to expose its details.
        assertEquals(json, bundle.rawJson())
        assertEquals(json, bundle.raw)
        assertEquals(ResourceType.BUNDLE, bundle.resourceType)
    }

    @Test
    fun `equals and hashCode work`() {
        val json = """{"id":1,"name":"Name"}"""
        val bundle1 = SampleFHIRBundle(json)
        val bundle2 = SampleFHIRBundle(json)
        val bundle3 = SampleFHIRBundle("{}")

        assertTrue(bundle1 == bundle1)
        assertFalse(bundle1.equals(null))
        assertTrue(bundle1 == bundle2)
        assertTrue(bundle1 != bundle3)
        assertFalse(bundle1.equals(json))
        assertTrue(bundle1.hashCode() == bundle2.hashCode())
    }

    @Test
    fun `finds next url`() {
        val json = """{"id":1,"name":"Name"}"""
        val bundle = SampleFHIRBundle(json)

        assertEquals("www.test.com", bundle.getURL("next"))
    }

    @Test
    fun `no url returns null`() {
        val json = """{"id":1,"name":"Name"}"""
        val bundle = SampleFHIRBundle(json)

        assertNull(bundle.getURL("previous"))
    }
}

class SampleFHIRBundle(
    private val rawString: String,
    override val dataSource: DataSource = DataSource.FHIR_R4,
    override val resources: List<SampleFHIRResource> = listOf(),
    override val links: List<Link> = listOf(SampleLink(rawString))
) : FHIRBundle<SampleFHIRResource>(rawString) {
    fun rawJson() = jsonObject.toJsonString()
}

class SampleLink(override val raw: String) : Link {
    override val relation = "next"
    override val url = Uri("www.test.com")
}
