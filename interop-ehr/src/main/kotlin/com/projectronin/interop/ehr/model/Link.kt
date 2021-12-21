package com.projectronin.interop.ehr.model

import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * Representation of a FHIR link element
 */
interface Link : EHRElement {
    /**
     * Relation of the link
     */
    val relation: String

    /**
     * Url for the link
     */
    val url: Uri
}
