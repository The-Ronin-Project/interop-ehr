package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.HttpVerb

/**
 * Additional execution information (transaction/batch/history)
 *
 * See [FHIR Spec](http://www.hl7.org/fhir/bundle-definitions.html#Bundle.entry.request)
 */
data class BundleRequest(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val method: HttpVerb,
    val url: Uri,
    val ifNoneMatch: String? = null,
    val ifModifiedSince: Instant? = null,
    val ifMatch: String? = null,
    val ifNoneExist: String? = null
) : BackboneElement
