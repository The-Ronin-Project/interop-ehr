package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationAdministrationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.ronin.util.OriginalMedDataType
import com.projectronin.interop.fhir.ronin.util.populateExtensionWithReference
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and transformer for the Ronin Medication Administration profile
 */
@Component
class RoninMedicationAdministration(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninProfile<MedicationAdministration>(
        R4MedicationAdministrationValidator,
        RoninProfile.MEDICATION_ADMINISTRATION.value,
        normalizer,
        localizer
    ) {
    override val rcdmVersion = RCDMVersion.V3_27_0
    override val profileVersion = 1

    private val requiredCategoryError = FHIRError(
        code = "RONIN_MEDADMIN_001",
        description = "More than one category cannot be present if category is not null",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(MedicationAdministration::category)
    )

    private val invalidMedicationAdministrationExtensionUrlError = FHIRError(
        code = "RONIN_MEDADMIN_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Medication Administration extension url is invalid",
        location = LocationContext(MedicationAdministration::extension)
    )

    private val invalidMedicationAdministrationExtensionValueError = FHIRError(
        code = "RONIN_MEDADMIN_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "Medication Administration extension value is invalid",
        location = LocationContext(MedicationAdministration::extension)
    )

    private val invalidMedicationAdministrationExtensionTypeError = FHIRError(
        code = "RONIN_MEDADMIN_004",
        severity = ValidationIssueSeverity.ERROR,
        description = "Medication Administration extension type is invalid",
        location = LocationContext(MedicationAdministration::extension)
    )

    private val requiredMedicationAdministrationExtensionError = FHIRError(
        code = "RONIN_MEDADMIN_005",
        severity = ValidationIssueSeverity.ERROR,
        description = "Medication Administration extension list cannot be empty or the value of medication[x] is NOT\n" +
            " one of the following: codeable concept|contained|literal|logical reference",
        location = LocationContext(MedicationAdministration::extension)
    )

    private val requiredMedicationAdministrationExtensionSizeError = FHIRError(
        code = "RONIN_MEDADMIN_006",
        severity = ValidationIssueSeverity.ERROR,
        description = "Medication Administration extension list cannot exceed size of one(1)",
        location = LocationContext(MedicationAdministration::extension)
    )

    override fun validateRonin(
        element: MedicationAdministration,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            // extension - not empty
            val extension = element.extension
            checkTrue(extension.isNotEmpty(), requiredMedicationAdministrationExtensionError, parentContext)

            if (extension.isNotEmpty()) {
                // if not empty - can only be 1..1
                checkTrue(
                    extension.size == 1,
                    requiredMedicationAdministrationExtensionSizeError,
                    parentContext
                )
            }

            if (extension.size == 1) {
                // extension - original medication datatype extension - url value must be ORIGINAL_MEDICATION_DATATYPE
                checkTrue(
                    extension.all { it.url?.value == RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value },
                    invalidMedicationAdministrationExtensionUrlError,
                    parentContext
                )
                // check that the value in the extension is an acceptable value (OriginalMedDataType enum)
                checkTrue(
                    extension.all { OriginalMedDataType from it.value?.value!! != null },
                    invalidMedicationAdministrationExtensionValueError,
                    parentContext
                )
                // check that the extension type is DynamicValueType.CODE
                checkTrue(
                    extension.all { it.value?.type == DynamicValueType.CODE },
                    invalidMedicationAdministrationExtensionTypeError,
                    parentContext
                )
            }

            // category can only be of size 1 if it exists/is populated
            ifNotNull(element.category) {
                checkTrue(element.category?.coding?.size!! == 1, requiredCategoryError, parentContext)
            }

            // required subject is validated in R4
            ifNotNull(element.subject) {
                // check that subject reference has type and the extension is the data authority extension identifier
                requireDataAuthorityExtensionIdentifier(
                    element.subject,
                    LocationContext(MedicationAdministration::subject),
                    validation
                )
            }
        }
    }

    override fun transformInternal(
        normalized: MedicationAdministration,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<MedicationAdministration>?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant),
            extension = normalized.extension + populateExtensionWithReference(normalized.medication) // populate extension based on medication[x]
        )

        return Pair(TransformResponse(transformed), Validation())
    }
}
