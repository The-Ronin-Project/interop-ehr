package com.projectronin.interop.ehr.outputs

import com.projectronin.interop.fhir.r4.resource.Patient

data class GetFHIRIDResponse(val fhirID: String, val newPatientObject: Patient? = null)
