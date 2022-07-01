package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.transform.ObservationTransformer
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyObservation
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.transform.fhir.r4.util.localize
import com.projectronin.interop.transform.util.toFhirIdentifier
import mu.KotlinLogging
import com.projectronin.interop.ehr.model.Observation as EHRObservation
import com.projectronin.interop.fhir.r4.resource.Observation as R4Observation

/**
 * Implementation of [ObservationTransformer] suitable for all R4 FHIR Observations
 */
class R4ObservationTransformer : ObservationTransformer {
    private val logger = KotlinLogging.logger { }

    override fun transformObservations(bundle: Bundle<EHRObservation>, tenant: Tenant): List<OncologyObservation> {
        require(bundle.dataSource == DataSource.FHIR_R4) { "Bundle is not an R4 FHIR resource" }
        return bundle.transformResources(tenant, this::transformObservation)
    }

    override fun transformObservation(observation: EHRObservation, tenant: Tenant): OncologyObservation? {
        require(observation.dataSource == DataSource.FHIR_R4) { "Observation is not an R4 FHIR resource" }

        val r4Observation = observation.resource as R4Observation

        val id = r4Observation.id
        if (id == null) {
            logger.warn { "Unable to transform Observation due to missing ID" }
            return null
        }

        return OncologyObservation(
            id = id.localize(tenant),
            meta = r4Observation.meta?.localize(tenant),
            implicitRules = r4Observation.implicitRules,
            language = r4Observation.language,
            text = r4Observation.text?.localize(tenant),
            contained = r4Observation.contained,
            extension = r4Observation.extension.map { it.localize(tenant) },
            modifierExtension = r4Observation.modifierExtension.map { it.localize(tenant) },
            identifier = r4Observation.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
            basedOn = r4Observation.basedOn.map { it.localize(tenant) },
            partOf = r4Observation.partOf.map { it.localize(tenant) },
            status = r4Observation.status,
            category = r4Observation.category.map { it.localize(tenant) },
            code = r4Observation.code.localize(tenant),
            subject = r4Observation.subject?.localize(tenant),
            focus = r4Observation.focus.map { it.localize(tenant) },
            encounter = r4Observation.encounter?.localize(tenant),
            effective = r4Observation.effective,
            issued = r4Observation.issued,
            performer = r4Observation.performer.map { it.localize(tenant) },
            value = r4Observation.value,
            dataAbsentReason = r4Observation.dataAbsentReason?.localize(tenant),
            interpretation = r4Observation.interpretation.map { it.localize(tenant) },
            bodySite = r4Observation.bodySite?.localize(tenant),
            method = r4Observation.method?.localize(tenant),
            specimen = r4Observation.specimen?.localize(tenant),
            device = r4Observation.device?.localize(tenant),
            referenceRange = r4Observation.referenceRange.map { it.localize(tenant) },
            hasMember = r4Observation.hasMember.map { it.localize(tenant) },
            derivedFrom = r4Observation.derivedFrom.map { it.localize(tenant) },
            component = r4Observation.component.map { it.localize(tenant) },
            note = r4Observation.note.map { it.localize(tenant) }
        )
    }
}
