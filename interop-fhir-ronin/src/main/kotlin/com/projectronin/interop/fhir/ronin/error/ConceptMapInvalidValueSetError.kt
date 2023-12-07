package com.projectronin.interop.fhir.ronin.error

import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import kotlin.reflect.KProperty1

/**
 * Defines the error for a concept map required value set failure.
 * The source value was mapped to a target value that is not in the required value set.
 */
class ConceptMapInvalidValueSetError(
    actualLocation: LocationContext,
    conceptMapName: String,
    sourceValue: String,
    targetValue: String?,
    metadata: List<ConceptMapMetadata>?,
) :
    FHIRError(
            "INV_CONMAP_VALUE_SET",
            ValidationIssueSeverity.ERROR,
            "$conceptMapName mapped '$sourceValue' to '$targetValue' which is outside of required value set",
            actualLocation,
            metadata,
        ) {
    /**
     * Creates an ConceptMapInvalidValueSetError based off an explicit property.
     */
    constructor(
        actualLocation: KProperty1<*, *>,
        sourceValue: String,
        targetValue: String,
        conceptMapName: String,
        metadata: List<ConceptMapMetadata>,
    ) : this(
        LocationContext(actualLocation),
        conceptMapName,
        sourceValue,
        targetValue,
        metadata,
    )
}
