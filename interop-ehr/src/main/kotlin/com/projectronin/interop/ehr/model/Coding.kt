package com.projectronin.interop.ehr.model

/**
 * A reference to a code defined by a terminology system
 */
interface Coding : EHRElement {
    /**
     * Identity of the terminology system.
     */
    val system: String?

    /**
     * Version of the system - if relevant.
     */
    val version: String?

    /**
     * Symbol in syntax defined by the system.
     */
    val code: String?

    /**
     * Representation defined by the system.
     */
    val display: String?

    /**
     * If this coding was chosen directly by the user.
     */
    val userSelected: Boolean?
}
