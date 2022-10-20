package com.projectronin.interop.fhir.ronin.localization

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.ronin.code.normalizeCoding
import com.projectronin.interop.fhir.ronin.code.normalizeIdentifier
import com.projectronin.interop.fhir.validate.Validatable
import com.projectronin.interop.tenant.config.model.Tenant

object Normalizer : BaseGenericTransformer() {
    /**
     * Normalizes the [element] for the [tenant]
     */
    fun <T : Any> normalize(element: T, tenant: Tenant): T {
        val normalizedValues = getTransformedValues(element, tenant)
        return copy(element, normalizedValues)
    }

    override fun transformType(element: Any, parameterName: String, tenant: Tenant): Any? {
        return when (element) {
            is Coding -> normalizeCoding(element, parameterName, tenant)
            is Identifier -> normalizeIdentifier(element, parameterName, tenant)
            is Validatable<*> -> transformOrNull(element, parameterName, tenant)
            else -> null
        }
    }

    /**
     * Normalizes the [coding] for the [tenant].
     */
    private fun normalizeCoding(coding: Coding, parameterName: String, tenant: Tenant): Coding? {
        val nonNormalizedCoding = transformOrNull(coding, parameterName, tenant)
        val normalizedSystem = coding.system?.normalizeCoding()
        return if (normalizedSystem == coding.system) {
            nonNormalizedCoding
        } else {
            (nonNormalizedCoding ?: coding).copy(system = normalizedSystem)
        }
    }

    /**
     * Normalizes the [identifier] for the [tenant].
     */
    private fun normalizeIdentifier(identifier: Identifier, parameterName: String, tenant: Tenant): Identifier? {
        val nonNormalizedIdentifier = transformOrNull(identifier, parameterName, tenant)
        val normalizedSystem = identifier.system?.normalizeIdentifier()
        return if (normalizedSystem == identifier.system) {
            nonNormalizedIdentifier
        } else {
            (nonNormalizedIdentifier ?: identifier).copy(system = normalizedSystem)
        }
    }
}
