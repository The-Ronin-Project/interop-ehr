package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.epic.readResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.resource.Bundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicConditionBundleTest {
    @Test
    fun `can build from object`() {
        val bundle = readResource<Bundle>("/ExampleConditionBundle.json")
        val json = this::class.java.getResource("/ExampleConditionBundle.json")!!.readText()

        val conditionBundle = EpicConditionBundle(bundle)
        assertEquals(bundle, conditionBundle.resource)
        assertEquals(deformat(json), conditionBundle.raw)
        assertEquals(DataSource.FHIR_R4, conditionBundle.dataSource)
        assertEquals(ResourceType.BUNDLE, conditionBundle.resourceType)
        assertEquals(7, conditionBundle.resources.size)
        assertEquals(1, conditionBundle.links.size)
    }
}
