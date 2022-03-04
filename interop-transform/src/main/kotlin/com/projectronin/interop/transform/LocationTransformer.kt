package com.projectronin.interop.transform

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Location
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.fhir.r4.resource.Location as R4Location
/**
 * Defines a Transformer capable of converting EHR [Location]s into Ronin [OncologyLocation]s.
 */
interface LocationTransformer {
    /**
     * Transforms the EHR [location] into a Ronin [OncologyLocation] based on the [tenant]. If the transformation
     * can not be completed due to missing or incomplete information, null will be returned.
     */
    fun transformLocation(location: Location, tenant: Tenant): R4Location?

    /**
     * Transforms the [bundle] into a List of Ronin [OncologyLocation]s based on the [tenant]. Only [Location]s that
     * could be transformed successfully will be included in the response.
     */
    fun transformLocations(bundle: Bundle<Location>, tenant: Tenant): List<R4Location>
}
