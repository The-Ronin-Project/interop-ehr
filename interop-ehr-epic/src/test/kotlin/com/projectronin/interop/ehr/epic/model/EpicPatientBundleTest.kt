package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.ehr.epic.readResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.resource.Bundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicPatientBundleTest {
    @Test
    fun `can build from object`() {
        val bundle = readResource<Bundle>("/ExamplePatientBundle.json")
        val json = this::class.java.getResource("/ExamplePatientBundle.json")!!.readText()

        val patientBundle = EpicPatientBundle(bundle)
        assertEquals(bundle, patientBundle.resource)
        assertEquals(deformat(json), patientBundle.raw)
        assertEquals(DataSource.FHIR_R4, patientBundle.dataSource)
        assertEquals(ResourceType.BUNDLE, patientBundle.resourceType)
        assertEquals(1, patientBundle.resources.size)
        assertEquals(1, patientBundle.links.size)
    }
}
