package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

@Component
class RoninBodyWeight(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninVitalSign(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_BODY_WEIGHT.value,
        normalizer,
        localizer
    ) {
    companion object {
        internal val bodyWeightCode = Code("29463-7")
    }

    // Quantity unit codes - [USCore Body Weight Units](http://hl7.org/fhir/R4/valueset-ucum-bodyweight.html)
    override val validQuantityCodes = listOf("kg", "[lb_av]", "g")

    // Reference checks - override BaseRoninObservation value lists as needed for RoninBodyWeight
    override val validBasedOnValues = listOf("CarePlan", "MedicationRequest")
    override val validDerivedFromValues = listOf("DocumentReference")
    override val validHasMemberValues = listOf("MolecularSequence", "Observation", "QuestionnaireResponse")
    override val validPartOfValues = listOf(
        "ImagingStudy",
        "Immunization",
        "MedicationAdministration",
        "MedicationDispense",
        "MedicationStatement",
        "Procedure"
    )

    override fun qualifies(resource: Observation): Boolean {
        return resource.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == bodyWeightCode } ?: false
    }

    private val noBodySiteError = FHIRError(
        code = "RONIN_WTOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "bodySite not allowed for Body Weight observation",
        location = LocationContext(Observation::bodySite)
    )

    override fun validateObservation(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateObservation(element, parentContext, validation)
        validation.apply {
            checkTrue(element.bodySite == null, noBodySiteError, parentContext)
        }
    }

    private val noBodyWeightCodeError = FHIRError(
        code = "USCORE_WTOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${bodyWeightCode.value} required for US Core Body Weight profile",
        location = LocationContext(Observation::code)
    )

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)
        validation.apply {
            val code = element.code
            if (code != null) {
                checkTrue(
                    code.coding.any { it.system == CodeSystem.LOINC.uri && it.code == bodyWeightCode },
                    noBodyWeightCodeError,
                    parentContext
                )
            }
        }
        validateVitalSignValue(element.value, validQuantityCodes, parentContext, validation)
    }

    private val requiredIdError = RequiredFieldError(Observation::id)

    override fun transformInternal(
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Observation?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier(),
            bodySite = null // bodySite should not be supplied for Body Weight
        )
        return Pair(transformed, validation)
    }
}
