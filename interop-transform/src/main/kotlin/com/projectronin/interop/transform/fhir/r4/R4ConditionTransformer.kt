package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Condition
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.transform.ConditionTransformer
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyCondition
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.transform.fhir.r4.util.localize
import com.projectronin.interop.transform.util.toFhirIdentifier
import mu.KotlinLogging
import org.springframework.stereotype.Component
import com.projectronin.interop.fhir.r4.resource.Condition as R4Condition

/**
 * Implementation of [ConditionTransformer] suitable for all R4 FHIR Appointments
 */

@Component
class R4ConditionTransformer : ConditionTransformer {
    private val logger = KotlinLogging.logger { }

    override fun transformConditions(
        bundle: Bundle<Condition>,
        tenant: Tenant
    ): List<OncologyCondition> {
        require(bundle.dataSource == DataSource.FHIR_R4) { "Bundle is not an R4 FHIR resource" }

        return bundle.transformResources(tenant, this::transformCondition)
    }
    override fun transformCondition(condition: Condition, tenant: Tenant): OncologyCondition? {
        require(condition.dataSource == DataSource.FHIR_R4) { "Condition is not an R4 FHIR resource" }
        val r4Condition = condition.resource as R4Condition
        val id = r4Condition.id
        if (id == null) {
            logger.warn { "Unable to transform condition due to missing ID" }
            return null
        }

        if (r4Condition.category.isEmpty()) {
            logger.warn { "Unable to transform condition $id due to missing category" }
            return null
        }

        if (r4Condition.code == null) {
            logger.warn { "Unable to transform condition $id due to missing code" }
            return null
        }

        try {
            return OncologyCondition(
                id = id.localize(tenant),
                meta = r4Condition.meta?.localize(tenant),
                implicitRules = r4Condition.implicitRules,
                language = r4Condition.language,
                text = r4Condition.text?.localize(tenant),
                contained = r4Condition.contained,
                extension = r4Condition.extension.map { it.localize(tenant) },
                modifierExtension = r4Condition.modifierExtension.map { it.localize(tenant) },
                identifier = r4Condition.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
                clinicalStatus = r4Condition.clinicalStatus?.localize(tenant),
                verificationStatus = r4Condition.verificationStatus?.localize(tenant),
                category = r4Condition.category.map { it.localize(tenant) },
                severity = r4Condition.severity?.localize(tenant),
                code = r4Condition.code!!.localize(tenant),
                bodySite = r4Condition.bodySite.map { it.localize(tenant) },
                subject = r4Condition.subject.localize(tenant),
                encounter = r4Condition.encounter?.localize(tenant),
                onset = r4Condition.onset,
                abatement = r4Condition.abatement,
                recordedDate = r4Condition.recordedDate,
                recorder = r4Condition.recorder?.localize(tenant),
                asserter = r4Condition.asserter?.localize(tenant),
                stage = r4Condition.stage.map { it.localize(tenant) },
                evidence = r4Condition.evidence.map { it.localize(tenant) },
                note = r4Condition.note.map { it.localize(tenant) }
            )
        } catch (e: Exception) {
            logger.warn(e) { "Unable to transform condition: ${e.message}" }
            return null
        }
    }
}
