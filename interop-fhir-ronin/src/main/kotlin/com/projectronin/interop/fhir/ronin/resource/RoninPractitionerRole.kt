package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerRoleValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Validator and Transformer for the Ronin [OncologyPractitionerRole](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-practitionerrole.html) profile.
 */
object RoninPractitionerRole : USCoreBasedProfile<PractitionerRole>(
    R4PractitionerRoleValidator,
    RoninProfile.PRACTITIONER_ROLE.value
) {
    private val requiredPractitionerError = RequiredFieldError(PractitionerRole::practitioner)

    override fun validateRonin(element: PractitionerRole, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)

            checkNotNull(element.practitioner, requiredPractitionerError, parentContext)
        }
    }

    private val requiredTelecomValueError = RequiredFieldError(ContactPoint::value)

    override fun validateUSCore(element: PractitionerRole, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            element.telecom.forEachIndexed { index, contactPoint ->
                val currentContext = parentContext.append(LocationContext("PractitionerRole", "telecom[$index]"))
                // R4ContactPoint already verifies that a system is present if a value is present, so just checking value here.
                checkNotNull(contactPoint.value, requiredTelecomValueError, currentContext)
            }
        }
    }

    private val requiredIdError = RequiredFieldError(Practitioner::id)
    private val requiredTelecomSystemWarning = FHIRError(
        code = "USCORE_PRACRL_001",
        severity = ValidationIssueSeverity.WARNING,
        description = "telecom filtered for no system",
        location = LocationContext(ContactPoint::system)
    )
    private val requiredTelecomValueWarning = FHIRError(
        code = "USCORE_PRACRL_002",
        severity = ValidationIssueSeverity.WARNING,
        description = "telecom filtered for no value",
        location = LocationContext(ContactPoint::value)
    )

    override fun transformInternal(
        original: PractitionerRole,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<PractitionerRole?, Validation> {
        val validation = Validation()

        val invalidTelecoms = original.telecom.filterIndexed { index, contactPoint ->
            val nullSystem = contactPoint.system == null
            val nullValue = contactPoint.value == null

            validation.apply {
                val currentContext = parentContext.append(LocationContext("PractitionerRole", "telecom[$index]"))
                checkTrue(!nullSystem, requiredTelecomSystemWarning, currentContext)
                checkTrue(!nullValue, requiredTelecomValueWarning, currentContext)
            }

            nullSystem || nullValue
        }.toSet()

        val telecoms = original.telecom - invalidTelecoms
        if (invalidTelecoms.isNotEmpty()) {
            logger.info { "${invalidTelecoms.size} telecoms removed from PractitionerRole ${original.id} due to missing system and/or value" }
        }

        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta.transform(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + original.getFhirIdentifiers() + tenant.toFhirIdentifier(),
            period = original.period?.localize(tenant),
            practitioner = original.practitioner?.localize(tenant),
            organization = original.organization?.localize(tenant),
            code = original.code.map { it.localize(tenant) },
            specialty = original.specialty.map { it.localize(tenant) },
            location = original.location.map { it.localize(tenant) },
            healthcareService = original.healthcareService.map { it.localize(tenant) },
            telecom = telecoms.map { it.localize(tenant) },
            availableTime = original.availableTime.map { it.localize(tenant) },
            notAvailable = original.notAvailable.map { it.localize(tenant) },
            endpoint = original.endpoint.map { it.localize(tenant) }
        )
        return Pair(transformed, validation)
    }
}
