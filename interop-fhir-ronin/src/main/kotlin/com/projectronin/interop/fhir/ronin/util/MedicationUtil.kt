package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.ronin.profile.RoninExtension

enum class OriginalMedDataType(val value: Code) {
    LiteralReference(Code("literal reference")),
    LogicalReference(Code("logical reference")),
    ContainedReference(Code("contained reference")),
    CodeableConcept(Code("codeable concept"));

    companion object { // helper to check value of extension populated from medication[x]
        infix fun from(value: Any?): OriginalMedDataType? =
            value?.let { OriginalMedDataType.values().firstOrNull { it.value == value } }
    }
}

enum class OriginalDynamicType(val value: String) {
    CodeableConcept("CODEABLE_CONCEPT")
}

/**
 * Medication Administration Extension needs to be populated with Original Medication DataType
 * based on the medication reference value. The value of the extension must be one of the following:
 * literal reference (e.g. "reference": "Patient/1234") |
 * logical reference (e.g. reference contains an identifier, https://www.hl7.org/fhir/references.html) |
 * contained reference (e.g. "reference": "#00000000" starts with # |
 * codeable concept (e.g. type is codeable concept)
 */
fun populateExtensionWithReference(
    normalized: DynamicValue<Any>?
): List<Extension> {
    val type = normalized?.type ?: return emptyList()
    val extensionValue = when (type) {
        DynamicValueType.CODEABLE_CONCEPT -> OriginalMedDataType.CodeableConcept.value
        DynamicValueType.REFERENCE -> getReferenceType(normalized)
        else -> null
    }
    return if (extensionValue == null) {
        emptyList()
    } else {
        listOf(
            Extension(
                url = Uri(RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                value = DynamicValue(type = DynamicValueType.CODE, value = extensionValue)
            )
        )
    }
}

/**
 * Get reference type in order to populate the extension for Medication Administration
 */
private fun getReferenceType(medicationReference: DynamicValue<Any>?): Code? {
    val reference = medicationReference?.value as Reference
    return if (reference.identifier != null) {
        OriginalMedDataType.LogicalReference.value
    } else if (reference.reference?.value?.startsWith("#") == true) {
        OriginalMedDataType.ContainedReference.value
    } else if (reference.reference?.value?.contains("/") == true) {
        OriginalMedDataType.LiteralReference.value
    } else {
        null
    }
}

/**
 * Check that reference type is Medication for Medication Administration
 */
fun checkReferenceType(medication: DynamicValue<Any>?): Boolean {
    return (medication?.value as Reference).type?.value == "Medication"
}
