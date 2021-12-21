package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.valueset.NameUse

/**
 * A name of a human with text, parts and usage information. Names may be changed or repudiated. People may have different
 * names in different contexts. Names may be divided into parts of different type that have variable significance depending
 * on context, though the division into parts is not always significant. With personal names, the different parts might
 * or might not be imbued with some implicit meaning; various cultures associate different importance with the name parts
 * and the degree to which systems SHALL care about name parts around the world varies widely.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#HumanName)
 */
data class HumanName(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val use: NameUse? = null,
    val text: String? = null,
    val family: String? = null,
    val given: List<String> = listOf(),
    val prefix: List<String> = listOf(),
    val suffix: List<String> = listOf(),
    val period: Period? = null
) : Element
