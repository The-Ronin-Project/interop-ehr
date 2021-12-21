package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.enums.DataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicFindPractitionersResponseTest {
    @Test
    fun `can build from JSON`() {
        val json = this::class.java.getResource("/ExampleFindPractitionersResponse.json")!!.readText()

        val findPractitionersResponse = EpicFindPractitionersResponse(json)

        assertEquals(DataSource.FHIR_R4, findPractitionersResponse.dataSource)
        assertEquals(ResourceType.BUNDLE, findPractitionersResponse.resourceType)
        assertEquals(1, findPractitionersResponse.links.size)

        // PractitionerRoles
        val practitionerRoles = findPractitionersResponse.practitionerRoles
        assertEquals(64, practitionerRoles.resources.size)
        assertEquals(DataSource.FHIR_R4, practitionerRoles.dataSource)
        assertEquals(ResourceType.BUNDLE, practitionerRoles.resourceType)
        assertEquals(1, practitionerRoles.links.size)

        // Practitioners
        val practitioners = findPractitionersResponse.practitioners
        assertEquals(64, practitioners.resources.size)
        assertEquals(DataSource.FHIR_R4, practitioners.dataSource)
        assertEquals(ResourceType.BUNDLE, practitioners.resourceType)
        assertEquals(1, practitioners.links.size)
    }

    @Test
    fun `can build old style with duplicates from JSON`() {
        // Old style of this API puts the provider after each resource instead of at the bottom,
        // and can have duplicate providers.
        val json = this::class.java.getResource("/ExampleFindPractitionersResponseWithDuplicates.json")!!.readText()

        val findPractitionersResponse = EpicFindPractitionersResponse(json)

        assertEquals(DataSource.FHIR_R4, findPractitionersResponse.dataSource)
        assertEquals(ResourceType.BUNDLE, findPractitionersResponse.resourceType)
        assertEquals(1, findPractitionersResponse.links.size)

        // PractitionerRoles
        val practitionerRoles = findPractitionersResponse.practitionerRoles
        assertEquals(2, practitionerRoles.resources.size)
        assertEquals(DataSource.FHIR_R4, practitionerRoles.dataSource)
        assertEquals(ResourceType.BUNDLE, practitionerRoles.resourceType)
        assertEquals(1, practitionerRoles.links.size)

        // Practitioners
        val practitioners = findPractitionersResponse.practitioners
        assertEquals(1, practitioners.resources.size)
        assertEquals(DataSource.FHIR_R4, practitioners.dataSource)
        assertEquals(ResourceType.BUNDLE, practitioners.resourceType)
        assertEquals(1, practitioners.links.size)
    }

    @Test
    fun `check that no resources are handled`() {
        val json = """
            |{
            |  "Entry": null
            |}""".trimMargin()

        val findPractitionersResponse = EpicFindPractitionersResponse(json)
        assertEquals(0, findPractitionersResponse.links.size)

        // PractitionerRoles
        val practitionerRoles = findPractitionersResponse.practitionerRoles
        assertEquals(0, practitionerRoles.resources.size)
        assertEquals(0, practitionerRoles.links.size)

        // Practitioners
        val practitioners = findPractitionersResponse.practitioners
        assertEquals(0, practitioners.resources.size)
        assertEquals(0, practitionerRoles.links.size)
    }
}
