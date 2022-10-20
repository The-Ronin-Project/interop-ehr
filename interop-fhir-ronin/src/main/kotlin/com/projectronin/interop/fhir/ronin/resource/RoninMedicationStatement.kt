package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationStatementValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Validator and transformer for the Ronin Medication Statement profile
 */
object RoninMedicationStatement :
    BaseRoninProfile<MedicationStatement>(R4MedicationStatementValidator, RoninProfile.MEDICATION_STATEMENT.value) {

    override fun validate(element: MedicationStatement, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)
        }
    }

    override fun transformInternal(
        normalized: MedicationStatement,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<MedicationStatement?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )

        return Pair(transformed, Validation())
    }
}
