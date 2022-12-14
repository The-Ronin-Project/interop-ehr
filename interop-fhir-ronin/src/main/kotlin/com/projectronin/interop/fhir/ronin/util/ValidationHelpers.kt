package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.common.enums.CodedEnum
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
