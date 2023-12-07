package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import java.util.UUID

/**
 * Returns the input [udpId] if valid for the [tenantId]. Otherwise, prefixes
 * [tenantId]- to a generated string value.
 */
fun generateUdpId(
    udpId: Id?,
    tenantId: String,
): Id {
    return udpId?.value?.let {
        if (it.startsWith("$tenantId-")) udpId else udpId.copy(value = "$tenantId-$it")
    } ?: Id(udpIdValue(tenantId, fhirIdValue()))
}

/**
 * If the [tenantId]- prefix is already on the [id] the [id] is returned. Else
 * prefixes [tenantId] to [id] as [tenantId]-[id] and returns a UDP string.
 * If [id] is empty or null, the [id] portion of the return string is generated.
 */
fun udpIdValue(
    tenantId: String,
    id: String? = null,
): String {
    return when {
        id.isNullOrEmpty() -> "$tenantId-${fhirIdValue()}"
        id.startsWith("$tenantId-") -> id
        else -> "$tenantId-$id"
    }
}

/**
 * If [id] is empty or null, randomly generate a string that works as a FHIR id.
 * If [id] is invalid for FHIR, throw IllegalArgumentException.
 * If [id] has a tenant prefix, strip off the prefix and return the FHIR id.
 */
fun fhirIdValue(
    id: String? = null,
    tenantId: String? = null,
): String {
    return when {
        id.isNullOrEmpty() -> UUID.randomUUID().toString()
        !id.matches(Regex("""[A-Za-z0-9\-\.]+""")) ->
            throw IllegalArgumentException("$id is invalid as a FHIR id")
        tenantId.isNullOrEmpty() -> id
        id.startsWith("$tenantId-") -> id.substringAfter("$tenantId-")
        else -> id
    }
}
