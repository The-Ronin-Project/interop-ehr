package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Validator and Transformer for the Ronin Practitioner profile.
 */
object RoninPractitioner :
    USCoreBasedProfile<Practitioner>(R4PractitionerValidator, RoninProfile.PRACTITIONER.value) {

    override fun validateRonin(element: Practitioner, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)

            // TODO: RoninNormalizedTelecom extension
        }
    }

    private val requiredNameError = RequiredFieldError(Practitioner::name)
    private val requiredNameFamilyError = RequiredFieldError(HumanName::family)

    override fun validateUSCore(element: Practitioner, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkTrue(element.name.isNotEmpty(), requiredNameError, parentContext)

            element.name.forEachIndexed { index, name ->
                val currentContext = parentContext.append(LocationContext("Practitioner", "name[$index]"))
                checkNotNull(name.family, requiredNameFamilyError, currentContext)
            }

            // A practitioner identifier is also required, but Ronin has already checked for identifiers we provide.
        }
    }

    override fun transformInternal(
        normalized: Practitioner,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Practitioner?, Validation> {
        // TODO: RoninNormalizedTelecom extension

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + tenant.toFhirIdentifier() + normalized.getFhirIdentifiers()
        )
        return Pair(transformed, Validation())
    }
}
