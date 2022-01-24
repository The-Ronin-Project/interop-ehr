package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.enums.DataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicConditionBundleTest {
    @Test
    fun `can build from JSON`() {
        val json = this::class.java.getResource("/ExampleConditionBundle.json")!!.readText()

        val conditionBundle = EpicConditionBundle(json)
        assertEquals(json, conditionBundle.raw)
        assertEquals(DataSource.FHIR_R4, conditionBundle.dataSource)
        assertEquals(ResourceType.BUNDLE, conditionBundle.resourceType)
        assertEquals(7, conditionBundle.resources.size)
        assertEquals(1, conditionBundle.links.size)
    }
}
