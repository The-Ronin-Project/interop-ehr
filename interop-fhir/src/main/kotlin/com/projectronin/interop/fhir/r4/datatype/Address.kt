package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.valueset.AddressType
import com.projectronin.interop.fhir.r4.valueset.AddressUse

/**
 * An address expressed using postal conventions (as opposed to GPS or other location definition formats). This data type
 * may be used to convey addresses for use in delivering mail as well as for visiting locations which might not be valid
 * for mail delivery. There are a variety of postal address formats defined around the world.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#address)
 */
data class Address(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val use: AddressUse? = null,
    val type: AddressType? = null,
    val text: String? = null,
    val line: List<String> = listOf(),
    val city: String? = null,
    val district: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val period: Period? = null
) : Element
