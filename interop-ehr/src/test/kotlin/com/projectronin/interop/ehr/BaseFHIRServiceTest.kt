package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.mergeBundles
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BaseFHIRServiceTest {
    private class TestFHIRService(override val standardParameters: Map<String, Any> = emptyMap()) :
        BaseFHIRService<Location>() {
        override val fhirResourceType: Class<Location> = Location::class.java

        override fun getByID(tenant: Tenant, resourceFHIRId: String): Location {
            TODO("Not yet implemented")
        }

        override fun getByIDs(tenant: Tenant, resourceFHIRIds: List<String>): Map<String, Location> {
            TODO("Not yet implemented")
        }

        fun getParameters(parameters: Map<String, Any?>): Map<String, Any?> = standardizeParameters(parameters)

        fun getMergedResponses(responses: List<Bundle>): Bundle = mergeResponses(responses)
    }

    @Test
    fun `returns standard parameters when no new parameters are provided`() {
        val standardParameters = mapOf("_count" to 20)
        val service = TestFHIRService(standardParameters)
        val newParameters = emptyMap<String, Any?>()
        val finalParameters = service.getParameters(newParameters)
        assertEquals(standardParameters, finalParameters)
    }

    @Test
    fun `returns combined parameters when non-standard parameters are provided`() {
        val standardParameters = mapOf("_count" to 20)
        val service = TestFHIRService(standardParameters)
        val newParameters = mapOf("val1" to 1, "val2" to "value")
        val finalParameters = service.getParameters(newParameters)
        assertEquals(mapOf("val1" to 1, "val2" to "value", "_count" to 20), finalParameters)
    }

    @Test
    fun `ignores standard parameters that are provided by consumer`() {
        val standardParameters = mapOf("_count" to 20)
        val service = TestFHIRService(standardParameters)
        val newParameters = mapOf("_count" to 100)
        val finalParameters = service.getParameters(newParameters)
        assertEquals(newParameters, finalParameters)
    }

    @Test
    fun `ignores _count standard parameter when _id parameter provided`() {
        val standardParameters = mapOf("_count" to 20)
        val service = TestFHIRService(standardParameters)
        val newParameters = mapOf("_id" to "id1")
        val finalParameters = service.getParameters(newParameters)
        assertEquals(newParameters, finalParameters)
    }

    @Test
    fun `includes other standard parameters when _id parameter provided`() {
        val standardParameters = mapOf("_count" to 20, "user" to "user1")
        val service = TestFHIRService(standardParameters)
        val newParameters = mapOf("_id" to "id1")
        val finalParameters = service.getParameters(newParameters)
        assertEquals(mapOf("_id" to "id1", "user" to "user1"), finalParameters)
    }

    @Test
    fun `supports merging responses with only one response`() {
        val bundle1 = mockk<Bundle>()
        val result = TestFHIRService().getMergedResponses(listOf(bundle1))
        assertEquals(bundle1, result)
    }

    @Test
    fun `supports merging multiple responses`() {
        mockkStatic("com.projectronin.interop.fhir.r4.BundleUtilKt")
        val mockkResult = mockk<Bundle>()
        every { mergeBundles(any(), any()) } returns mockkResult

        val bundle1 = mockk<Bundle>()
        val bundle2 = mockk<Bundle>()
        val bundle3 = mockk<Bundle>()

        val result = TestFHIRService().getMergedResponses(listOf(bundle1, bundle2, bundle3))
        assertEquals(mockkResult, result)

        verify(exactly = 1) { mergeBundles(bundle1, bundle2) }
        verify(exactly = 1) { mergeBundles(mockkResult, bundle3) }

        unmockkAll()
    }
}
