package com.projectronin.interop.ehr.hl7.converters.datatypes

import com.projectronin.interop.fhir.r4.valueset.ContactPointUse

fun ContactPointUse.toHL7Code(): String? {
    return when (this) {
        ContactPointUse.HOME -> "PRN"
        ContactPointUse.WORK -> "WPN"
        ContactPointUse.MOBILE -> "ORN"
        ContactPointUse.OLD -> null
        ContactPointUse.TEMPORARY -> null
    }
}
