package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.ronin.error.InvalidReferenceResourceTypeError
import com.projectronin.interop.fhir.validate.InvalidValueSetError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import kotlin.reflect.KProperty1

/**
 * Confirms that the [targetValue] is a valid code for enum [T], returning the enum if valid, otherwise null.
 */
inline fun <reified T> getCodedEnumOrNull(targetValue: String?): T? where T : Enum<T>, T : CodedEnum<T> =
    runCatching { CodedEnum.byCode<T>(targetValue ?: "") }.getOrNull()

fun validateReferenceList(
    referenceList: List<Reference>,
    resourceTypesList: List<String>,
    context: LocationContext,
    validation: Validation
) {
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

fun validateReference(
    reference: Reference?,
    resourceTypesList: List<String>,
    context: LocationContext,
    validation: Validation
) {
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
fun Coding?.isInValueSet(qualifyList: List<Coding>): Boolean {
    this?.let { coding ->
        return (qualifyList.isEmpty() || qualifyList.any { (it.system == coding.system) && (it.code == coding.code) })
    }
    return false
}

/**
 * Check whether the code in a Code matches a getValueSet() result from the NormalizationRegistryClient
 */
fun Code?.isInValueSet(qualifyList: List<Coding>): Boolean {
    this?.let { code ->
        return (qualifyList.isEmpty() || qualifyList.any { it.code == code })
    }
    return false
}

/**
 * Validates that the list of codeable concepts contains at least one coding in the provided value set.
 */
fun List<CodeableConcept>?.validateCodeInValueSet(
    requiredValueSet: List<Coding>,
    actualLocation: KProperty1<*, *>,
    context: LocationContext,
    validation: Validation
) {
    this?.let {
        validation.apply {
            checkTrue(
                it.isEmpty() || it.qualifiesForValueSet(requiredValueSet),
                InvalidValueSetError(
                    actualLocation,
                    it.joinToString { codeableConcept -> codeableConcept.coding.joinToString { coding -> "${coding.system?.value ?: "null"}|${coding.code?.value ?: "null"}" } }
                ),
                context
            )
        }
    }
}

/**
 * Check whether any CodeableConcept within a list of CodeableConcept
 * contains a Coding that matches a Coding from the input [qualifyingList] of Coding
 */
fun List<CodeableConcept>?.qualifiesForValueSet(qualifyList: List<Coding>): Boolean {
    this?.let { conceptList ->
        if (conceptList.isNotEmpty()) {
            return (qualifyList.isEmpty() || conceptList.any { con -> con.qualifiesForValueSet(qualifyList) })
        }
    }
    return false
}

/**
 * Check whether a CodeableConcept contains a Coding that matches a Coding from the input [qualifyingList] of Coding
 */
fun CodeableConcept?.qualifiesForValueSet(qualifyList: List<Coding>): Boolean {
    this?.let { concept ->
        return (qualifyList.isEmpty() || concept.coding.any { it.isInValueSet(qualifyList) })
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
