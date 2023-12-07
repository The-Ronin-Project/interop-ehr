package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UnlocalizeTest {
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @Test
    fun `removes tenant mnemonic from String`() {
        assertEquals("some string", "test-some string".unlocalize(tenant))
    }

    @Test
    fun `localize, unlocalize String`() {
        assertEquals("id-value", "id-value".localize(tenant).unlocalize(tenant))
    }

    @Test
    fun `removes tenant mnemonic from Id`() {
        assertEquals(Id("id-value"), Id("test-id-value").unlocalize(tenant))
    }

    @Test
    fun `tenant mnemonic is not in the Id`() {
        val idValue = "tenant-id-value"
        assertFalse(idValue.contains(tenant.mnemonic))
        assertEquals(Id(idValue), Id(idValue).unlocalize(tenant))
    }

    @Test
    fun `tenant mnemonic is in the Id, but it is not a prefix on the Id`() {
        val idValue = "tenant-id-test-value"
        assertTrue(idValue.contains(tenant.mnemonic))
        assertEquals(Id(idValue), Id(idValue).unlocalize(tenant))
    }

    @Test
    fun `removes tenant mnemonic from List of String`() {
        val localizedIdList = listOf("test-A", "test-B", "test-C", "test-D", "test-E")
        val idList = listOf("A", "B", "C", "D", "E")
        assertEquals(idList, localizedIdList.unlocalize(tenant))
    }

    @Test
    fun `tenant mnemonic is not in every member of the List of String`() {
        val localizedIdList = listOf("tenant-A", "test-B", "tenacious-C", "testimonial-D", "test-E")
        val idList = listOf("tenant-A", "B", "tenacious-C", "testimonial-D", "E")
        assertEquals(idList, localizedIdList.unlocalize(tenant))
    }

    @Test
    fun `tenant mnemonic is not in any member of the List of String`() {
        val localizedIdList = listOf("tenant-A", "testament-B", "tenacious-C", "testimonial-D", "tern-E")
        assertEquals(localizedIdList, localizedIdList.unlocalize(tenant))
    }
}
