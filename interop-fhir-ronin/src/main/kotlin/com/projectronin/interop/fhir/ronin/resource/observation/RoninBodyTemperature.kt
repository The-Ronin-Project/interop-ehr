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
class RoninBodyTemperature(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninVitalSign(R4ObservationValidator, RoninProfile.OBSERVATION_BODY_TEMPERATURE.value, normalizer, localizer) {
    companion object {
        internal val bodyTemperatureCode = Code("8310-5")
    }

    // Quantity unit codes - [USCore Body Temperature Units](http://hl7.org/fhir/R4/valueset-ucum-bodytemp.html)
    override val validQuantityCodes = listOf("Cel", "[degF]")

    // Reference checks - override BaseRoninObservation value lists as needed for RoninBodyTemperature
    override val validDerivedFromValues = listOf(
        "DocumentReference",
        "ImagingStudy",
        "Media",
        "MolecularSequence",
        "Observation",
        "QuestionnaireResponse"
    )
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
        return resource.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == bodyTemperatureCode }
            ?: false
    }

    private val noBodyTemperatureCodeError = FHIRError(
        code = "USCORE_TMPOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${bodyTemperatureCode.value} required for US Core Body Temperature profile",
        location = LocationContext(Observation::code)
    )

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)
        validation.apply {
            val code = element.code
            if (code != null) {
                checkTrue(
                    code.coding.any { it.system == CodeSystem.LOINC.uri && it.code == bodyTemperatureCode },
                    noBodyTemperatureCodeError,
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
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )
        return Pair(transformed, validation)
    }
}
