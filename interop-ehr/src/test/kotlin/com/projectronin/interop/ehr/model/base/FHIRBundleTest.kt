package com.projectronin.interop.ehr.model.base

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.valueset.BundleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FHIRBundleTest {
    @Test
    fun `creates json object`() {
        val bundleObject = Bundle(id = Id("123"), type = BundleType.SEARCHSET)
        val bundle = SampleFHIRBundle(bundleObject)

        assertEquals("""{"resourceType":"Bundle","id":"123","type":"searchset"}""", bundle.raw)
        assertEquals(bundleObject, bundle.resource)
        assertEquals(ResourceType.BUNDLE, bundle.resourceType)
    }

    @Test
    fun `equals and hashCode work`() {
        val bundleObject = Bundle(id = Id("123"), type = BundleType.SEARCHSET)
        val bundle1 = SampleFHIRBundle(bundleObject)
        val bundle2 = SampleFHIRBundle(bundleObject)
        val bundle3 = SampleFHIRBundle(Bundle(id = Id("456"), type = BundleType.BATCH))

        assertTrue(bundle1 == bundle1)
        assertFalse(bundle1.equals(null))
        assertTrue(bundle1 == bundle2)
        assertTrue(bundle1 != bundle3)
        assertFalse(bundle1.equals(bundleObject))
        assertTrue(bundle1.hashCode() == bundle2.hashCode())
    }

    @Test
    fun `finds next url`() {
        val bundleObject = Bundle(id = Id("123"), type = BundleType.SEARCHSET)
        val bundle = SampleFHIRBundle(bundleObject)

        assertEquals("www.test.com", bundle.getURL("next"))
    }

    @Test
    fun `no url returns null`() {
        val bundleObject = Bundle(id = Id("123"), type = BundleType.SEARCHSET)
        val bundle = SampleFHIRBundle(bundleObject)

        assertNull(bundle.getURL("previous"))
    }
}

class SampleFHIRBundle(
    resource: Bundle,
    override val dataSource: DataSource = DataSource.FHIR_R4,
    override val resources: List<Patient> = listOf(),
    override val links: List<Link> = listOf(SampleLink(resource))
) : FHIRBundle<Patient>(resource)
