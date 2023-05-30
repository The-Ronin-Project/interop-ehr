package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerRoleValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
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
 * Validator and Transformer for the Ronin [OncologyPractitionerRole](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-practitionerrole.html) profile.
 */
@Component
class RoninPractitionerRole(normalizer: Normalizer, localizer: Localizer) : USCoreBasedProfile<PractitionerRole>(
    R4PractitionerRoleValidator,
    RoninProfile.PRACTITIONER_ROLE.value,
    normalizer,
    localizer
) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    private val requiredPractitionerError = RequiredFieldError(PractitionerRole::practitioner)

    override fun validateRonin(element: PractitionerRole, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

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
        normalized: PractitionerRole,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<PractitionerRole?, Validation> {
        val validation = Validation()

        val invalidTelecoms = normalized.telecom.filterIndexed { index, contactPoint ->
            val nullSystem = contactPoint.system == null
            val nullValue = contactPoint.value == null

            validation.apply {
                val currentContext = parentContext.append(LocationContext("PractitionerRole", "telecom[$index]"))
                checkTrue(!nullSystem, requiredTelecomSystemWarning, currentContext)
                checkTrue(!nullValue, requiredTelecomValueWarning, currentContext)
            }

            nullSystem || nullValue
        }.toSet()

        val telecoms = normalized.telecom - invalidTelecoms
        if (invalidTelecoms.isNotEmpty()) {
            logger.info { "${invalidTelecoms.size} telecoms removed from PractitionerRole ${normalized.id} due to missing system and/or value" }
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant),
            telecom = telecoms
        )
        return Pair(transformed, validation)
    }
}
