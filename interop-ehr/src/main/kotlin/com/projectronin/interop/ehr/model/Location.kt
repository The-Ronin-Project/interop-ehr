package com.projectronin.interop.ehr.model

import com.projectronin.interop.fhir.r4.valueset.LocationMode

/**
 * Representation of a Location.
 */
interface Location : EHRResource {
    /**
     * Logical ID of this [Location].
     */
    val id: String

    /**
     * Unique code or number identifying the [Location] to its users.
     */
    val identifier: List<Identifier>

    /**
     * The name of the [Location].
     */
    val name: String?

    /**
     * The mode of the [Location]. Include values defined in [http://hl7.org/fhir/ValueSet/location-mode].
     */
    val mode: LocationMode?

    /**
     * The physical [Location].
     */
    val address: Address?

    /**
     * [Location] contact information.
     */
    val telecom: List<ContactPoint>

    /**
     * Organization responsible for provisioning and upkeep of this [Location].
     */
    val managingOrganization: Reference?

    /**
     * Another [Location] this one is physically a part of.
     */
    val partOf: Reference?
}
