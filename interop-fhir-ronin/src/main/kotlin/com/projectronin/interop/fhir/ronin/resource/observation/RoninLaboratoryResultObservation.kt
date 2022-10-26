package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant

object RoninLaboratoryResultObservation :
    USCoreBasedProfile<Observation>(R4ObservationValidator, RoninProfile.OBSERVATION_LABORATORY_RESULT.value) {
    private val laboratoryCode = Code("laboratory")

    override fun qualifies(resource: Observation): Boolean {
        return resource.category.any { category ->
            category.coding.any { it.system == CodeSystem.OBSERVATION_CATEGORY.uri && it.code == laboratoryCode }
        }
    }

    override fun validateRonin(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)

            // TODO: RoninExtension.TENANT_SOURCE_OBSERVATION_CODE, check Ronin IG and consider requireCodeableConcept()
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
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Observation?, Validation> {
        // TODO: RoninExtension.TENANT_SOURCE_OBSERVATION_CODE, filterCodeableConcept(), extensionCodeableConcept()

        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )
        return Pair(transformed, validation)
    }
}
