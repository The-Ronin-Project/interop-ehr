package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.valueset.SearchEntryMode

/**
 * Search related information
 *
 * See [FHIR Spec](http://www.hl7.org/fhir/bundle-definitions.html#Bundle.entry.search)
 */
data class BundleSearch(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val mode: SearchEntryMode? = null,
    val score: Decimal? = null
) : BackboneElement
