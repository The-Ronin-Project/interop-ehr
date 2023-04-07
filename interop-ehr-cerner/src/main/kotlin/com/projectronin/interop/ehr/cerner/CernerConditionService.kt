package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.ConditionService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.ehr.util.toOrParams
import com.projectronin.interop.ehr.util.toSearchTokens
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.valueset.ConditionClinicalStatusCodes
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CernerConditionService(cernerClient: CernerClient) : ConditionService,
    CernerFHIRService<Condition>(cernerClient) {
    override val fhirURLSearchPart = "/Condition"
    override val fhirResourceType = Condition::class.java
    val clinicalSystem = CodeSystem.CONDITION_CLINICAL.uri.value
    val searchableCodes = listOf(
        ConditionClinicalStatusCodes.ACTIVE,
        ConditionClinicalStatusCodes.INACTIVE,
        ConditionClinicalStatusCodes.RESOLVED
    ).map {
        FHIRSearchToken(clinicalSystem, it.code)
    }

    @Trace
    override fun findConditions(
        tenant: Tenant,
        patientFhirId: String,
        conditionCategoryCode: String,
        clinicalStatus: String
    ): List<Condition> {
        return findConditionsByCodes(
            tenant,
            patientFhirId,
            listOf(conditionCategoryCode).toSearchTokens(),
            listOf(clinicalStatus).toSearchTokens()
        )
    }

    @Trace
    override fun findConditionsByCodes(
        tenant: Tenant,
        patientFhirId: String,
        conditionCategoryCodes: List<FHIRSearchToken>,
        clinicalStatusCodes: List<FHIRSearchToken>
    ): List<Condition> {
        val searchCodes = clinicalStatusCodes.ifEmpty { searchableCodes }
        val clinicalStatus = searchCodes.joinToString(",") { it.code }

        val parameters = mapOf(
            "patient" to patientFhirId,
            "category" to conditionCategoryCodes.toOrParams(),
            "clinical-status" to clinicalStatus
        )
        return getResourceListFromSearch(tenant, parameters)
    }
}
