package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationStatementValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.resource.extractor.MedicationExtractor
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.ronin.util.populateExtensionWithReference
import com.projectronin.interop.fhir.ronin.util.validateMedicationDatatype
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and transformer for the Ronin Medication Statement profile
 */
@Component
class RoninMedicationStatement(
    normalizer: Normalizer,
    localizer: Localizer,
    private val medicationExtractor: MedicationExtractor
) :
    BaseRoninProfile<MedicationStatement>(
        R4MedicationStatementValidator,
        RoninProfile.MEDICATION_STATEMENT.value,
        normalizer,
        localizer
    ) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    private val requiredMedicationReferenceError = FHIRError(
        code = "RONIN_MEDSTAT_001",
        description = "Medication must be a Reference",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(MedicationStatement::medication)
    )

    override fun validateRonin(element: MedicationStatement, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            // check that subject reference has type and the extension is the data authority extension identifier
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(
                    element.subject,
                    LocationContext(MedicationStatement::subject),
                    validation
                )
            }

            validateMedicationDatatype(element.extension, parentContext, this)

            element.medication?.let { medication ->
                checkTrue(
                    medication.type == DynamicValueType.REFERENCE,
                    requiredMedicationReferenceError,
                    parentContext
                )
            }
        }
    }

    override fun transformInternal(
        normalized: MedicationStatement,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<MedicationStatement>?, Validation> {
        val medicationExtraction =
            medicationExtractor.extractMedication(normalized.medication, normalized.contained, normalized)

        val medication = medicationExtraction?.updatedMedication ?: normalized.medication
        val contained = medicationExtraction?.updatedContained ?: normalized.contained
        val embeddedMedications = medicationExtraction?.extractedMedication?.let { listOf(it) } ?: emptyList()

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant),
            medication = medication,
            contained = contained,
            extension = normalized.extension + populateExtensionWithReference(normalized.medication) // populate extension based on medication[x]
        )

        return Pair(TransformResponse(transformed, embeddedMedications), Validation())
    }
}
