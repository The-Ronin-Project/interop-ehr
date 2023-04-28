package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.qualifiesForValueSet
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.ronin.util.validateReferenceList
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Base class capable of handling common tasks associated to Ronin Observation profiles.
 */
abstract class BaseRoninObservation(
    extendedProfile: ProfileValidator<Observation>,
    profile: String,
    normalizer: Normalizer,
    localizer: Localizer
) : USCoreBasedProfile<Observation>(extendedProfile, profile, normalizer, localizer) {

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    open val qualifyingCategories: List<Coding> = emptyList()

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    open val qualifyingCodes: List<Coding> = emptyList()

    override fun qualifies(resource: Observation): Boolean {
        return (
            resource.category.qualifiesForValueSet(qualifyingCategories) &&
                resource.code.qualifiesForValueSet(qualifyingCodes)
            )
    }

    // Reference checks - subclasses may override lists to modify validation logic for reference attributes
    open val validBasedOnValues = listOf(
        "CarePlan",
        "DeviceRequest",
        "ImmunizationRecommendation",
        "MedicationRequest",
        "NutritionOrder",
        "ServiceRequest"
    )
    open val validDerivedFromValues = listOf("DocumentReference", "Observation")
    open val validDeviceValues = listOf("Device", "DeviceMetric")
    open val validEncounterValues = listOf("Encounter")
    open val validHasMemberValues = listOf("Observation")
    open val validNoteAuthorValues = listOf("Organization", "Patient", "Practitioner")
    open val validSpecimenValues = listOf("Specimen")
    open val validSubjectValues = listOf("Patient", "Location")
    open val validPartOfValues = listOf("Immunization", "MedicationStatement", "Procedure")
    open val validPerformerValues = listOf("CareTeam", "Organization", "Patient", "Practitioner", "PractitionerRole")

    // Dynamic value checks - subclasses may override lists
    open val acceptedEffectiveTypes = listOf(
        DynamicValueType.DATE_TIME,
        DynamicValueType.PERIOD,
        DynamicValueType.TIMING,
        DynamicValueType.INSTANT
    )

    // Dynamic value checks - same for all subclasses
    private val acceptedAuthorTypes = listOf(
        DynamicValueType.STRING,
        DynamicValueType.REFERENCE
    )

    private val requiredSubjectError = RequiredFieldError(Observation::subject)

    private val requiredCodeError = RequiredFieldError(Observation::code)

    private val singleObservationCodeError = FHIRError(
        code = "RONIN_OBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Coding list is restricted to 1 entry",
        location = LocationContext(Observation::code)
    )

    /**
     * Validates the [element] against RoninObservation rules. Validation logic for reference attributes may vary by
     * Observation type. This logic is controlled by overriding the open val variables like [validSubjectValues].
     */
    override fun validateRonin(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, validation)

            requireCodeableConcept("code", element.code, parentContext, validation)
            requireCodeCoding("code", element.code?.coding, parentContext, validation)

            checkNotNull(element.subject, requiredSubjectError, parentContext)
            validateReference(element.subject, validSubjectValues, LocationContext(Observation::subject), validation)

            validateReferenceList(
                element.basedOn,
                validBasedOnValues,
                LocationContext(Observation::basedOn),
                validation
            )
            validateReferenceList(
                element.derivedFrom,
                validDerivedFromValues,
                LocationContext(Observation::derivedFrom),
                validation
            )
            validateReference(element.device, validDeviceValues, LocationContext(Observation::device), validation)
            validateReference(
                element.encounter,
                validEncounterValues,
                LocationContext(Observation::encounter),
                validation
            )
            validateReferenceList(
                element.hasMember,
                validHasMemberValues,
                LocationContext(Observation::hasMember),
                validation
            )
            validateReferenceList(element.partOf, validPartOfValues, LocationContext(Observation::partOf), validation)
            validateReferenceList(
                element.performer,
                validPerformerValues,
                LocationContext(Observation::performer),
                validation
            )
            validateReference(element.specimen, validSpecimenValues, LocationContext(Observation::specimen), validation)

            element.note.forEachIndexed { index, note ->
                note.author?.let { author ->
                    if (author.type == DynamicValueType.REFERENCE) {
                        val reference = author.value as? Reference
                        validateReference(
                            reference,
                            validNoteAuthorValues,
                            LocationContext("Observation", "note[$index].author"),
                            validation
                        )
                    }
                }
            }

            // category and code (basics), dataAbsentReason, effective, status - validated by R4ObservationValidator
        }
    }

    /**
     * Validates the [element] against USCore Observation rules.
     */
    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {}

    /**
     * To validate subclasses against Ronin rules for a specific Observation type.
     * Observation type is defined by Ronin Common Data Model based on Observation.category and Observation.code.
     */
    open fun validateObservation(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkTrue(
                element.category.qualifiesForValueSet(qualifyingCategories),
                FHIRError(
                    code = "RONIN_OBS_002",
                    severity = ValidationIssueSeverity.ERROR,
                    description = "Must match this system|code: ${ qualifyingCategories.joinToString(", ") { "${it.system?.value}|${it.code?.value}" } }",
                    location = LocationContext(Observation::category)
                ),
                parentContext
            )

            checkTrue(
                element.code.qualifiesForValueSet(qualifyingCodes),
                FHIRError(
                    code = "RONIN_OBS_003",
                    severity = ValidationIssueSeverity.ERROR,
                    description = "Must match this system|code: ${ qualifyingCodes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" } }",
                    location = LocationContext(Observation::code)
                ),
                parentContext
            )
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
