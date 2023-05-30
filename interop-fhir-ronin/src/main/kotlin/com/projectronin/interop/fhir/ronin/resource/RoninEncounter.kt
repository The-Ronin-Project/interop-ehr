package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.validate.resource.R4EncounterValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and Transformer for the Ronin Encounter profile.
 */
@Component
class RoninEncounter(normalizer: Normalizer, localizer: Localizer) :
    USCoreBasedProfile<Encounter>(
        R4EncounterValidator,
        RoninProfile.ENCOUNTER.value,
        normalizer,
        localizer
    ) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 3

    private val requiredSubjectError = RequiredFieldError(Encounter::subject)

    private val requiredIdentifierSystemError = RequiredFieldError(Identifier::system)
    private val requiredIdentifierValueError = RequiredFieldError(Identifier::value)

    private val requiredExtensionError = RequiredFieldError(Encounter::extension)
    private val requiredExtensionClassError = FHIRError(
        code = "RONIN_ENC_001",
        description = "Encounter requires source class extension",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Encounter::extension)
    )
    private val requiredExtensionTypeError = FHIRError(
        code = "RONIN_ENC_002",
        description = "Encounter requires source type extension",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Encounter::extension)
    )
    private val requiredTypeSize = FHIRError(
        code = "RONIN_ENC_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "Must have one, and only one, type",
        location = LocationContext(Encounter::type)
    )
    private val requiredCodingSize = FHIRError(
        code = "RONIN_ENC_004",
        severity = ValidationIssueSeverity.ERROR,
        description = "One, and only one, coding entry is allowed for type",
        location = LocationContext(CodeableConcept::coding)
    )

    override fun validateRonin(element: Encounter, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            // check that subject reference has type and the extension is the data authority extension identifier
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(
                    element.subject,
                    LocationContext(Encounter::subject),
                    validation
                )
            }

            // TODO: Corrections for the below will come with the next profile update on RCDM 3.20.0, so leaving these commented out
            // See https://projectronin.slack.com/archives/C03RGK7PZU3/p1684879867024799

            // checkNotNull(element.extension, requiredExtensionError, parentContext)
            // checkTrue(
            //     element.extension.any {
            //         it.url == RoninExtension.TENANT_SOURCE_ENCOUNTER_CLASS.uri
            //     },
            //     requiredExtensionClassError,
            //     parentContext
            // )
            // checkTrue(
            //     element.extension.any {
            //         it.url == RoninExtension.TENANT_SOURCE_ENCOUNTER_TYPE.uri
            //     },
            //     requiredExtensionTypeError,
            //     parentContext
            // )

            if (element.type.size == 1) {
                checkTrue(
                    element.type[0].coding.size == 1,
                    requiredCodingSize,
                    parentContext.append(LocationContext("Encounter", "type[0]"))
                )
            } else {
                checkTrue(false, requiredTypeSize, parentContext)
            }
        }
    }

    override fun validateUSCore(element: Encounter, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.subject, requiredSubjectError, parentContext)
            validateReference(element.subject, listOf("Patient"), LocationContext(Encounter::subject), validation)

            element.identifier.forEachIndexed { index, identifier ->
                val identifierContext = parentContext.append(LocationContext("Encounter", "identifier[$index]"))
                checkNotNull(identifier.system, requiredIdentifierSystemError, identifierContext)
                checkNotNull(identifier.value, requiredIdentifierValueError, identifierContext)
            }

            // required status, required status value set, and required class errors are checked by R4EncounterValidator

            // BackboneElement required fields errors are checked by R4EncounterValidator
        }
    }

    override fun transformInternal(
        normalized: Encounter,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<Encounter?, Validation> {
        // TODO: The following are pending 3.20.0 updates. Review and implement the below based off the notes provided there.
        // TODO: RoninExtension.TENANT_SOURCE_ENCOUNTER_TYPE
        // TODO: Check concept maps for type code
        // TODO: RoninExtension.TENANT_SOURCE_ENCOUNTER_CLASS
        // TODO: Check concept maps for class code

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            extension = normalized.extension,
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )
        return Pair(transformed, Validation())
    }
}
