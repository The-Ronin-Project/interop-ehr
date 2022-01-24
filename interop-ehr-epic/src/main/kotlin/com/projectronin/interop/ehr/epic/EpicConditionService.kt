package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.ConditionService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicConditionBundle
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Condition
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Service providing access to conditions within Epic.
 */
@Component
class EpicConditionService(epicClient: EpicClient) : ConditionService, EpicPagingService(epicClient) {
    private val conditionSearchUrlPart = "/api/FHIR/R4/Condition"

    override fun findConditions(
        tenant: Tenant,
        patientFhirId: String,
        conditionCategoryCode: String,
        clinicalStatus: String,
    ): Bundle<Condition> {
        val parameters = mapOf(
            "patient" to patientFhirId,
            "category" to conditionCategoryCode,
            "clinical-status" to clinicalStatus
        )

        return getBundleWithPaging(tenant, conditionSearchUrlPart, parameters, ::EpicConditionBundle)
    }
}
