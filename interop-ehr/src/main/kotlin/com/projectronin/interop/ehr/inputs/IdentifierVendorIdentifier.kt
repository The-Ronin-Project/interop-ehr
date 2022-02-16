package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.datatype.Identifier

/**
 * Common implementation of the [VendorIdentifier] returning a FHIR [identifier].
 */
data class IdentifierVendorIdentifier(override val identifier: Identifier) : VendorIdentifier<Identifier>
