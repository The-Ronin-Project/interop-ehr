package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * Set of metadata that provides technical and workflow context to the resource.
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/resource.html#Meta)
 */
data class Meta(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val versionId: Id? = null,
    val lastUpdated: Instant? = null,
    val source: Uri? = null,
    val profile: List<Canonical> = listOf(),
    val security: List<Coding> = listOf(),
    val tag: List<Coding> = listOf()
) : Element
