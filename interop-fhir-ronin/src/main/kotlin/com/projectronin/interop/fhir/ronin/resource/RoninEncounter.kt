package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.validate.resource.R4EncounterValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

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
    private val requiredTypeError = RequiredFieldError(Encounter::type)
    private val requiredSubjectError = RequiredFieldError(Encounter::subject)

    private val requiredIdentifierSystemError = RequiredFieldError(Identifier::system)
    private val requiredIdentifierValueError = RequiredFieldError(Identifier::value)

    override fun validateRonin(element: Encounter, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)
        }
    }

    override fun validateUSCore(element: Encounter, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkTrue(element.type.isNotEmpty(), requiredTypeError, parentContext)

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
        tenant: Tenant
    ): Pair<Encounter?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )
        return Pair(transformed, Validation())
    }
}
