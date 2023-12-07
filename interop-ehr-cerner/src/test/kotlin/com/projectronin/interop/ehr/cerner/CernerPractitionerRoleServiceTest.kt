package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.outputs.FindPractitionersResponse
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.statement.HttpResponse
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CernerPractitionerRoleServiceTest {
    private lateinit var cernerClient: CernerClient
    private lateinit var httpResponse: HttpResponse
    private lateinit var practitionerRoleService: CernerPractitionerRoleService
    private lateinit var tenant: Tenant

    @BeforeEach
    fun setup() {
        cernerClient = mockk()
        httpResponse = mockk()
        tenant = mockk()
        practitionerRoleService = CernerPractitionerRoleService(cernerClient)
    }

    @Test
    fun `getById returns an empty PractitionerRole`() {
        val actualResult = practitionerRoleService.getByID(tenant, "123")
        val expectedResult = PractitionerRole()

        assertEquals(actualResult, expectedResult)
    }

    @Test
    fun `getByIDs returns empty-map`() {
        val response = practitionerRoleService.getByIDs(tenant, listOf("12345678", "87654321"))
        assertEquals(response.size, 0)
    }

    @Test
    fun `findPractitionersByLocation returns empty result`() {
        val actualResult = practitionerRoleService.findPractitionersByLocation(tenant, listOf("123", "321"))

        val expectedResult =
            FindPractitionersResponse(
                Bundle(
                    type = null,
                ),
            )

        assertEquals(expectedResult.resource, actualResult.resource)
        assertEquals(emptyList<Resource<*>>(), actualResult.resources)
        assertEquals(emptyList<PractitionerRole>(), actualResult.practitionerRoles)
        assertEquals(emptyList<Practitioner>(), actualResult.practitioners)
        assertEquals(emptyList<Location>(), actualResult.locations)
    }
}
