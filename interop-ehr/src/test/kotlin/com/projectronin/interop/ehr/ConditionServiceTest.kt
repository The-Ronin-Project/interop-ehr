package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConditionServiceTest {
    // Ideally we'd just mockk the ObservationService interface, but mockk has some issues with that
    // approach.  https://github.com/mockk/mockk/issues/64

    abstract class ConditionServiceMock : ConditionService {
        override fun findConditionsByCodes(
            tenant: Tenant,
            patientFhirId: String,
            conditionCategoryCodes: List<FHIRSearchToken>,
            clinicalStatusCodes: List<FHIRSearchToken>
        ): List<Condition> {
            if (clinicalStatusCodes.isEmpty()) {
                return listOf(mockk { every { id?.value } returns "mocked!" })
            } else {
                throw NotImplementedError("Non defaults don't work")
            }
        }
    }
    private val conditionService = spyk<ConditionServiceMock>()

    @Test
    fun `default is applied`() {
        val tenant = mockk<Tenant>()
        val result = conditionService.findConditionsByCodes(tenant, "fhirId", emptyList())
        assertEquals("mocked!", result.first().id?.value)
    }
}
