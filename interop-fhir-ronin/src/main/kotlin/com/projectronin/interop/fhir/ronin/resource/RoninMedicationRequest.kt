package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationRequestValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.resource.extractor.MedicationExtractor
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.ronin.util.populateExtensionWithReference
import com.projectronin.interop.fhir.ronin.util.validateMedicationDatatype
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and transformer for the Ronin Medication Request profile
 */
@Component
class RoninMedicationRequest(
    normalizer: Normalizer,
    localizer: Localizer,
    private val medicationExtractor: MedicationExtractor,
) :
    USCoreBasedProfile<MedicationRequest>(
            R4MedicationRequestValidator,
            RoninProfile.MEDICATION_REQUEST.value,
            normalizer,
            localizer,
        ) {
    override val rcdmVersion = RCDMVersion.V3_29_0
    override val profileVersion = 3

    private val requiredRequesterError = RequiredFieldError(MedicationRequest::requester)

    private val requiredMedicationReferenceError =
        FHIRError(
            code = "RONIN_MEDREQ_001",
            description = "Medication must be a Reference",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(MedicationRequest::medication),
        )

    override fun validateRonin(
        element: MedicationRequest,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            // check that subject reference extension is the data authority extension identifier
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(
                    element.subject,
                    LocationContext(MedicationRequest::subject),
                    validation,
                )
            }

            checkNotNull(element.requester, requiredRequesterError, parentContext)

            validateMedicationDatatype(element.extension, parentContext, this)

            element.medication?.let { medication ->
                checkTrue(
                    medication.type == DynamicValueType.REFERENCE,
                    requiredMedicationReferenceError,
                    parentContext,
                )
            }

            // priority required value set is validated in R4
        }
    }

    override fun validateUSCore(
        element: MedicationRequest,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validation.apply {
            validateReference(
                element.subject,
                listOf(ResourceType.Patient),
                LocationContext(MedicationRequest::subject),
                this,
            )
        }
    }

    override fun transformInternal(
        normalized: MedicationRequest,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): Pair<TransformResponse<MedicationRequest>?, Validation> {
        val medicationExtraction =
            medicationExtractor.extractMedication(normalized.medication, normalized.contained, normalized)

        val medication = medicationExtraction?.updatedMedication ?: normalized.medication
        val contained = medicationExtraction?.updatedContained ?: normalized.contained
        val embeddedMedications = medicationExtraction?.extractedMedication?.let { listOf(it) } ?: emptyList()

        val transformed =
            normalized.copy(
                meta = normalized.meta.transform(),
                identifier = normalized.getRoninIdentifiersForResource(tenant),
                medication = medication,
                contained = contained,
                // populate extension based on medication[x]
                extension = normalized.extension + populateExtensionWithReference(normalized.medication),
            )

        return Pair(TransformResponse(transformed, embeddedMedications), Validation())
    }
}
