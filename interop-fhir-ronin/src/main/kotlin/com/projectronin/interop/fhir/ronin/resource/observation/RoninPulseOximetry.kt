package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant

object RoninPulseOximetry : BaseRoninVitalSign(R4ObservationValidator, RoninProfile.OBSERVATION_PULSE_OXIMETRY.value) {
    internal val pulseOxCode = Code("59408-5")
    internal val O2SatCode = Code("2708-6")
    internal val flowRateCode = Code("3151-8")
    internal val concentrationCode = Code("3150-0")

    // Quantity unit codes - [US Core Pulse Oximetry](http://hl7.org/fhir/us/core/STU5.0.1/StructureDefinition-us-core-pulse-oximetry.html)
    private val validFlowRateCodes = listOf("L/min")
    private val validConcentrationCodes = listOf("%")

    // Reference checks - override BaseRoninObservation value lists as needed for RoninPulseOximetry
    override val validDerivedFromValues = listOf("DocumentReference", "ImagingStudy", "Media", "MolecularSequence", "Observation", "QuestionnaireResponse")
    override val validHasMemberValues = listOf("MolecularSequence", "Observation", "QuestionnaireResponse")
    override val validPartOfValues = listOf("ImagingStudy", "Immunization", "MedicationAdministration", "MedicationDispense", "MedicationStatement", "Procedure")

    override fun qualifies(resource: Observation): Boolean {
        val hasPulseOx = resource.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == pulseOxCode } ?: false
        val hasO2SatCode = resource.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == O2SatCode } ?: false
        return (hasPulseOx && hasO2SatCode)
    }

    private val noPulseOxCodeError = FHIRError(
        code = "USCORE_PXOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${pulseOxCode.value} required for US Core Pulse Oximetry profile",
        location = LocationContext(Observation::code)
    )
    private val noO2SatCodeError = FHIRError(
        code = "USCORE_PXOBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${O2SatCode.value} required for US Core Pulse Oximetry profile",
        location = LocationContext(Observation::code)
    )
    private val noFlowRateCodeError = FHIRError(
        code = "USCORE_PXOBS_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${flowRateCode.value} required for pulse oximetry flow rate",
        location = LocationContext(Observation::code)
    )
    private val noConcentrationCodeError = FHIRError(
        code = "USCORE_PXOBS_004",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${concentrationCode.value} required for pulse oximetry oxygen concentration",
        location = LocationContext(Observation::code)
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

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)

        validation.apply {
            val code = element.code
            if (code != null) {
                checkTrue(
                    code.coding.any { it.system == CodeSystem.LOINC.uri && it.code == pulseOxCode },
                    noPulseOxCodeError,
                    parentContext
                )
                checkTrue(
                    code.coding.any { it.system == CodeSystem.LOINC.uri && it.code == O2SatCode },
                    noO2SatCodeError,
                    parentContext
                )
            }
        }

        if (element.dataAbsentReason == null) {
            val componentCodeContext = LocationContext(Observation::component)
            val components = element.component
            val flowRate = components.filter { comp ->
                comp.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == flowRateCode } ?: false
            }
            val concentration = components.filter { comp ->
                comp.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == concentrationCode } ?: false
            }

            if (flowRate.size == 1) {
                validateVitalSignValue(flowRate.first().value, validFlowRateCodes, parentContext, validation)
            }
            if (concentration.size == 1) {
                validateVitalSignValue(concentration.first().value, validConcentrationCodes, parentContext, validation)
            }

            validation.apply {
                checkTrue(flowRate.isNotEmpty(), noFlowRateCodeError, componentCodeContext)
                checkTrue(flowRate.size <= 1, conflictingFlowRateCodeError, componentCodeContext)
                checkTrue(concentration.isNotEmpty(), noConcentrationCodeError, componentCodeContext)
                checkTrue(concentration.size <= 1, conflictingConcentrationCodeError, componentCodeContext)
            }
        }
    }

    /**
     * Cancel the base validation that would overly restrict Observation.code.codingn for this profile.
     */
    override fun validateObservation(element: Observation, parentContext: LocationContext, validation: Validation) {}

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
        )
        return Pair(transformed, validation)
    }
}
