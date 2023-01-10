package com.projectronin.interop.ehr.outputs

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.valueset.BundleType
import com.projectronin.interop.fhir.util.asCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FindPractitionersResponseTest {

    @Test
    fun `test everything`() {
        val entries = mutableListOf<BundleEntry>()
        entries.add(BundleEntry(resource = Practitioner(id = Id("PRACT1"))))
        entries.add(BundleEntry(resource = PractitionerRole(id = Id("PROLE1"))))
        entries.add(BundleEntry(resource = Location(id = Id("LOCALE1"))))
        val bundle = Bundle(
            id = Id("123"),
            type = BundleType.BATCH_RESPONSE.asCode(),
            entry = entries
        )
        val response = FindPractitionersResponse(bundle)
        assertEquals(response.practitioners[0].id?.value, "PRACT1")
        assertEquals(response.practitionerRoles[0].id?.value, "PROLE1")
        assertEquals(response.locations[0].id?.value, "LOCALE1")
        assertEquals(response.resources.size, 3)
    }
}
