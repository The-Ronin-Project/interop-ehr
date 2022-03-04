package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.epic.readResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.valueset.BundleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicFindPractitionersResponseTest {
    @Test
    fun `can build from object`() {
        val response = readResource<Bundle>("/ExampleFindPractitionersResponse.json")

        val findPractitionersResponse = EpicFindPractitionersResponse(response)

        assertEquals(DataSource.FHIR_R4, findPractitionersResponse.dataSource)
        assertEquals(ResourceType.BUNDLE, findPractitionersResponse.resourceType)
        assertEquals(1, findPractitionersResponse.links.size)

        // PractitionerRoles
        val practitionerRoles = findPractitionersResponse.practitionerRoles
        assertEquals(71, practitionerRoles.resources.size)
        assertEquals(DataSource.FHIR_R4, practitionerRoles.dataSource)
        assertEquals(ResourceType.BUNDLE, practitionerRoles.resourceType)
        assertEquals(1, practitionerRoles.links.size)

        // Practitioners
        val practitioners = findPractitionersResponse.practitioners
        assertEquals(71, practitioners.resources.size)
        assertEquals(DataSource.FHIR_R4, practitioners.dataSource)
        assertEquals(ResourceType.BUNDLE, practitioners.resourceType)
        assertEquals(1, practitioners.links.size)

        // Locations
        val locations = findPractitionersResponse.locations
        assertEquals(53, locations.resources.size)
        assertEquals(DataSource.FHIR_R4, practitioners.dataSource)
        assertEquals(ResourceType.BUNDLE, practitioners.resourceType)
        assertEquals(1, locations.links.size)
    }

    @Test
    fun `can build old style with duplicates from JSON`() {
        // Old style of this API puts the provider after each resource instead of all providers at the bottom, and can
        // have duplicate providers. In either style, providers and locations are mixed and locations may be duplicates.
        val response = readResource<Bundle>("/ExampleFindPractitionersResponseWithDuplicates.json")

        val findPractitionersResponse = EpicFindPractitionersResponse(response)

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

        // Locations
        val locations = findPractitionersResponse.locations
        assertEquals(1, practitioners.resources.size)
        assertEquals(DataSource.FHIR_R4, practitioners.dataSource)
        assertEquals(ResourceType.BUNDLE, practitioners.resourceType)
        assertEquals(1, locations.links.size)
    }

    @Test
    fun `check that no resources are handled`() {
        val response = Bundle(id = Id("123"), type = BundleType.SEARCHSET)

        val findPractitionersResponse = EpicFindPractitionersResponse(response)
        assertEquals(0, findPractitionersResponse.links.size)

        // PractitionerRoles
        val practitionerRoles = findPractitionersResponse.practitionerRoles
        assertEquals(0, practitionerRoles.resources.size)
        assertEquals(0, practitionerRoles.links.size)

        // Practitioners
        val practitioners = findPractitionersResponse.practitioners
        assertEquals(0, practitioners.resources.size)
        assertEquals(0, practitionerRoles.links.size)

        // Locations
        val locations = findPractitionersResponse.locations
        assertEquals(0, practitioners.resources.size)
        assertEquals(0, locations.links.size)
    }
}
