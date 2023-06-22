package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class RoninStringOrDARUtilTest {
    @Test
    fun `keeps provided name`() {
        val providedName = "Someplace"
        val roninLocationName = generateStringOrDAR(providedName)
        assertEquals(roninLocationName, "Someplace".asFHIR())
    }

    @Test
    fun `generates DAR if empty name is provided`() {
        val roninLocationName = generateStringOrDAR("")
        assertNotNull(roninLocationName.extension)
        assertNotNull(roninLocationName.extension[0].url)
        assertNotNull(roninLocationName.extension[0].value)
    }

    @Test
    fun `generates name if none is provided`() {
        val roninLocation = rcdmLocation("test") {
            name
        }
        assertNotNull(roninLocation.name)
    }
}
