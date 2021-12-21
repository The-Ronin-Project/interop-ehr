package com.projectronin.interop.fhir.r4.datatype

/**
 * A relationship between two Quantity values expressed as a numerator and a denominator. The Ratio datatype should only
 * be used to express a relationship of two numbers if the relationship cannot be suitably expressed using a Quantity
 * and a common unit. Where the denominator value is known to be fixed to "1", Quantity should be used instead of Ratio.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#Ratio)
 */
data class Ratio(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val numerator: Quantity? = null,
    val denominator: Quantity? = null
) : Element {
    init {
        numerator?.let {
            require(denominator != null) { "denominator required when numerator present" }
        }
        denominator?.let {
            require(numerator != null) { "numerator required when denominator present" }
        }

        // We've already checked numerator and denominator, so we either have both or we have neither.
        require(numerator != null || extension.isNotEmpty()) { "extension required if no numerator and denominator" }
    }
}
