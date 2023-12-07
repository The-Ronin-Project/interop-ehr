package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity

private val requiredMedicationDatatypeExtensionError =
    FHIRError(
        code = "RONIN_MEDDTEXT_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Extension must contain original Medication Datatype",
        location = LocationContext("", "extension"),
    )

private val invalidMedicationDatatypeExtensionValueError =
    FHIRError(
        code = "RONIN_MEDDTEXT_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Medication Datatype extension value is invalid",
        location = LocationContext("", "extension"),
    )

private val invalidMedicationDatatypeExtensionTypeError =
    FHIRError(
        code = "RONIN_MEDDTEXT_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "Medication Datatype extension type is invalid",
        location = LocationContext(MedicationAdministration::extension),
    )

fun validateMedicationDatatype(
    extensions: List<Extension>,
    parentContext: LocationContext,
    validation: Validation,
) {
    validation.apply {
        val medicationDatatypeExtension =
            extensions.singleOrNull { it.url?.value == RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value }
        checkNotNull(medicationDatatypeExtension, requiredMedicationDatatypeExtensionError, parentContext)

        ifNotNull(medicationDatatypeExtension) {
            checkNotNull(
                OriginalMedDataType.from(medicationDatatypeExtension.value?.value),
                invalidMedicationDatatypeExtensionValueError,
                parentContext,
            )
            checkTrue(
                medicationDatatypeExtension.value?.type == DynamicValueType.CODE,
                invalidMedicationDatatypeExtensionTypeError,
                parentContext,
            )
        }
    }
}
