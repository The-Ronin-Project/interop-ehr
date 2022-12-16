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

object RoninHeartRate : BaseRoninVitalSign(R4ObservationValidator, RoninProfile.OBSERVATION_HEART_RATE.value) {
    internal val heartRateCode = Code("8867-4")

    // Quantity unit codes - from [USCore Vitals Common](http://hl7.org/fhir/R4/valueset-ucum-vitals-common.html)
    override val validQuantityCodes = listOf("/min")

    // Reference checks - override BaseRoninObservation value lists as needed for RoninBodyTemperature
    override val validDerivedFromValues = listOf("DocumentReference", "ImagingStudy", "Media", "MolecularSequence", "Observation", "QuestionnaireResponse")
    override val validHasMemberValues = listOf("MolecularSequence", "Observation", "QuestionnaireResponse")
    override val validPartOfValues = listOf("ImagingStudy", "Immunization", "MedicationAdministration", "MedicationDispense", "MedicationStatement", "Procedure")

    override fun qualifies(resource: Observation): Boolean {
        return resource.code?.coding?.any { it.system == CodeSystem.LOINC.uri && it.code == heartRateCode } ?: false
    }

    private val noHeartRateCodeError = FHIRError(
        code = "USCORE_HROBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "LOINC code ${heartRateCode.value} required for US Core Heart Rate profile",
        location = LocationContext(Observation::code)
    )

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)
        validation.apply {
            val code = element.code
            if (code != null) {
                checkTrue(
                    code.coding.any { it.system == CodeSystem.LOINC.uri && it.code == heartRateCode },
                    noHeartRateCodeError,
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
        )
        return Pair(transformed, validation)
    }
}