package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append

/**
 * Base class capable of handling common tasks associated to Ronin profiles.
 */
abstract class BaseRoninProfile<T : Resource<T>>(
    extendedProfile: ProfileValidator<T>,
    protected val profile: String,
    normalizer: Normalizer,
    localizer: Localizer
) : BaseProfile<T>(extendedProfile, normalizer, localizer) {
    abstract val rcdmVersion: RCDMVersion
    abstract val profileVersion: Int

    private val requiredTenantIdentifierError = FHIRError(
        code = "RONIN_TNNT_ID_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Tenant identifier is required",
        location = LocationContext("", "identifier")
    )
    private val wrongTenantIdentifierTypeError = FHIRError(
        code = "RONIN_TNNT_ID_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Tenant identifier provided without proper CodeableConcept defined",
        location = LocationContext("", "identifier")
    )
    private val requiredTenantIdentifierValueError = FHIRError(
        code = "RONIN_TNNT_ID_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "Tenant identifier value is required",
        location = LocationContext("", "identifier")
    )

    private val requiredFhirIdentifierError = FHIRError(
        code = "RONIN_FHIR_ID_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "FHIR identifier is required",
        location = LocationContext("", "identifier")
    )
    private val wrongFhirIdentifierTypeError = FHIRError(
        code = "RONIN_FHIR_ID_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "FHIR identifier provided without proper CodeableConcept defined",
        location = LocationContext("", "identifier")
    )
    private val requiredFhirIdentifierValueError = FHIRError(
        code = "RONIN_FHIR_ID_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "FHIR identifier value is required",
        location = LocationContext("", "identifier")
    )
    private val containedResourcePresentWarning = FHIRError(
        code = "RONIN_CONTAINED_RESOURCE",
        severity = ValidationIssueSeverity.WARNING,
        description = "There is a Contained Resource present",
        location = LocationContext("", "contained")
    )

    private val requiredDataAuthorityIdentifierError = FHIRError(
        code = "RONIN_DAUTH_ID_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Data Authority identifier required",
        location = LocationContext("", "identifier")
    )
    private val wrongDataAuthorityIdentifierTypeError = FHIRError(
        code = "RONIN_DAUTH_ID_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Data Authority identifier provided without proper CodeableConcept defined",
        location = LocationContext("", "identifier")
    )
    private val requiredDataAuthorityIdentifierValueError = FHIRError(
        code = "RONIN_DAUTH_ID_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "Data Authority identifier value is required",
        location = LocationContext("", "identifier")
    )

    private val requiredDataAuthorityExtensionIdentifier = FHIRError(
        code = "RONIN_DAUTH_EX_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Data Authority extension identifier is required for reference",
        location = LocationContext("", "type.extension")
    )

    private val requiredReferenceType = FHIRError(
        code = "RONIN_REQ_REF_TYPE_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Attribute Type is required for the reference",
        location = LocationContext("", "type")
    )

    private val requiredMeta = RequiredFieldError(LocationContext("", "meta"))
    private val requiredMetaProfile = FHIRError(
        code = "RONIN_META_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "No profiles found for expected type `$profile`",
        location = LocationContext(Meta::profile)
    )
    private val requiredMetaSource = RequiredFieldError(Meta::source)

    /**
     * Validates the [element] against Ronin's rules.
     */
    abstract fun validateRonin(element: T, parentContext: LocationContext, validation: Validation)

    override fun validate(element: T, parentContext: LocationContext, validation: Validation) {
        validateRonin(element, parentContext, validation)
    }

    /**
     * When the [Coding] list for a [CodeableConcept] contains no [Coding] that passes validation for the Ronin profile.
     */
    fun roninInvalidCodingError(field: String) = FHIRError(
        code = "RONIN_NOV_CODING_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Coding list entry missing the required fields",
        location = LocationContext("", field)
    )

    /**
     * When the [Coding] list for a [CodeableConcept] has more than one entry with userSelected set, we cannot choose.
     */
    private fun roninInvalidUserSelectError(field: String) = FHIRError(
        code = "RONIN_INV_CODING_SEL_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "More than one coding entry has userSelected true",
        location = LocationContext("", field)
    )

    /**
     * Transforms the Meta relative to the current profile. If more than one profile qualifies add the qualifying
     * and is a ronin profile add the profile canonical to the meta profile list, profile must exist in roninProfile enum
     */
    fun Meta?.transform(): Meta {
        val roninProfiles = RoninProfile.values().map { it.value }
        val roninProfile = this?.profile ?: emptyList()
        val transformedProfile = roninProfile.plus(Canonical(this@BaseRoninProfile.profile)).filter { it.value in roninProfiles }.distinct()
        return this?.copy(profile = transformedProfile) ?: Meta(profile = transformedProfile)
    }

    /**
     * Validates the supplied [meta] contains the required elements for all Ronin profiles.
     */
    protected fun requireMeta(
        meta: Meta?,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.apply {
            checkNotNull(meta, requiredMeta, parentContext)

            ifNotNull(meta) {
                val metaContext = parentContext.append(LocationContext("", "meta"))
                checkTrue(meta.profile.contains(Canonical(profile)), requiredMetaProfile, metaContext)
                checkNotNull(meta.source, requiredMetaSource, metaContext)
            }
        }
    }

    /**
     * Validates that the supplied [identifier] list contains the required Ronin identifiers.
     */
    protected fun requireRoninIdentifiers(
        identifier: List<Identifier>,
        parentContext: LocationContext,
        validation: Validation
    ) {
        requireTenantIdentifier(identifier, parentContext, validation)
        requireFhirIdentifier(identifier, parentContext, validation)
        requireDataAuthorityIdentifier(identifier, parentContext, validation)
    }

    /**
     * Validates that the supplied [identifier] list contains at least one valid tenant identifier.
     */
    private fun requireTenantIdentifier(
        identifier: List<Identifier>,
        parentContext: LocationContext,
        validation: Validation
    ) {
        val tenantIdentifier = identifier.find { it.system == CodeSystem.RONIN_TENANT.uri }
        validation.apply {
            checkNotNull(tenantIdentifier, requiredTenantIdentifierError, parentContext)
            ifNotNull(tenantIdentifier) {
                checkTrue(
                    tenantIdentifier.type == CodeableConcepts.RONIN_TENANT,
                    wrongTenantIdentifierTypeError,
                    parentContext
                )
                checkNotNull(tenantIdentifier.value, requiredTenantIdentifierValueError, parentContext)
            }
        }
    }

    /**
     * Validates that the supplied [identifier] list contains at least one valid FHIR identifier.
     */
    private fun requireFhirIdentifier(
        identifier: List<Identifier>,
        parentContext: LocationContext,
        validation: Validation
    ) {
        val fhirIdentifier = identifier.find { it.system == CodeSystem.RONIN_FHIR_ID.uri }
        validation.apply {
            checkNotNull(fhirIdentifier, requiredFhirIdentifierError, parentContext)
            ifNotNull(fhirIdentifier) {
                // tenantIdentifier.use is constrained by the IdentifierUse enum type, so it needs no validation.
                checkTrue(
                    fhirIdentifier.type == CodeableConcepts.RONIN_FHIR_ID,
                    wrongFhirIdentifierTypeError,
                    parentContext
                )
                checkNotNull(fhirIdentifier.value, requiredFhirIdentifierValueError, parentContext)
            }
        }
    }

    protected fun containedResourcePresent(
        containedResource: List<Resource<*>>,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.apply {
            checkTrue(containedResource.isEmpty(), containedResourcePresentWarning, parentContext)
            if (containedResource.isNotEmpty()) {
                logger.warn { "contained resource found @ $parentContext" }
            }
        }
    }

    /**
     * Validates that the supplied [identifier] list contains the Data Authority identifier.
     */
    private fun requireDataAuthorityIdentifier(
        identifier: List<Identifier>,
        parentContext: LocationContext,
        validation: Validation
    ) {
        val dataAuthorityIdentifier = identifier.find { it.system == CodeSystem.RONIN_DATA_AUTHORITY.uri }
        validation.apply {
            checkNotNull(dataAuthorityIdentifier, requiredDataAuthorityIdentifierError, parentContext)
            ifNotNull(dataAuthorityIdentifier) {
                checkTrue(
                    dataAuthorityIdentifier.type == CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    wrongDataAuthorityIdentifierTypeError,
                    parentContext
                )

                checkNotNull(dataAuthorityIdentifier.value, requiredDataAuthorityIdentifierValueError, parentContext)
            }
        }
    }

    protected fun requireDataAuthorityExtensionIdentifier(
        reference: Reference?,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.apply {
            val referenceType = reference?.type

            ifNotNull(referenceType) {
                // This only matters if the reference is populated. If the reference is not populated, then it's not part of a Data Authority.
                reference?.reference?.let {
                    val dataAuthExtensionIdentifier = referenceType?.extension
                    checkTrue(
                        dataAuthExtensionIdentifier == dataAuthorityExtension,
                        requiredDataAuthorityExtensionIdentifier,
                        parentContext
                    )
                }
            }
        }
    }

    /**
     * Validates a [CodeableConcept] object and its child [Coding] against the Ronin profile. Specify the [fieldName]
     * for the [CodeableConcept] (example: "code" for Medication.code
     */
    protected fun requireCodeableConcept(
        fieldName: String,
        code: CodeableConcept?,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.apply {
            code?.let {
                requireCoding(fieldName, code.coding, parentContext, validation)
            }
        }
    }

    /**
     * Validates a [Coding] object against the Ronin profile. The [Coding] has a parent [CodeableConcept] field whose
     * name is in [parentFieldName] (example: "code" for Medication.code)
     */
    private fun requireCoding(
        parentFieldName: String,
        coding: List<Coding>?,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.apply {
            checkNotNull(
                coding,
                RequiredFieldError(LocationContext("", "$parentFieldName.coding")),
                parentContext
            )
            ifNotNull(coding) {
                checkTrue(
                    coding.all {
                        // The FHIR spec indicates all strings should contain non-whitespace content, so checking for blank
                        !it.system?.value.isNullOrBlank() && !it.code?.value.isNullOrBlank()
                        // TODO: Re-enable after ConceptMapping in place
                        //  && !it.display?.value.isNullOrBlank()
                    },
                    roninInvalidCodingError(parentFieldName),
                    parentContext
                )
                checkTrue(
                    coding.filter { it.userSelected?.value == true }.size <= 1,
                    roninInvalidUserSelectError(parentFieldName),
                    parentContext
                )
            }
        }
    }

    /**
     * Validates that the [Coding] list is not empty. (examples: Condition CNDPAHC & CNDEDX)
     */
    protected fun requireCodeCoding(
        parentFieldName: String,
        coding: List<Coding>?,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.apply {
            coding?.let {
                checkTrue(
                    it.isNotEmpty(),
                    roninInvalidCodingError(parentFieldName),
                    parentContext
                )
            }
        }
    }

    /**
     * Creates an [Extension] list using the supplied [url] and [CodeableConcept] object. Supports the calling transform
     * by checking input for nulls and returning a list or emptyList. This supports simpler list arithmetic in the caller.
     */
    protected fun getExtensionOrEmptyList(url: RoninExtension, codeableConcept: CodeableConcept?): List<Extension> {
        return codeableConcept?.let {
            listOf(
                Extension(
                    url = Uri(url.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = codeableConcept
                    )
                )
            )
        } ?: emptyList()
    }

    /**
     * Creates an [Extension] list using the supplied [url] and [Coding] object. Supports the calling transform
     * by checking input for nulls and returning a list or emptyList. This supports simpler list arithmetic in the caller.
     */
    protected fun getExtensionOrEmptyList(url: RoninExtension, coding: Coding?): List<Extension> {
        return coding?.let {
            listOf(
                Extension(
                    url = Uri(url.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = coding
                    )
                )
            )
        } ?: emptyList()
    }
}
