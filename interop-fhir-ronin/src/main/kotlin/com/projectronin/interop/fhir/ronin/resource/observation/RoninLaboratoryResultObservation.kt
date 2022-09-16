package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant

const val RONIN_LAB_RESULT_PROFILE =
    "http://projectronin.io/fhir/ronin.common-fhir-model.uscore-r4/StructureDefinition/ronin-laboratoryresultobservation"

object RoninLaboratoryResultObservation :
    USCoreBasedProfile<Observation>(R4ObservationValidator, RONIN_LAB_RESULT_PROFILE) {
    private val laboratoryCode = Code("laboratory")

    override fun qualifies(resource: Observation): Boolean {
        return resource.category.any { category ->
            category.coding.any { it.system == CodeSystem.OBSERVATION_CATEGORY.uri && it.code == laboratoryCode }
        }
    }

    override fun validateRonin(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)
        }
    }

    private val requiredLaboratoryCodeError = FHIRError(
        code = "USCORE_LABOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "A category code of \"${laboratoryCode.value}\" is required",
        location = LocationContext(Observation::category)
    )

    private val requiredSubjectError = RequiredFieldError(Observation::subject)
    private val requiredPatientError = FHIRError(
        code = "USCORE_LABOBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Subject must represent a patient",
        location = LocationContext(Observation::subject)
    )

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkTrue(
                element.category.flatMap { it.coding }
                    .any { it.system == CodeSystem.OBSERVATION_CATEGORY.uri && it.code == laboratoryCode },
                requiredLaboratoryCodeError,
                parentContext
            )

            val subject = element.subject
            checkNotNull(subject, requiredSubjectError, parentContext)
            ifNotNull(subject) {
                checkTrue(subject.isForType("Patient"), requiredPatientError, parentContext)
            }
        }
    }

    private val requiredIdError = RequiredFieldError(Observation::id)

    override fun transformInternal(
        original: Observation,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Observation?, Validation> {
        val validation = validation {
            checkNotNull(original.id, requiredIdError, parentContext)
        }

        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta.transform(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + original.getFhirIdentifiers() + tenant.toFhirIdentifier(),
            basedOn = original.basedOn.map { it.localize(tenant) },
            partOf = original.partOf.map { it.localize(tenant) },
            category = original.category.map { it.localize(tenant) },
            code = original.code?.localize(tenant),
            subject = original.subject?.localize(tenant),
            focus = original.focus.map { it.localize(tenant) },
            encounter = original.encounter?.localize(tenant),
            performer = original.performer.map { it.localize(tenant) },
            dataAbsentReason = original.dataAbsentReason?.localize(tenant),
            interpretation = original.interpretation.map { it.localize(tenant) },
            bodySite = original.bodySite?.localize(tenant),
            method = original.method?.localize(tenant),
            specimen = original.specimen?.localize(tenant),
            device = original.device?.localize(tenant),
            referenceRange = original.referenceRange.map { it.localize(tenant) },
            hasMember = original.hasMember.map { it.localize(tenant) },
            derivedFrom = original.derivedFrom.map { it.localize(tenant) },
            component = original.component.map { it.localize(tenant) },
            note = original.note.map { it.localize(tenant) }
        )
        return Pair(transformed, validation)
    }
}
