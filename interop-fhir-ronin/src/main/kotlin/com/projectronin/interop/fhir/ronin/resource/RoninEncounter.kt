package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.validate.resource.R4EncounterValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
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
    override val rcdmVersion = RCDMVersion.V3_20_0
    override val profileVersion = 4

    private val requiredSubjectError = RequiredFieldError(Encounter::subject)

    private val requiredIdentifierSystemError = RequiredFieldError(Identifier::system)
    private val requiredIdentifierValueError = RequiredFieldError(Identifier::value)

    private val requiredExtensionClassError = FHIRError(
        code = "RONIN_ENC_001",
        description = "Tenant source encounter class extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Encounter::extension)
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

            checkTrue(
                element.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_ENCOUNTER_CLASS.uri &&
                        it.value?.type == DynamicValueType.CODING
                },
                requiredExtensionClassError,
                parentContext
            )
        }
    }

    private val requiredTypeError = RequiredFieldError(Encounter::type)

    override fun validateUSCore(element: Encounter, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkTrue(element.type.isNotEmpty(), requiredTypeError, parentContext)

            checkNotNull(element.subject, requiredSubjectError, parentContext)

            element.identifier.forEachIndexed { index, identifier ->
                val identifierContext = parentContext.append(LocationContext("Encounter", "identifier[$index]"))
                checkNotNull(identifier.system, requiredIdentifierSystemError, identifierContext)
                checkNotNull(identifier.value, requiredIdentifierValueError, identifierContext)
            }

            // required status, required status value set, and required class errors are checked by R4EncounterValidator

            // BackboneElement required fields errors are checked by R4EncounterValidator
        }
    }

    override fun conceptMap(
        normalized: Encounter,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<Encounter, Validation> {
        // TODO: Check concept maps for class code
        val classExtension = getExtensionOrEmptyList(RoninExtension.TENANT_SOURCE_ENCOUNTER_CLASS, normalized.`class`)
        val mapped = normalized.copy(
            extension = normalized.extension + classExtension
        )
        return Pair(mapped, Validation())
    }

    override fun transformInternal(
        normalized: Encounter,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<Encounter>?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )
        return Pair(TransformResponse(transformed), Validation())
    }
}
