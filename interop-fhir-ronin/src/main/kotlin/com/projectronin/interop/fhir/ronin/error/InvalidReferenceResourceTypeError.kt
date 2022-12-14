package com.projectronin.interop.fhir.ronin.error

import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import kotlin.reflect.KProperty1

/**
 * Defines the error for an invalid resource type in a reference.
 * Example: an Observation.subject being a "PractitionerRole" resource type.
 */
class InvalidReferenceResourceTypeError(
    actualLocation: LocationContext,
    validValues: List<String>
) :
    FHIRError(
        "RONIN_INV_REF_TYPE",
        ValidationIssueSeverity.ERROR,
        "The referenced resource type was not${validValues.toIntro()} ${validValues.toDisplay()}",
        actualLocation
    ) {
    /**
     * Creates an InvalidReferenceResourceTypeError based off an explicit property.
     */
    constructor(actualLocation: KProperty1<*, *>, validValues: List<String>) : this(
        LocationContext(actualLocation),
        validValues
    )
}

private fun List<String>.toIntro(): String = if (this.size > 1) " one of" else ""
private fun List<String>.toDisplay(): String = if (this.isNotEmpty()) this.joinToString() else "valid"
