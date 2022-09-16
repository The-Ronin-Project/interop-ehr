package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.resource.base.BaseValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.InvalidDynamicValueError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity

object USCoreVitalSignsValidator : BaseValidator<Observation>(R4ObservationValidator) {
    internal val vitalSignsCode = Code("vital-signs")

    private val acceptedEffectives = listOf(
        DynamicValueType.DATE_TIME,
        DynamicValueType.PERIOD
    )

    private val requiredVitalSignsCodeError = FHIRError(
        code = "USCORE_VSOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "A category code of \"${vitalSignsCode.value}\" is required",
        location = LocationContext(Observation::category)
    )

    private val requiredSubjectError = RequiredFieldError(Observation::subject)
    private val requiredPatientError = FHIRError(
        code = "USCORE_VSOBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Subject must represent a patient",
        location = LocationContext(Observation::subject)
    )

    private val requiredEffectiveError = RequiredFieldError(Observation::effective)
    private val invalidEffectiveError = InvalidDynamicValueError(Observation::effective, acceptedEffectives)

    override fun validate(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkTrue(
                element.category.flatMap { it.coding }
                    .any { it.system == CodeSystem.OBSERVATION_CATEGORY.uri && it.code == vitalSignsCode },
                requiredVitalSignsCodeError,
                parentContext
            )

            val subject = element.subject
            checkNotNull(subject, requiredSubjectError, parentContext)
            ifNotNull(subject) {
                checkTrue(subject.isForType("Patient"), requiredPatientError, parentContext)
            }

            val effective = element.effective
            checkNotNull(effective, requiredEffectiveError, parentContext)
            ifNotNull(effective) {
                checkTrue(acceptedEffectives.contains(effective.type), invalidEffectiveError, parentContext)
            }
        }
    }
}
