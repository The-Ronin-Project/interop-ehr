package com.projectronin.interop.fhir.ronin.normalization.dependson

import com.projectronin.interop.fhir.r4.resource.Observation
import org.springframework.stereotype.Component

/**
 * [DependsOnEvaluator] for [Observation]s.
 */
@Component
class ObservationDependsOnEvaluator : BaseDependsOnEvaluator<Observation>(Observation::class) {
    override fun meetsDependsOn(resource: Observation, normalizedProperty: String, dependsOnValue: String): Boolean {
        return when (normalizedProperty) {
            "observation.code.text" -> meetsCodeText(resource, dependsOnValue)
            else -> false
        }
    }

    private fun meetsCodeText(resource: Observation, value: String?): Boolean {
        return resource.code?.text?.value == value
    }
}
