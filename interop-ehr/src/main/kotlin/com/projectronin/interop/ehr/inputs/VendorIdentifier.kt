package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.datatype.primitive.Id

/**
 * Representation of a vendor-specific identifier. Consumers should utilize [com.projectronin.interop.ehr.IdentifierService]
 * to get appropriate instances.
 */
interface VendorIdentifier<T> {
    /**
     * True if the [identifier] represents the FHIR id. If false, [identifier] represents an [com.projectronin.interop.ehr.model.Identifier].
     */
    val isFhirId: Boolean
        get() = identifier is Id

    /**
     * The identifier to use for the current vendor.
     */
    val identifier: T
}
