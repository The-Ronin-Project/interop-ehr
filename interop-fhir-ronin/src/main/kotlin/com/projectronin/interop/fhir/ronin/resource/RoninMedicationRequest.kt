package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationRequestValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and transformer for the Ronin Medication Request profile
 */
@Component
class RoninMedicationRequest(normalizer: Normalizer, localizer: Localizer) :
    USCoreBasedProfile<MedicationRequest>(
        R4MedicationRequestValidator,
        RoninProfile.MEDICATION_REQUEST.value,
        normalizer,
        localizer
    ) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    private val validRequesterValues =
        listOf("Device", "Organization", "Patient", "Practitioner", "PractitionerRole", "RelatedPerson")

    private val requiredRequesterError = RequiredFieldError(MedicationRequest::requester)

    override fun validateRonin(element: MedicationRequest, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            // subject required is validated in R4
            validateReference(element.subject, listOf("Patient"), LocationContext(MedicationRequest::subject), this)

            // check that subject reference has type and the extension is the data authority extension identifier
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(
                    element.subject,
                    LocationContext(MedicationRequest::subject),
                    validation
                )
            }

            checkNotNull(element.requester, requiredRequesterError, parentContext)
            validateReference(
                element.requester,
                validRequesterValues,
                LocationContext(MedicationRequest::requester),
                this
            )

            // priority required value set is validated in R4
        }
    }

    override fun validateUSCore(element: MedicationRequest, parentContext: LocationContext, validation: Validation) {}

    override fun transformInternal(
        normalized: MedicationRequest,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<MedicationRequest?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )

        return Pair(transformed, Validation())
    }
}
