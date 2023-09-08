package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.normalization.ValueSetList
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.isInValueSet
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RoninPulseOximetry(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient
) :
    BaseRoninVitalSign(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_PULSE_OXIMETRY.value,
        normalizer,
        localizer,
        registryClient
    ) {
    override val rcdmVersion = RCDMVersion.V3_26_1
    override val profileVersion = 3

    // Multipart qualifying codes for RoninPulseOximetry
    fun qualifyingFlowRateCodes(): ValueSetList = registryClient.getRequiredValueSet(
        "Observation.component:FlowRate.code",
        profile
    )

    fun qualifyingConcentrationCodes(): ValueSetList = registryClient.getRequiredValueSet(
        "Observation.component:Concentration.code",
        profile
    )

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

    override val validQuantityCodes = listOf("%")

    override fun validateVitalSign(element: Observation, parentContext: LocationContext, validation: Validation) {
        validateVitalSignValue(element.value, parentContext, validation)

        if (element.dataAbsentReason == null) {
            val components = element.component
            val flowRate = components.filter { comp ->
                comp.code?.coding?.any { it.isInValueSet(qualifyingFlowRateCodes().codes) } ?: false
            }
            val concentration = components.filter { comp ->
                comp.code?.coding?.any { it.isInValueSet(qualifyingConcentrationCodes().codes) } ?: false
            }

            if (flowRate.size == 1) {
                validateVitalSignValue(
                    flowRate.first().value,
                    LocationContext("Observation", "component:FlowRate"),
                    validation,
                    validFlowRateCodes
                )
            }
            if (concentration.size == 1) {
                validateVitalSignValue(
                    concentration.first().value,
                    LocationContext("Observation", "component:Concentration"),
                    validation,
                    validConcentrationCodes
                )
            }

            validation.apply {
                val flowRateCodeContext = LocationContext("Observation", "component:FlowRate.code")
                checkTrue(
                    flowRate.isNotEmpty(),
                    FHIRError(
                        code = "RONIN_PXOBS_004",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                        qualifyingFlowRateCodes().codes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = flowRateCodeContext,
                        metadata = listOf(qualifyingFlowRateCodes().metadata!!)
                    ),
                    parentContext
                )
                checkTrue(
                    flowRate.size <= 1,
                    FHIRError(
                        code = "USCORE_PXOBS_005",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Only 1 entry is allowed for pulse oximetry flow rate",
                        location = flowRateCodeContext,
                        metadata = listOf(qualifyingFlowRateCodes().metadata!!)
                    ),
                    parentContext
                )

                val concentrationCodeContext = LocationContext("Observation", "component:Concentration.code")
                checkTrue(
                    concentration.isNotEmpty(),
                    FHIRError(
                        code = "RONIN_PXOBS_005",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                        qualifyingConcentrationCodes().codes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = concentrationCodeContext,
                        metadata = listOf(qualifyingConcentrationCodes().metadata!!)
                    ),
                    parentContext
                )
                checkTrue(
                    concentration.size <= 1,
                    FHIRError(
                        code = "USCORE_PXOBS_006",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Only 1 entry is allowed for pulse oximetry oxygen concentration",
                        location = concentrationCodeContext,
                        metadata = listOf(qualifyingConcentrationCodes().metadata!!)
                    ),
                    parentContext
                )
            }
        }
    }

    private val requiredIdError = RequiredFieldError(Observation::id)

    override fun transformInternal(
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<Observation?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )
        return Pair(transformed, validation)
    }
}
