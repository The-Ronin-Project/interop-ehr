package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationStatementValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.util.localize
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
        original: MedicationStatement,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<MedicationStatement?, Validation> {
        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta.transform(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + original.getFhirIdentifiers() + tenant.toFhirIdentifier(),
            basedOn = original.basedOn.map { it.localize(tenant) },
            partOf = original.partOf.map { it.localize(tenant) },
            statusReason = original.statusReason.map { it.localize(tenant) },
            category = original.category?.localize(tenant),
            medication = when (original.medication?.type) {
                DynamicValueType.CODEABLE_CONCEPT -> {
                    val codeableConcept = (original.medication?.value as CodeableConcept).localize(tenant)
                    DynamicValue<Any>(original.medication!!.type, codeableConcept)
                }
                DynamicValueType.REFERENCE -> {
                    val reference = (original.medication?.value as Reference).localize(tenant)
                    DynamicValue<Any>(original.medication!!.type, reference)
                }
                else -> original.medication
            },
            subject = original.subject?.localize(tenant),
            context = original.context?.localize(tenant),
            effective = when (original.effective?.type) {
                DynamicValueType.PERIOD -> {
                    val period = (original.effective?.value as Period).localize(tenant)
                    DynamicValue<Any>(original.effective!!.type, period)
                }
                else -> original.effective
            },
            informationSource = original.informationSource?.localize(tenant),
            derivedFrom = original.derivedFrom.map { it.localize(tenant) },
            reasonCode = original.reasonCode.map { it.localize(tenant) },
            reasonReference = original.reasonReference.map { it.localize(tenant) },
            note = original.note.map { it.localize((tenant)) },
            dosage = original.dosage.map { it.localize(tenant) }
        )

        return Pair(transformed, Validation())
    }
}
