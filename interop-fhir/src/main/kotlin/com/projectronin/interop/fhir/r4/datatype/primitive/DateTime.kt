package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * A date, date-time or partial date (e.g. just year or year + month) as used in human communication. The format is
 * YYYY, YYYY-MM, YYYY-MM-DD or YYYY-MM-DDThh:mm:ss+zz:zz, e.g. 2018, 1973-06, 1905-08-23, 2015-02-07T13:28:17-05:00 or
 * 2017-01-01T00:00:00.000Z. If hours and minutes are specified, a time zone SHALL be populated. Seconds must be provided
 * due to schema type constraints but may be zero-filled and may be ignored at receiver discretion. Dates SHALL be valid
 * dates. The time "24:00" is not allowed. Leap Seconds are allowed.
 *
 * See [FHIR documentation](http://hl7.org/fhir/R4/datatypes.html#dateTime)
 */
data class DateTime @JsonCreator constructor(override val value: String) : Primitive<String> {
    private val validRegex =
        Regex("""([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1])(T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\.[0-9]+)?(Z|(\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00)))?)?)?""")

    init {
        require(validRegex.matches(value)) { "Supplied value is not valid for a DateTime" }
    }
}
