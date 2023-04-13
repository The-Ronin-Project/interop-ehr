package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.isInValueSet
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
class RoninPulseOximetry(
    normalizer: Normalizer,
    localizer: Localizer,
    private val registryClient: NormalizationRegistryClient
) :
    BaseRoninVitalSign(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_PULSE_OXIMETRY.value,
        normalizer,
        localizer
    ) {

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    override val qualifyingCodes: List<Coding> by lazy {
        registryClient.getRequiredValueSet(
            "Observation.code",
            profile
        )
    }

    // Multipart qualifying codes for RoninPulseOximetry
    private val qualifyingFlowRateCodes = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("3151-8")))
    private val qualifyingConcentrationCodes = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("3150-0")))

    // Quantity unit codes - [US Core Pulse Oximetry](http://hl7.org/fhir/us/core/STU5.0.1/StructureDefinition-us-core-pulse-oximetry.html)
    private val validFlowRateCodes = listOf("L/min")
    private val validConcentrationCodes = listOf("%")

    // Reference checks - override BaseRoninObservation value lists as needed for RoninPulseOximetry
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

    private val conflictingFlowRateCodeError = FHIRError(
        code = "USCORE_PXOBS_005",
        severity = ValidationIssueSeverity.ERROR,
        description = "Only 1 entry is allowed for pulse oximetry flow rate",
        location = LocationContext(Observation::code)
    )
    private val conflictingConcentrationCodeError = FHIRError(
        code = "USCORE_PXOBS_006",
        severity = ValidationIssueSeverity.ERROR,
        description = "Only 1 entry is allowed for pulse oximetry oxygen concentration",
        location = LocationContext(Observation::code)
    )

    override fun validateVitalSign(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateVitalSign(element, parentContext, validation)

        if (element.dataAbsentReason == null) {
            val componentCodeContext = LocationContext(Observation::component)
            val components = element.component
            val flowRate = components.filter { comp ->
                comp.code?.coding?.any { it.isInValueSet(qualifyingFlowRateCodes) } ?: false
            }
            val concentration = components.filter { comp ->
                comp.code?.coding?.any { it.isInValueSet(qualifyingConcentrationCodes) } ?: false
            }

            if (flowRate.size == 1) {
                validateVitalSignValue(flowRate.first().value, validFlowRateCodes, parentContext, validation)
            }
            if (concentration.size == 1) {
                validateVitalSignValue(concentration.first().value, validConcentrationCodes, parentContext, validation)
            }

            validation.apply {
                checkTrue(
                    flowRate.isNotEmpty(),
                    FHIRError(
                        code = "RONIN_PXOBS_004",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                        qualifyingFlowRateCodes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = componentCodeContext
                    ),
                    componentCodeContext
                )
                checkTrue(flowRate.size <= 1, conflictingFlowRateCodeError, componentCodeContext)
                checkTrue(
                    concentration.isNotEmpty(),
                    FHIRError(
                        code = "RONIN_PXOBS_005",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                        qualifyingConcentrationCodes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = componentCodeContext
                    ),
                    componentCodeContext
                )
                checkTrue(concentration.size <= 1, conflictingConcentrationCodeError, componentCodeContext)
            }
        }
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
