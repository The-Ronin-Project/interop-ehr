package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationStatementValidator
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Validator and transformer for the Ronin Medication Statement profile
 */
@Component
class RoninMedicationStatement(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninProfile<MedicationStatement>(
        R4MedicationStatementValidator,
        RoninProfile.MEDICATION_STATEMENT.value,
        normalizer,
        localizer
    ) {

    override fun validate(element: MedicationStatement, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            // required subject is validated in R4
            validateReference(element.subject, listOf("Patient"), LocationContext(MedicationStatement::subject), this)

            // check that subject reference has type and the extension is the data authority extension identifier
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(element.subject, LocationContext(MedicationStatement::subject), validation)
            }
        }
    }

    override fun transformInternal(
        normalized: MedicationStatement,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<MedicationStatement?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )

        return Pair(transformed, Validation())
    }
}
