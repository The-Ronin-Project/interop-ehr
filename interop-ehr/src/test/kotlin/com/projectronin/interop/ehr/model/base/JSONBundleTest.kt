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

class JSONBundleTest {
    @Test
    fun `creates json object`() {
        val bundleObject = "Bundle String"
        val bundle = SampleJSONBundle(bundleObject)

        assertEquals("\"$bundleObject\"", bundle.raw)
        assertEquals(bundleObject, bundle.resource)
        assertEquals(ResourceType.BUNDLE, bundle.resourceType)
    }

    @Test
    fun `equals and hashCode work`() {
        val bundleObject = "Bundle String"
        val bundle1 = SampleJSONBundle(bundleObject)
        val bundle2 = SampleJSONBundle(bundleObject)
        val bundle3 = SampleJSONBundle("Other")

        assertTrue(bundle1 == bundle1)
        assertFalse(bundle1.equals(null))
        assertTrue(bundle1 == bundle2)
        assertTrue(bundle1 != bundle3)
        assertFalse(bundle1.equals(bundleObject))
        assertTrue(bundle1.hashCode() == bundle2.hashCode())
    }

    @Test
    fun `finds next url`() {
        val bundleObject = "Bundle String"
        val bundle = SampleJSONBundle(bundleObject)

        assertEquals("www.test.com", bundle.getURL("next"))
    }

    @Test
    fun `no url returns null`() {
        val bundleObject = "Bundle String"
        val bundle = SampleJSONBundle(bundleObject)

        assertNull(bundle.getURL("previous"))
    }
}

class SampleJSONBundle(
    resource: String,
    override val dataSource: DataSource = DataSource.FHIR_R4,
    override val resources: List<SampleJSONResource> = listOf(),
    override val links: List<Link> = listOf(SampleLink(resource))
) : JSONBundle<SampleJSONResource, String>(resource)

class SampleLink(override val element: Any) : Link {
    override val raw = element.toString()
    override val relation = "next"
    override val url = Uri("www.test.com")
}
