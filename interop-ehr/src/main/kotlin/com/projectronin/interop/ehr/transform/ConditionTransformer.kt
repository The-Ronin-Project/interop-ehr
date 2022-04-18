package com.projectronin.interop.ehr.transform

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Condition
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyCondition
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines a Transformer capable of converting EHR [Condition]s into [OncologyCondition]s.
 */
interface ConditionTransformer {
    /**
     * Transforms the [Condition] into an [OncologyCondition] based on the [tenant]. If the transformation
     * can not be completed due to missing or incomplete information, null will be returned.
     */
    fun transformCondition(condition: Condition, tenant: Tenant): OncologyCondition?

    /**
     * Transforms the [bundle] into a List of [OncologyCondition]s based on the [tenant]. Only [Condition]s that
     * could be transformed successfully will be included in the response.
     */
    fun transformConditions(bundle: Bundle<Condition>, tenant: Tenant): List<OncologyCondition>
}
