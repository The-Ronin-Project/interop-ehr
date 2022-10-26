package com.projectronin.interop.fhir.ronin.error

import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import kotlin.reflect.KProperty1

/**
 * When the [Coding] list for a [CodeableConcept] contains no [Coding] that passes validation for the Ronin profile.
 */
class RoninNoValidCodingError(actualLocation: LocationContext) : FHIRError(
    code = "RONIN_NOV_CODING",
    severity = ValidationIssueSeverity.ERROR,
    description = "No coding list entry provides all the required fields",
    location = actualLocation
) {
    /**
     * Creates a [NoValidCodingError] based off an explicit property.
     */
    constructor(actualLocation: KProperty1<*, *>) : this(LocationContext(actualLocation))
}
