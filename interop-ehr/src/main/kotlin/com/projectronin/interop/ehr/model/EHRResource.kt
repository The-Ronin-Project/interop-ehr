package com.projectronin.interop.ehr.model

import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.enums.DataSource

/**
 * Base interface defining all EHR resources.
 */
interface EHRResource {
    /**
     * The raw data from an EHR backing this model.
     */
    val raw: String

    /**
     * The resource from an EHR backing this model. In most cases, this should be a Kotlin object based off the raw string
     * that is being adapted into this resource. The [dataSource] can be used to further limit what the expected type
     * should be if any direct processing needs to occur against this resource.
     */
    val resource: Any

    /**
     * The type of resource represented.
     */
    val resourceType: ResourceType

    /**
     * The data source of the resource. This can help define the format of the [raw] for any consumers.
     */
    val dataSource: DataSource
}
