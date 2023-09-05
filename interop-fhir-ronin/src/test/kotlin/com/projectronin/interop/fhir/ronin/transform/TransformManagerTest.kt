package com.projectronin.interop.fhir.ronin.transform

import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.validation.ValidationClient
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TransformManagerTest {
    private val validationClient: ValidationClient = mockk()
    private val transformManager = TransformManager(validationClient)

    @Test
    fun `transform with no validation issues`() {
        val resource = mockk<Patient>()
        val tenant = mockk<Tenant>()

        val transformed = mockk<Patient>()
        val validation = mockk<Validation> {
            every { hasIssues() } returns false
        }

        val transformer = mockk<ProfileTransformer<Patient>> {
            every { transform(resource, tenant) } returns Pair(transformed, validation)
        }

        val result = transformManager.transformResource(resource, transformer, tenant)
        assertEquals(transformed, result)

        verify(exactly = 0) { validationClient.reportIssues(any(), any<Patient>(), any<Tenant>()) }
    }

    @Test
    fun `transform with validation issues and no transformation`() {
        val resource = mockk<Patient> {
            every { resourceType } returns "Patient"
        }
        val tenant = mockk<Tenant>()

        val validation = mockk<Validation> {
            every { hasIssues() } returns true
            every { issues() } returns emptyList()
        }

        val transformer = mockk<ProfileTransformer<Patient>> {
            every { transform(resource, tenant) } returns Pair(null, validation)
        }

        every { validationClient.reportIssues(validation, resource, tenant) } returns mockk()

        val result = transformManager.transformResource(resource, transformer, tenant)
        assertNull(result)

        verify(exactly = 1) { validationClient.reportIssues(validation, resource, tenant) }
    }

    @Test
    fun `transform with validation issues and a transformation`() {
        val resource = mockk<Patient>() {
            every { resourceType } returns "Patient"
        }
        val tenant = mockk<Tenant>()

        val transformed = mockk<Patient>()
        val validation = mockk<Validation> {
            every { hasIssues() } returns true
            every { issues() } returns emptyList()
        }

        val transformer = mockk<ProfileTransformer<Patient>> {
            every { transform(resource, tenant) } returns Pair(transformed, validation)
        }

        every { validationClient.reportIssues(validation, transformed, tenant) } returns mockk()

        val result = transformManager.transformResource(resource, transformer, tenant)
        assertEquals(transformed, result)

        verify(exactly = 1) { validationClient.reportIssues(validation, transformed, tenant) }
    }
}
