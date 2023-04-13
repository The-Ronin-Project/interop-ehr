package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

@Component
class RoninRespiratoryRate(
    normalizer: Normalizer,
    localizer: Localizer,
    private val registryClient: NormalizationRegistryClient
) :
    BaseRoninVitalSign(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_RESPIRATORY_RATE.value,
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

    // Quantity unit codes - from [USCore Vitals Common](http://hl7.org/fhir/R4/valueset-ucum-vitals-common.html)
    override val validQuantityCodes = listOf("/min")

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
