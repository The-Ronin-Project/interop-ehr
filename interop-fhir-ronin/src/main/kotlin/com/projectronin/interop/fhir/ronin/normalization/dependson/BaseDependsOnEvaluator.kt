package com.projectronin.interop.fhir.ronin.normalization.dependson

import com.projectronin.interop.fhir.r4.resource.ConceptMapDependsOn
import com.projectronin.interop.fhir.r4.resource.Resource
import java.util.Locale
import kotlin.reflect.KClass

/**
 * Base [DependsOnEvaluator] to simplify implementations.
 */
abstract class BaseDependsOnEvaluator<T : Resource<T>>(override val resourceType: KClass<T>) : DependsOnEvaluator<T> {
    /**
     * Determines if the [resource] meets the [dependsOnValue] for the [normalizedProperty].
     */
    protected abstract fun meetsDependsOn(resource: T, normalizedProperty: String, dependsOnValue: String): Boolean

    override fun meetsDependsOn(resource: T, dependsOn: List<ConceptMapDependsOn>): Boolean {
        return dependsOn.all {
            val property = it.property?.value?.lowercase(Locale.getDefault())
                ?: throw IllegalStateException("Null property found for DependsOn: $it")
            val value = it.value?.value ?: throw IllegalStateException("Null value found for DependsOn: $it")
            meetsDependsOn(resource, property, value)
        }
    }
}
