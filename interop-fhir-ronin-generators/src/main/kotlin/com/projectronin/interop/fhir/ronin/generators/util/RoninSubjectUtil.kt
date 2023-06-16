package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.Reference

fun generateSubject(reference: Reference, possibleReference: Reference): Reference {
    return reference.type?.let { reference } ?: possibleReference
}
