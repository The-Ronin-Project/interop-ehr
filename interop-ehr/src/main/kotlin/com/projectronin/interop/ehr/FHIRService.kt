package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.mergeBundles
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant

interface FHIRService<T : Resource<T>> {
    val fhirResourceType: Class<T>
    fun getByID(tenant: Tenant, resourceFHIRId: String): T
    fun getByIDs(tenant: Tenant, resourceFHIRIds: List<String>): Map<String, T>
}

/**
 * Base implementation of the [FHIRService] providing some helper functionality that may be useful acorss EHR vendors
 */
abstract class BaseFHIRService<T : Resource<T>> : FHIRService<T> {
    abstract val standardParameters: Map<String, Any>

    /**
     * Standardizes the [parameters], including any standard parameters that have not already been included and returning the combined map.
     */
    protected fun standardizeParameters(parameters: Map<String, Any?>): Map<String, Any?> {
        val hasIds = parameters["_id"] != null
        val parametersToAdd = standardParameters.mapNotNull {
            if (parameters.containsKey(it.key)) {
                null
            } else if (hasIds && it.key == "_count") {
                null
            } else {
                it.toPair()
            }
        }
        return parameters + parametersToAdd
    }

    /**
     * Merges the [responses] into a single [Bundle].
     */
    protected fun mergeResponses(
        responses: List<Bundle>
    ): Bundle {
        var bundle = responses.first()
        responses.subList(1, responses.size).forEach { bundle = mergeBundles(bundle, it) }
        return bundle
    }
}
