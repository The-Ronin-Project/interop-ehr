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
class RoninBloodPressure(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninVitalSign(R4ObservationValidator, RoninProfile.OBSERVATION_BLOOD_PRESSURE.value, normalizer, localizer) {
    companion object {
        internal val bloodPressureCode = Code("85354-9")
        internal val systolicCode = Code("8480-6")
        internal val diastolicCode = Code("8462-4")
    }

    // Quantity unit codes - [USCore Blood Pressure Units](http://hl7.org/fhir/us/core/STU5.0.1/StructureDefinition-us-core-blood-pressure.html)
    override val validQuantityCodes = listOf("mm[Hg]")

    // Reference checks - override BaseRoninObservation value lists as needed for RoninBloodPressure
    override val validBasedOnValues = listOf("CarePlan", "MedicationRequest")
    override val validPartOfValues = listOf("MedicationStatement", "Procedure")
    override val validDerivedFromValues = listOf("DocumentReference")
    override val validHasMemberValues = listOf("MolecularSequence", "Observation", "QuestionnaireResponse")

    override fun qualifies(resource: Observation): Boolean {
        return resource.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == bloodPressureCode } ?: false
    }

    private val noBloodPressureCodeError = FHIRError(
        code = "USCORE_BPOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${bloodPressureCode.value} required for US Core Blood Pressure profile",
        location = LocationContext(Observation::code)
    )
    private val noSystolicCodeError = FHIRError(
        code = "USCORE_BPOBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${systolicCode.value} required for systolic blood pressure",
        location = LocationContext(Observation::code)
    )
    private val noDiastolicCodeError = FHIRError(
        code = "USCORE_BPOBS_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${diastolicCode.value} required for diastolic blood pressure",
        location = LocationContext(Observation::code)
    )
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

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)

        validation.apply {
            val code = element.code
            if (code != null) {
                checkTrue(
                    code.coding.any { it.system == CodeSystem.LOINC.uri && it.code == bloodPressureCode },
                    noBloodPressureCodeError,
                    parentContext
                )
            }
        }

        if (element.dataAbsentReason == null) {
            val componentCodeContext = LocationContext(Observation::component)
            val components = element.component
            val systolic = components.filter { comp ->
                comp.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == systolicCode } ?: false
            }
            val diastolic = components.filter { comp ->
                comp.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == diastolicCode } ?: false
            }

            if (systolic.size == 1) {
                validateVitalSignValue(systolic.first().value, validQuantityCodes, parentContext, validation)
            }
            if (diastolic.size == 1) {
                validateVitalSignValue(diastolic.first().value, validQuantityCodes, parentContext, validation)
            }

            validation.apply {
                checkTrue(systolic.isNotEmpty(), noSystolicCodeError, componentCodeContext)
                checkTrue(systolic.size <= 1, conflictingSystolicCodeError, componentCodeContext)
                checkTrue(diastolic.isNotEmpty(), noDiastolicCodeError, componentCodeContext)
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
