package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Condition
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality for an EHR's condition service.
 */
interface ConditionService {
    /**
     * Finds the [Bundle] of [Condition]s associated with the requested [tenant], [patientFhirId],
     * [conditionCategoryCode] and [clinicalStatus].
     */
    fun findConditions(tenant: Tenant, patientFhirId: String, conditionCategoryCode: String, clinicalStatus: String):
        Bundle<Condition>
}
