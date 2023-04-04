package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
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
class RoninBloodPressure(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninVitalSign(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_BLOOD_PRESSURE.value,
        normalizer,
        localizer
    ) {

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    override val qualifyingCodes = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("85354-9")))

    // Multipart qualifying codes for RoninBloodPressure
    internal val qualifyingSystolicCodes = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8480-6")))
    internal val qualifyingDiastolicCodes = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8462-4")))

    // Quantity unit codes - [USCore Blood Pressure Units](http://hl7.org/fhir/us/core/STU5.0.1/StructureDefinition-us-core-blood-pressure.html)
    override val validQuantityCodes = listOf("mm[Hg]")

    // Reference checks - override BaseRoninObservation value lists as needed for RoninBloodPressure
    override val validBasedOnValues = listOf("CarePlan", "MedicationRequest")
    override val validPartOfValues = listOf("MedicationStatement", "Procedure")
    override val validDerivedFromValues = listOf("DocumentReference")
    override val validHasMemberValues = listOf("MolecularSequence", "Observation", "QuestionnaireResponse")

    private val conflictingSystolicCodeError = FHIRError(
        code = "USCORE_BPOBS_004",
        severity = ValidationIssueSeverity.ERROR,
        description = "Only 1 entry is allowed for systolic blood pressure",
        location = LocationContext(Observation::code)
    )
    private val conflictingDiastolicCodeError = FHIRError(
        code = "USCORE_BPOBS_005",
        severity = ValidationIssueSeverity.ERROR,
        description = "Only 1 entry is allowed for diastolic blood pressure",
        location = LocationContext(Observation::code)
    )

    override fun validateVitalSign(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateVitalSign(element, parentContext, validation)

        if (element.dataAbsentReason == null) {
            val componentCodeContext = LocationContext(Observation::component)
            val components = element.component
            val systolic = components.filter { comp ->
                comp.code?.coding?.any { it.isInValueSet(qualifyingSystolicCodes) } ?: false
            }
            val diastolic = components.filter { comp ->
                comp.code?.coding?.any { it.isInValueSet(qualifyingDiastolicCodes) } ?: false
            }

            if (systolic.size == 1) {
                validateVitalSignValue(systolic.first().value, validQuantityCodes, parentContext, validation)
            }
            if (diastolic.size == 1) {
                validateVitalSignValue(diastolic.first().value, validQuantityCodes, parentContext, validation)
            }

            validation.apply {
                checkTrue(
                    systolic.isNotEmpty(),
                    FHIRError(
                        code = "USCORE_BPOBS_001",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                        qualifyingSystolicCodes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = componentCodeContext
                    ),
                    componentCodeContext
                )
                checkTrue(systolic.size <= 1, conflictingSystolicCodeError, componentCodeContext)
                checkTrue(
                    diastolic.isNotEmpty(),
                    FHIRError(
                        code = "USCORE_BPOBS_002",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                        qualifyingDiastolicCodes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = componentCodeContext
                    ),
                    componentCodeContext
                )
                checkTrue(diastolic.size <= 1, conflictingDiastolicCodeError, componentCodeContext)
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
