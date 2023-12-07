package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationAdministrationValidator
import com.projectronin.interop.fhir.r4.valueset.MedicationAdministrationStatus
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.error.ConceptMapInvalidValueSetError
import com.projectronin.interop.fhir.ronin.error.FailedConceptMapLookupError
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.resource.extractor.MedicationExtractor
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.ronin.util.getCodedEnumOrNull
import com.projectronin.interop.fhir.ronin.util.populateExtensionWithReference
import com.projectronin.interop.fhir.ronin.util.validateMedicationDatatype
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and transformer for the Ronin Medication Administration profile
 */
@Component
class RoninMedicationAdministration(
    normalizer: Normalizer,
    localizer: Localizer,
    private val medicationExtractor: MedicationExtractor,
    private val registryClient: NormalizationRegistryClient,
) :
    BaseRoninProfile<MedicationAdministration>(
            R4MedicationAdministrationValidator,
            RoninProfile.MEDICATION_ADMINISTRATION.value,
            normalizer,
            localizer,
        ) {
    override val rcdmVersion = RCDMVersion.V3_31_0
    override val profileVersion = 3

    private val requiredCategoryError =
        FHIRError(
            code = "RONIN_MEDADMIN_001",
            description = "More than one category cannot be present if category is not null",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(MedicationAdministration::category),
        )

    private val requiredMedicationReferenceError =
        FHIRError(
            code = "RONIN_MEDADMIN_002",
            description = "Medication must be a Reference",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(MedicationAdministration::medication),
        )
    private val invalidAppointmentStatusExtensionError =
        FHIRError(
            code = "RONIN_MEDADMIN_003",
            severity = ValidationIssueSeverity.ERROR,
            description = "Tenant source medication administration status extension is missing or invalid",
            location = LocationContext(MedicationAdministration::extension),
        )

    override fun validateRonin(
        element: MedicationAdministration,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            validateMedicationDatatype(element.extension, parentContext, this)

            element.medication?.let { medication ->
                checkTrue(
                    medication.type == DynamicValueType.REFERENCE,
                    requiredMedicationReferenceError,
                    parentContext,
                )
            }

            // category can only be of size 1 if it exists/is populated
            ifNotNull(element.category) {
                val categorySize = element.category?.coding?.size!! == 1
                checkTrue(
                    categorySize,
                    requiredCategoryError,
                    parentContext,
                )
            }

            // required subject is validated in R4
            ifNotNull(element.subject) {
                // check that subject reference has type and the extension is the data authority extension identifier
                requireDataAuthorityExtensionIdentifier(
                    element.subject,
                    LocationContext(MedicationAdministration::subject),
                    validation,
                )
            }

            // extension - status tenant source extension - 1..1
            checkTrue(
                element.extension.any {
                    it.url?.value == RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS.value &&
                        it.value?.type == DynamicValueType.CODING
                },
                invalidAppointmentStatusExtensionError,
                parentContext,
            )
        }
    }

    override fun conceptMap(
        normalized: MedicationAdministration,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): Pair<MedicationAdministration?, Validation> {
        val validation = Validation()

        val mappedStatusPair =
            normalized.status?.value?.let { statusValue ->
                val statusCode =
                    registryClient.getConceptMappingForEnum(
                        tenant,
                        "MedicationAdministration.status",
                        RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "MedicationAdministration.status", statusValue),
                        MedicationAdministrationStatus::class,
                        RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS.value,
                        normalized,
                        forceCacheReloadTS,
                    )

                if (statusCode == null) {
                    validation.checkTrue(
                        false,
                        FailedConceptMapLookupError(
                            LocationContext(MedicationAdministration::status),
                            statusValue,
                            RoninConceptMap.CODE_SYSTEMS.toUriString(tenant, "MedicationAdministration.status"),
                        ),
                        parentContext,
                    )
                    null
                } else {
                    val newCode = statusCode.coding.code

                    validation.checkNotNull(
                        getCodedEnumOrNull<MedicationAdministrationStatus>(newCode?.value),
                        ConceptMapInvalidValueSetError(
                            parentContext.append(LocationContext(MedicationAdministration::status)),
                            RoninConceptMap.CODE_SYSTEMS.toUriString(tenant, "MedicationAdministration.status"),
                            statusValue,
                            newCode?.value,
                            statusCode.metadata,
                        ),
                        parentContext,
                    )
                    Pair(
                        newCode,
                        statusCode.extension,
                    )
                }
            }

        val mapped =
            mappedStatusPair?.let {
                normalized.copy(
                    status = it.first,
                    extension = normalized.extension + it.second,
                )
            } ?: normalized
        return Pair(mapped, validation)
    }

    override fun transformInternal(
        normalized: MedicationAdministration,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): Pair<TransformResponse<MedicationAdministration>?, Validation> {
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
