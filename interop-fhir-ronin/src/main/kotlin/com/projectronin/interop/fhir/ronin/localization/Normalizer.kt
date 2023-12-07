package com.projectronin.interop.fhir.ronin.localization

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.ronin.code.normalizeCoding
import com.projectronin.interop.fhir.ronin.code.normalizeIdentifier
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.validate.Validatable
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class Normalizer : BaseGenericTransformer() {
    private val logger = KotlinLogging.logger { }

    override val ignoredFieldNames: Set<String> = setOf("contained")

    /**
     * Normalizes the [element] for the [tenant]
     */
    fun <T : Any> normalize(
        element: T,
        tenant: Tenant,
    ): T {
        val normalizedValues = getTransformedValues(element, tenant)
        return copy(element, normalizedValues)
    }

    override fun transformType(
        element: Any,
        tenant: Tenant,
    ): TransformResult {
        return when (element) {
            is Coding -> TransformResult(normalizeCoding(element, tenant))
            is Identifier -> TransformResult(normalizeIdentifier(element, tenant))
            is CodeableConcept -> TransformResult(normalizeCodeableConcept(element, tenant))
            is Extension -> normalizeExtension(element, tenant)
            is Validatable<*> -> TransformResult(transformOrNull(element, tenant))
            else -> TransformResult(null)
        }
    }

    private fun normalizeExtension(
        extension: Extension,
        tenant: Tenant,
    ): TransformResult {
        val normalizedExtension = transformOrNull(extension, tenant) ?: extension
        return if ((RoninExtension.values().find { it.value == normalizedExtension.url?.value } != null) ||
            (normalizedExtension.url != null && (normalizedExtension.value != null || normalizedExtension.extension.isNotEmpty()))
        ) {
            TransformResult(normalizedExtension)
        } else {
            logger.info { "Extension filtered out: $extension" }
            TransformResult(extension, true)
        }
    }

    /**
     * Normalizes the [coding] for the [tenant].
     */
    private fun normalizeCoding(
        coding: Coding,
        tenant: Tenant,
    ): Coding? {
        val nonNormalizedCoding = transformOrNull(coding, tenant)
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
    private fun normalizeIdentifier(
        identifier: Identifier,
        tenant: Tenant,
    ): Identifier? {
        val nonNormalizedIdentifier = transformOrNull(identifier, tenant)
        val normalizedSystem = identifier.system?.normalizeIdentifier()
        return if (normalizedSystem == identifier.system) {
            nonNormalizedIdentifier
        } else {
            (nonNormalizedIdentifier ?: identifier).copy(system = normalizedSystem)
        }
    }

    /**
     * Normalizes the [codeableConcept] for the [tenant].
     */
    private fun normalizeCodeableConcept(
        codeableConcept: CodeableConcept,
        tenant: Tenant,
    ): CodeableConcept {
        val nonNormalizedCodeableConcept = transformOrNull(codeableConcept, tenant) ?: codeableConcept

        // If text is populated on the codeable concept already, return as is.
        if (codeableConcept.text?.value?.isNotEmpty() == true) {
            return nonNormalizedCodeableConcept
        }

        // When text isn't populated, pull from the single coding, or the single user selected coding
        val selectedCoding =
            codeableConcept.coding.singleOrNull { it.userSelected?.value == true }
                ?: codeableConcept.coding.singleOrNull()
        if (selectedCoding != null && selectedCoding.display?.value?.isNotEmpty() == true) {
            return nonNormalizedCodeableConcept.copy(text = selectedCoding.display)
        }

        // Otherwise make no changes
        return nonNormalizedCodeableConcept
    }
}
