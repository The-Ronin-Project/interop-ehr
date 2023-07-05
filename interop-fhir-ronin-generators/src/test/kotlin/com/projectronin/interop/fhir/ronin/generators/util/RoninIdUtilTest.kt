package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.test.data.generator.NullDataGenerator
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninIdUtilTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `generate fhir id value when no id input is provided`() {
        val value = fhirIdValue()
        assertTrue(value.isNotEmpty())
    }

    @Test
    fun `fhir id value returns a non-empty non-udp id unchanged when tenant id is not provided`() {
        val value = fhirIdValue("0987-12345-67890")
        assertEquals("0987-12345-67890", value)
    }

    @Test
    fun `fhir id value strips tenant prefix when both udp id and tenant id are provided`() {
        val value = fhirIdValue("test-12345-67890", "test")
        assertEquals("12345-67890", value)
    }

    @Test
    fun `fhir id value returns a non-empty id unchanged when tenant id is null`() {
        val value = fhirIdValue("0987-12345-67890", null)
        assertEquals("0987-12345-67890", value)
    }

    @Test
    fun `fhir id value returns a non-empty id unchanged when tenant id is empty`() {
        val value = fhirIdValue("0987-12345-67890", "")
        assertEquals("0987-12345-67890", value)
    }

    @Test
    fun `fhir id value fails when id input is provided and is bad fhir`() {
        val exception = assertThrows<IllegalArgumentException> {
            fhirIdValue("name@org")
        }
        assertEquals("name@org is invalid as a FHIR id", exception.message)
    }

    @Test
    fun `generate udp id value when no id input is provided`() {
        val value = udpIdValue(tenant.mnemonic)
        assertTrue(value.split("/").last().startsWith("test-") == true)
    }

    @Test
    fun `generate udp id value when tenant and fhir id are provided`() {
        val value = udpIdValue(tenant.mnemonic, "12345")
        assertEquals("test-12345", value)
    }

    @Test
    fun `udp id value avoids re-prefixing the tenant when a udp id string is provided`() {
        val value = udpIdValue(tenant.mnemonic, "test-12345")
        assertEquals("test-12345", value)
    }

    @Test
    fun `use supplied Id value when valid for tenant`() {
        val inputId = Id("test-aaa")
        val resultId = generateUdpId(inputId, tenant.mnemonic)
        assertEquals(inputId, resultId)
    }

    @Test
    fun `normalize Id value to tenant`() {
        val inputId = Id("aaa")
        val resultId = generateUdpId(inputId, tenant.mnemonic)
        assertEquals(Id("test-aaa"), resultId)
    }

    @Test
    fun `generate udp id when null id generator is provided and no fhir id is provided`() {
        val inputId = NullDataGenerator<Id?>().generate()
        val resultId = generateUdpId(inputId, tenant.mnemonic)
        assertTrue(resultId.value?.startsWith("test-") == true)
    }
}
