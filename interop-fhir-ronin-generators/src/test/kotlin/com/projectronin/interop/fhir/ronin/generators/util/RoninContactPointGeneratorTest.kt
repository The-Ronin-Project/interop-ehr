package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RoninContactPointGeneratorTest {

    @Test
    fun `generates valid ContactPoint`() {
        // TODO so I don't break anything
        val roninTelecom = RoninContactPointGenerator().generate()
        Assertions.assertEquals(ContactPoint(), roninTelecom)
    }
}
