package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.ConditionService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.FHIRSearchToken
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.ehr.util.toOrParams
import com.projectronin.interop.ehr.util.toSearchTokens
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Service providing access to conditions within Epic.
 *
 * When entering category or clinicalStatus codes,
 * the Observation.category.coding.code or Observation.clinicalStatus.coding.code
 * may be entered by itself, or the caller may use FHIR token format,
 * to provide both the system and the code, as in:
 * ```
 * system|code
 * ```
 * The FHIR preferred choices for category code values are problem-list-item, encounter-diagnosis, and others
 * from the system [http://terminology.hl7.org/CodeSystem/condition-category](http://terminology.hl7.org/CodeSystem/condition-category.html)
 * Codes from this system can be input as just the code, such as:
 * ```
 * problem-list-item
 * ```
 * but for accuracy, each category code should be input along with its system, as a token:
 * ```
 * http://terminology.hl7.org/Codesystem/condition-category|problem-list-item
 * ```
 * You may mix tokens and codes in a comma-separated list, but a token is more clear.
 *
 * The FHIR preferred choices for clinicalStatus code values are active, resolved, and others
 * from the system [http://hl7.org/fhir/ValueSet/condition-clinical](http://hl7.org/fhir/ValueSet/condition-clinical.html)
 */
@Component
class EpicConditionService(epicClient: EpicClient) : ConditionService, EpicPagingService(epicClient) {
    private val conditionSearchUrlPart = "/api/FHIR/R4/Condition"

    override fun findConditions(
        tenant: Tenant,
        patientFhirId: String,
        conditionCategoryCode: String,
        clinicalStatus: String,
    ): List<Condition> {

        return findConditionsByCodes(
            tenant,
            patientFhirId,
            listOf(conditionCategoryCode).toSearchTokens(),
            listOf(clinicalStatus).toSearchTokens()
        )
    }

    override fun findConditionsByCodes(
        tenant: Tenant,
        patientFhirId: String,
        conditionCategoryCodes: List<FHIRSearchToken>,
        clinicalStatusCodes: List<FHIRSearchToken>,
    ): List<Condition> {
        val parameters = mapOf(
            "patient" to patientFhirId,
            "category" to conditionCategoryCodes.toOrParams(),
            "clinical-status" to clinicalStatusCodes.toOrParams(),
        )

        return getBundleWithPaging(tenant, conditionSearchUrlPart, parameters).toListOfType()
    }
}
