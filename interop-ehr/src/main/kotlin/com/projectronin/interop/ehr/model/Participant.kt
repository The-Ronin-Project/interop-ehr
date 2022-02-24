package com.projectronin.interop.ehr.model

interface Participant : EHRElement {
    /**
     * Reference to the resource
     */
    val actor: List<Reference>
}
