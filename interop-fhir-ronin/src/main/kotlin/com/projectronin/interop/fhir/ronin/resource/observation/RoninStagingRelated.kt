package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
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
class RoninStagingRelated(
    normalizer: Normalizer,
    localizer: Localizer,
    private val registryClient: NormalizationRegistryClient
) :
    BaseRoninObservation(
        R4ObservationValidator,
        RoninProfile.OBSERVATION_STAGING_RELATED.value,
        normalizer,
        localizer
    ) {

    override val qualifyingCodes: List<Coding> by lazy {
        registryClient.getRequiredValueSet(
            "Observation.code",
            RoninProfile.OBSERVATION_STAGING_RELATED.value
        )
    }
    override val validDerivedFromValues = listOf(
        "DocumentReference",
        "ImagingStudy",
        "Media",
        "MolecularSequence",
        "Observation",
        "QuestionnaireResponse"
    )

    override val validPartOfValues = listOf(
        "ImagingStudy",
        "Immunization",
        "MedicationAdministration",
        "MedicationDispense",
        "MedicationStatement",
        "Procedure"
    )

    override val validPerformerValues =
        listOf("Patient", "Practitioner", "PractitionerRole", "RelatedPerson", "Organization", "CareTeam")

    private val requiredIdError = RequiredFieldError(Observation::id)
    private val requiredCategoryError = RequiredFieldError(Observation::category)
    private val singleObservationCodeError = FHIRError(
        code = "RONIN_STAGING_OBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Coding list must contain exactly 1 entry",
        location = LocationContext(Observation::code)
    )

    private val requiredObservationCategoryCoding = FHIRError(
        code = "RONIN_STAGING_OBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Coding is required",
        location = LocationContext(Observation::category)
    )

    // category and category.coding must be present but are not fixed values
    // code.coding must exist in the valueSet
    override fun qualifies(resource: Observation): Boolean {
        return (
            (resource.code?.coding?.any { it.isInValueSet(qualifyingCodes) } ?: false) &&
                resource.category.isNotEmpty() && resource.category.any { category ->
                category.coding.isNotEmpty()
            }
            )
    }

    override fun validateRonin(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateRonin(element, parentContext, validation)
        validation.apply {
            checkTrue(
                element.category.any { category ->
                    category.coding.isNotEmpty()
                },
                requiredObservationCategoryCoding,
                parentContext
            )
        }
    }

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateUSCore(element, parentContext, validation)
        validation.apply {
            checkNotNull(element.category, requiredCategoryError, parentContext)
            checkTrue(element.category.isNotEmpty(), requiredCategoryError, parentContext)
        }
    }

    override fun validateObservation(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validateObservation(element, parentContext, validation)
        validation.apply {
            element.code?.coding?.let {
                checkTrue(it.size == 1, singleObservationCodeError, parentContext)
            }
        }
    }

    override fun validate(element: Observation, parentContext: LocationContext, validation: Validation) {
        super.validate(element, parentContext, validation) // Ronin, USCore
        validateObservation(element, parentContext, validation)
    }

    override fun transformInternal(
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Observation?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val tenantSourceCodeExtension = getExtensionOrEmptyList(
            RoninExtension.TENANT_SOURCE_OBSERVATION_CODE,
            normalized.code
        )

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            extension = normalized.extension + tenantSourceCodeExtension,
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )
        return Pair(transformed, validation)
    }
}
