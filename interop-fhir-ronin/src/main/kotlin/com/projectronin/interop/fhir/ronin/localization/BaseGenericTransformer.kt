package com.projectronin.interop.fhir.ronin.localization

import com.projectronin.interop.tenant.config.model.Tenant
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

/**
 * Transformer capable of processing across any type of object. This class should be extended to add specific transformation capabilities.
 */
abstract class BaseGenericTransformer {
    protected abstract val ignoredFieldNames: Set<String>

    /**
     * Transforms a specific [element] for the [tenant]. If unable to transform, null be returned.
     */
    protected abstract fun transformType(element: Any, tenant: Tenant): TransformResult

    /**
     * Transforms the [element] for [tenant] if transformation is needed or returns null if it's already transformed.
     */
    protected fun <T : Any> transformOrNull(element: T, tenant: Tenant): T? {
        val transformedValues = getTransformedValues(element, tenant)
        return if (transformedValues.isEmpty()) null else copy(element, transformedValues)
    }

    /**
     * Gets the map of transformed values for the [element] with respect to the [tenant]. Only values that were transformed will be returned in the response.
     */
    protected fun <T : Any> getTransformedValues(element: T, tenant: Tenant): Map<String, Any> {
        return element.javaClass.kotlin.memberProperties.mapNotNull { property ->
            if (property.visibility != KVisibility.PRIVATE) {
                val value = property.get(element)
                val propertyName = property.name

                value?.let {
                    val transformedValue = if (propertyName in ignoredFieldNames) {
                        null
                    } else if (value is List<*>) {
                        val collectionType = property.returnType.arguments.first().type!!.jvmErasure
                        transformList(value, collectionType, tenant)
                    } else {
                        transformType(value, tenant).element
                    }
                    transformedValue?.let { propertyName to transformedValue }
                }
            } else {
                null
            }
        }.toMap()
    }

    /**
     * Transforms a specific [collection] of [collectionType] for the [tenant]. If an individual element cannot be transformed, the original value will be included in the response. If no values were transformed, null will be returned.
     */
    private fun transformList(
        collection: List<*>,
        collectionType: KClass<*>,
        tenant: Tenant
    ): List<*>? {
        val originalAndLocalizedItems = collection.filter { it != null && it::class == collectionType }.map {
            it to transformType(it!!, tenant)
        }.filter { !it.second.removeFromElement }
        return if (collection.isNotEmpty() && originalAndLocalizedItems.isEmpty()) {
            originalAndLocalizedItems
        } else if (originalAndLocalizedItems.all { it.second.element == null }) {
            null
        } else {
            originalAndLocalizedItems.map {
                it.second.element ?: it.first
            }
        }
    }

    /**
     * Copies the [element] with the [transformedValues] updated.
     */
    protected fun <T : Any> copy(element: T, transformedValues: Map<String, Any>): T {
        if (transformedValues.isEmpty()) {
            return element
        }

        val copyFunction = element::class.memberFunctions.firstOrNull { it.name == "copy" } as? KFunction<T>
            ?: throw IllegalStateException()
        val parameters = copyFunction.parameters.filter { it.name != null }.associateBy { it.name }

        val copyArguments =
            mapOf(copyFunction.instanceParameter!! to element) + transformedValues.mapKeys { parameters[it.key]!! }
        return copyFunction.callBy(copyArguments)
    }
}

data class TransformResult(
    val element: Any?,
    val removeFromElement: Boolean = false
)
