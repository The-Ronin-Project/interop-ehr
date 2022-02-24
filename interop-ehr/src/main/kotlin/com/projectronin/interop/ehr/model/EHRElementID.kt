package com.projectronin.interop.ehr.model

import com.projectronin.interop.fhir.r4.datatype.primitive.Id

interface EHRElementID {
    /**
     * The direct reference to the object
     */
    val id: Id?
}
