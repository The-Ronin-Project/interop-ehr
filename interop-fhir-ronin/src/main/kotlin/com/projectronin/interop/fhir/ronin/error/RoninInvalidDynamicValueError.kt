package com.projectronin.interop.fhir.ronin.error

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import kotlin.reflect.KProperty1

/**
 * Error for an invalid dynamic value type that has a different set of types in a Ronin profile than in R4 or HSCore.
 * R4 reports [InvalidDynamicValueError]. [RoninInvalidDynamicValueError] can report the Ronin variation. The optional
 * [profileName] argument allows you to substitute a more informative profile name than the default "Ronin".
 */
class RoninInvalidDynamicValueError(
    actualLocation: LocationContext,
    validTypes: List<DynamicValueType>,
    profileName: String? = "Ronin"
) :
    FHIRError(
        "RONIN_INV_DYN_VAL",
        ValidationIssueSeverity.ERROR,
        "$profileName profile restricts ${actualLocation.field} to one of: ${validTypes.joinToString { it.code }}",
        actualLocation
    ) {
    /**
     * Creates a RoninInvalidDynamicValueError based off an explicit property.
     */
    constructor(actualLocation: KProperty1<*, DynamicValue<*>?>, validTypes: List<DynamicValueType>, profileName: String) : this(
        LocationContext(actualLocation),
        validTypes,
        profileName
    )
}
