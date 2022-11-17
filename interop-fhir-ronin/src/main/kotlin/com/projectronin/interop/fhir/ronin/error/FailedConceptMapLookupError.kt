package com.projectronin.interop.fhir.ronin.error

import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import kotlin.reflect.KProperty1

/**
 * Defines the error for a concept map lookup failure.
 * Either a concept map of this name was not found,
 * or the source value was not mapped to a target in this concept map.
 */
class FailedConceptMapLookupError(
    actualLocation: LocationContext,
    sourceValue: String,
    conceptMapName: String
) :
    FHIRError(
        "NOV_CONMAP_LOOKUP",
        ValidationIssueSeverity.ERROR,
        "Tenant source value '$sourceValue' has no target defined in $conceptMapName",
        actualLocation
    ) {
    /**
     * Creates an InvalidDynamicValueError based off an explicit property.
     */
    constructor(actualLocation: KProperty1<*, *>, sourceValue: String, conceptMapName: String) : this(
        LocationContext(actualLocation),
        sourceValue,
        conceptMapName
    )
}
