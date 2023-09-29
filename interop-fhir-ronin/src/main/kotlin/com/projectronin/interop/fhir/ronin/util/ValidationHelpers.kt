package com.projectronin.interop.fhir.ronin.util

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.InvalidReferenceType
import com.projectronin.interop.fhir.validate.InvalidValueSetError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import kotlin.reflect.KProperty1

/**
 * Confirms that the [targetValue] is a valid code for enum [T], returning the enum if valid, otherwise null.
 */
inline fun <reified T> getCodedEnumOrNull(targetValue: String?): T? where T : Enum<T>, T : CodedEnum<T> =
    runCatching { CodedEnum.byCode<T>(targetValue ?: "") }.getOrNull()

fun validateReferenceList(
    referenceList: List<Reference>,
    resourceTypesList: List<ResourceType>,
    context: LocationContext,
    validation: Validation,
    containedResource: List<Resource<*>>? = listOf()
) {
    validation.apply {
        referenceList.forEachIndexed { index, reference ->
            val currentContext = LocationContext(context.element, "${context.field}[$index]")
            validateReference(reference, resourceTypesList, currentContext, validation, containedResource)
        }
    }
}

fun validateReference(
    reference: Reference?,
    resourceTypesList: List<ResourceType>,
    context: LocationContext,
    validation: Validation,
    containedResource: List<Resource<*>>? = listOf()
) {
    val resourceTypesStringList = resourceTypesList.map { it.name }
    val requiredContainedResource = FHIRError(
        code = "RONIN_REQ_REF_1",
        severity = ValidationIssueSeverity.ERROR,
        description = "Contained resource is required if a local reference is provided",
        location = LocationContext(context.element, context.field)
    )

    validation.apply {
        reference?.let {
            if (it.reference?.value.toString().startsWith("#")) {
                val id = it.reference?.value.toString().substringAfter("#")
                checkTrue(
                    !containedResource.isNullOrEmpty(),
                    requiredContainedResource,
                    LocationContext(context.element, "")
                )
                checkTrue(
                    resourceTypesStringList.contains(containedResource?.find { r -> r.id?.value == id }?.resourceType),
                    InvalidReferenceType(Reference::reference, resourceTypesList),
                    context
                )
            } else {
                checkTrue(
                    reference.isInTypeList(resourceTypesStringList),
                    InvalidReferenceType(Reference::reference, resourceTypesList),
                    context
                )
            }
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
 * contains a Coding that matches a Coding from the input [qualifyList] of Coding
 */
fun List<CodeableConcept>?.qualifiesForValueSet(qualifyList: List<Coding>): Boolean {
    return qualifyList.isEmpty() || (this?.let { it.any { con -> con.qualifiesForValueSet(qualifyList) } } == true)
}

/**
 * Check whether a CodeableConcept contains a Coding that matches a Coding from the input [qualifyList] of Coding
 */
fun CodeableConcept?.qualifiesForValueSet(qualifyList: List<Coding>): Boolean {
    return qualifyList.isEmpty() || (this?.let { it.coding.any { it.isInValueSet(qualifyList) } } == true)
}

/**
 * Lazy check for yyyy-mm-dd DateTime.value format. Examples: "2023" fails, "2023-01" fails, "2023-01-17" succeeds. Per
 * [US Core](http://hl7.org/fhir/us/core/STU5.0.1/StructureDefinition-us-core-observation-lab.html), check string length
 */
fun String?.hasDayFormat(): Boolean {
    this?.length?.let { if (it < 10) return false }
    return true
}
