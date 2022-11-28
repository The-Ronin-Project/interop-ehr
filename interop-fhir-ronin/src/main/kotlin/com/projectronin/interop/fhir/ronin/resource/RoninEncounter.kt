package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.validate.resource.R4EncounterValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.RoninMedication.transform
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Validator and Transformer for the Ronin Encounter profile.
 */
object RoninEncounter :
    USCoreBasedProfile<Encounter>(
        R4EncounterValidator,
        RoninProfile.ENCOUNTER.value
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

            element.identifier.forEachIndexed { index, identifier ->
                val identifierContext = parentContext.append(LocationContext("Encounter", "identifier[$index]"))
                checkNotNull(identifier.system, requiredIdentifierSystemError, identifierContext)
                checkNotNull(identifier.value, requiredIdentifierValueError, identifierContext)
            }

            // validation of required status, required status value set, and required class is inherited from R4

            // validation of the BackboneElement required fields is inherited from R4
        }
    }

    override fun transformInternal(
        normalized: Encounter,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Encounter?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier(),
        )
        return Pair(transformed, Validation())
    }
}