package com.projectronin.interop.fhir.ronin.normalization.dependson

import com.projectronin.interop.fhir.r4.resource.ConceptMapDependsOn
import com.projectronin.interop.fhir.r4.resource.Resource
import kotlin.reflect.KClass

/**
 * Evaluator capable of determining if a resource of type [T] meets a concept map's dependsOn definition.
 */
interface DependsOnEvaluator<T : Resource<T>> {
    val resourceType: KClass<T>

    /**
     * Returns true if the [resource] meets the condition of the [dependsOn].
     */
    fun meetsDependsOn(
        resource: T,
        dependsOn: List<ConceptMapDependsOn>,
    ): Boolean
}
