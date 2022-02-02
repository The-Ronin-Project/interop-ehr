package com.projectronin.interop.ehr.model.base

import com.projectronin.interop.ehr.model.EHRResource
import com.projectronin.interop.fhir.r4.resource.Bundle as R4Bundle

/**
 * Base implementation suitable for FHIR bundles.
 */
abstract class FHIRBundle<out R : EHRResource>(override val resource: R4Bundle) : JSONBundle<R, R4Bundle>(resource)
