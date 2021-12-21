package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt

/**
 * Data that comes from a series of measurements taken by a device, which may have upper and lower limits. The data type
 * also supports more than one dimension in the data. A SampledData provides a concise way to handle the data produced by
 * devices that sample a particular physical state at a high frequency. A typical use for this is for the output of an
 * ECG or EKG device. The data type includes a series of raw decimal values (which are mostly simple integers), along
 * with adjustments for scale and factor.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/datatypes.html#SampledData)
 */
data class SampledData(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val origin: SimpleQuantity,
    val period: Double,
    val factor: Double? = null,
    val lowerLimit: Double? = null,
    val upperLimit: Double? = null,
    val dimensions: PositiveInt,
    val data: String? = null
) : Element
