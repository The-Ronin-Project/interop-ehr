package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninMetaUtilTest {

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `generate meta`() {
        val roninMeta = rcdmMeta(RoninProfile.PATIENT, tenant.mnemonic) {}
        assertEquals("http://projectronin.io/fhir/StructureDefinition/ronin-patient", roninMeta.profile[0].value)
        assertEquals("test", roninMeta.source!!.value)
    }

    @Test
    fun `generates meta with additional values`() {
        val roninMeta = rcdmMeta(RoninProfile.APPOINTMENT, tenant.mnemonic) {
            versionId of Id("x")
        }
        assertEquals("http://projectronin.io/fhir/StructureDefinition/ronin-appointment", roninMeta.profile[0].value)
        assertEquals("test", roninMeta.source!!.value)
        assertEquals("x", roninMeta.versionId!!.value)
    }
}
