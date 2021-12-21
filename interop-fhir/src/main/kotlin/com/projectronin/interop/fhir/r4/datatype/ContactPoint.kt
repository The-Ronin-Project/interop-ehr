package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse

/**
 * Details for all kinds of technology-mediated contact points for a person or organization, including telephone, email, etc.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#ContactPoint)
 */
data class ContactPoint(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val system: ContactPointSystem? = null,
    val value: String? = null,
    val use: ContactPointUse? = null,
    val rank: PositiveInt? = null,
    val period: Period? = null
) : Element
