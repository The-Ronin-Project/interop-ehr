package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.datatype.primitive.Id

/**
 * Common FHIR implementation of the [VendorIdentifier] returning a FHIR id.
 */
data class IdVendorIdentifier(override val identifier: Id) : VendorIdentifier<Id>
