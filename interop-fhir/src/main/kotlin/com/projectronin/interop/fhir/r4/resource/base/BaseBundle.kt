package com.projectronin.interop.fhir.r4.resource.base

import com.fasterxml.jackson.annotation.JsonTypeName
import com.projectronin.interop.fhir.r4.datatype.BundleLink
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Signature
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.BundleType

/**
 * Base class representing a FHIR R4 Bundle.
 *
 * See [FHIR Spec](http://www.hl7.org/fhir/bundle.html)
 * @param E The type of entry supported by this bundle
 */
@JsonTypeName("Bundle")
abstract class BaseBundle<E> {
    val resourceType: String = "Bundle"

    abstract val id: Id?
    abstract val meta: Meta?
    abstract val implicitRules: Uri?
    abstract val language: Code?
    abstract val identifier: Identifier?
    abstract val type: BundleType
    abstract val timestamp: Instant?
    abstract val total: UnsignedInt?
    abstract val link: List<BundleLink>
    abstract val entry: List<E>
    abstract val signature: Signature?

    protected fun validate() {}
}
