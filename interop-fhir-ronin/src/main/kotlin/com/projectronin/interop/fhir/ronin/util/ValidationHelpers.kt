package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.ronin.error.InvalidReferenceResourceTypeError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation

/**
 * Confirms that the [targetValue] is a valid code for enum [T], returning the enum if valid, otherwise null.
 */
inline fun <reified T> getCodedEnumOrNull(targetValue: String?): T? where T : Enum<T>, T : CodedEnum<T> =
    runCatching { CodedEnum.byCode<T>(targetValue ?: "") }.getOrNull()

fun validateReferenceList(referenceList: List<Reference>, resourceTypesList: List<String>, context: LocationContext, validation: Validation) {
    validation.apply {
        referenceList.forEachIndexed { index, reference ->
            val currentContext = LocationContext(context.element, "${context.field}[$index]")
            checkTrue(
                reference.isInTypeList(resourceTypesList),
                InvalidReferenceResourceTypeError(currentContext, resourceTypesList),
                LocationContext(context.element, "")
            )
        }
    }
}

fun validateReference(reference: Reference?, resourceTypesList: List<String>, context: LocationContext, validation: Validation) {
    validation.apply {
        ifNotNull(reference) {
            checkTrue(
                reference.isInTypeList(resourceTypesList),
                InvalidReferenceResourceTypeError(context, resourceTypesList),
                LocationContext(context.element, "")
            )
        }
    }
}

fun Reference?.isInTypeList(resourceTypeList: List<String>): Boolean {
    this?.let { reference ->
        resourceTypeList.forEach { value ->
            if (reference.isForType(value)) {
                return true
            }
        }
    }
    return false
}

/**
 * Check whether the system and code in a Coding match a getValueSet() result from the NormalizationRegistryClient
 */
fun Coding?.isInValueSet(valueSetCodingList: List<Coding>): Boolean {
    this?.let { coding ->
        return valueSetCodingList.any { (it.system == coding.system) && (it.code == coding.code) }
    }
    return false
}

/**
 * Lazy check for yyyy-mm-dd DateTime.value format. Examples: "2023" fails, "2023-01" fails, "2023-01-17" succeeds. Per
 * [US Core](http://hl7.org/fhir/us/core/STU5.0.1/StructureDefinition-us-core-observation-lab.html), check string length
 */
fun String?.hasDayFormat(): Boolean {
    this?.length?.let { if (it < 10) return false }
    return true
}
