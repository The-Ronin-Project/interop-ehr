package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality for an EHR's condition service.
 */
interface ConditionService {
    /**
     * Finds the [List] of [Condition]s associated with the requested [tenant], [patientFhirId],
     * [conditionCategoryCode] and [clinicalStatus].
     * Supports one code or system|value token for category or clinicalStatus.
     */
    fun findConditions(tenant: Tenant, patientFhirId: String, conditionCategoryCode: String, clinicalStatus: String):
        List<Condition>

    /**
     * Finds the [List] of [Condition]s associated with the requested [tenant], [patientFhirId],
     * [conditionCategoryCodes] and [clinicalStatusCodes].
     * Supports lists of codes or system|value tokens for category or clinicalStatus.
     */
    fun findConditionsByCodes(
        tenant: Tenant,
        patientFhirId: String,
        conditionCategoryCodes: List<FHIRSearchToken>,
        clinicalStatusCodes: List<FHIRSearchToken>
    ):
        List<Condition>
}
