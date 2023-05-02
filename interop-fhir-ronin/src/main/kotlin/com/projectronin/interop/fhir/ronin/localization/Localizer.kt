package com.projectronin.interop.fhir.ronin.localization

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.localizeReference
import com.projectronin.interop.fhir.validate.Validatable
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Localizer is capable of localizing an element. This localization may include system normalization or tenant-level
 * data segregation.
 */
@Component
class Localizer : BaseGenericTransformer() {

    /**
     * Localizes the [element] for the [tenant]
     */
    fun <T : Any> localize(element: T, tenant: Tenant): T {
        val localizedValues = getTransformedValues(element, tenant)
        return copy(element, localizedValues)
    }

    override fun transformType(element: Any, parameterName: String, tenant: Tenant): Any? {
        return when (element) {
            is DynamicValue<*> -> localizeDynamicValue(element as DynamicValue<Any>, parameterName, tenant)
            is Id -> localizeId(element, parameterName, tenant)
            is Reference -> localizeReference(element, parameterName, tenant)
            is Validatable<*> -> transformOrNull(element, parameterName, tenant)
            else -> null
        }
    }

    /**
     * Localizes the [dynamicValue] for the [tenant].
     */
    private fun localizeDynamicValue(
        dynamicValue: DynamicValue<Any>,
        parameterName: String,
        tenant: Tenant
    ): DynamicValue<Any>? {
        val localizedValue = transformType(dynamicValue.value, parameterName, tenant)
        return localizedValue?.let { DynamicValue(dynamicValue.type, it) }
    }

    /**
     * Localizes the [id] for the [tenant].
     */
    private fun localizeId(id: Id, parameterName: String, tenant: Tenant): Id? =
        if (parameterName == "versionId") null else Id(id.value?.localize(tenant), id.id, id.extension)

    /**
     * Localizes the [reference] for the [tenant].
     */
    private fun localizeReference(reference: Reference, parameterName: String, tenant: Tenant): Reference? {
        val nonReferenceLocalized = transformOrNull(reference, parameterName, tenant) ?: reference
        return nonReferenceLocalized.localizeReference(tenant)
    }
}
