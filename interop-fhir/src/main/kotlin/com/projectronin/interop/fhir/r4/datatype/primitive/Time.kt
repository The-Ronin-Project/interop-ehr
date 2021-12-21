package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * 	A time during the day, in the format hh:mm:ss. There is no date specified. Seconds must be provided due to schema
 * 	type constraints but may be zero-filled and may be ignored at receiver discretion. The time "24:00" SHALL NOT be used.
 * 	A time zone SHALL NOT be present. Times can be converted to a Duration since midnight.
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#time)
 */
data class Time @JsonCreator constructor(override val value: String) : Primitive<String> {
    private val validRegex =
        Regex("""([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\.[0-9]+)?""")

    init {
        require(validRegex.matches(value)) { "Supplied value is not valid for a Time" }
    }
}
