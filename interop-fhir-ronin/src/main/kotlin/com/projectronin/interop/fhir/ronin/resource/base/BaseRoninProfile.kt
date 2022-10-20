package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ProfileValidator
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity

/**
 * Base class capable of handling common tasks associated to Ronin profiles.
 */
abstract class BaseRoninProfile<T : Resource<T>>(
    extendedProfile: ProfileValidator<T>,
    private val profile: String
) : BaseProfile<T>(extendedProfile) {
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

    /**
     * Transforms the Meta relative to the current profile.
     */
    fun Meta?.transform(): Meta {
        val roninProfile = listOf(Canonical(this@BaseRoninProfile.profile))

        return this?.copy(profile = roninProfile) ?: Meta(profile = roninProfile)
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
    }

    /**
     * Validates that the supplied [identifier] list contains at least one valid tenant identifier.
     */
    private fun requireTenantIdentifier(
        identifier: List<Identifier>,
        parentContext: LocationContext,
        validation: Validation
    ) {
        val tenantIdentifier = identifier.find { it.system == RoninCodeSystem.TENANT.uri }
        validation.apply {
            checkNotNull(tenantIdentifier, requiredTenantIdentifierError, parentContext)
            ifNotNull(tenantIdentifier) {
                checkTrue(
                    tenantIdentifier.type == RoninCodeableConcepts.TENANT,
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
        val fhirIdentifier = identifier.find { it.system == RoninCodeSystem.FHIR_ID.uri }
        validation.apply {
            checkNotNull(fhirIdentifier, requiredFhirIdentifierError, parentContext)
            ifNotNull(fhirIdentifier) {
                // tenantIdentifier.use is constrained by the IdentifierUse enum type, so it needs no validation.
                checkTrue(
                    fhirIdentifier.type == RoninCodeableConcepts.FHIR_ID,
                    wrongFhirIdentifierTypeError,
                    parentContext
                )
                checkNotNull(fhirIdentifier.value, requiredFhirIdentifierValueError, parentContext)
            }
        }
    }
}
