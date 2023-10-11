package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines services supporting resolution and identification of appropriate Identifiers for a given tenant.
 */
interface IdentifierService {
    /**
     * Determines the appropriate [Identifier] for a practitioner for this [tenant]. [identifiers] should be the List of
     * all identifiers returned from an API containing identifiers referencing a Practitioner.
     */
    fun getPractitionerIdentifier(tenant: Tenant, identifiers: List<Identifier>): Identifier

    /**
     * Determines the appropriate [Identifier] for a patient for this [tenant]. [identifiers] should be the List of
     * all identifiers returned from an API containing identifiers referencing a Patient.
     */
    fun getPatientIdentifier(tenant: Tenant, identifiers: List<Identifier>): Identifier

    /**
     * Determines the appropriate [Identifier] for a [tenant] based on the provided [identifiers]. The [identifiers]
     * represents all the FHIR identifiers associated to a Practitioner.
     */
    fun getPractitionerProviderIdentifier(tenant: Tenant, identifiers: FHIRIdentifiers): Identifier

    /**
     * Determines the appropriate [Identifier] for a [tenant] based on the provided [identifiers]. The [identifiers]
     * represents all the FHIR identifiers associated to a Practitioner.
     */
    fun getPractitionerUserIdentifier(tenant: Tenant, identifiers: FHIRIdentifiers): Identifier

    /**
     * Determines the appropriate [Identifier] to use as a Ronin MRN for Patient based on the provided [identifiers] and [tenant].
     * The [identifiers] should be the List of all identifiers returned from an API for a patient.
     */
    fun getMRNIdentifier(tenant: Tenant, identifiers: List<Identifier>): Identifier

    /**
     * Determines the appropriate [Identifier] for a Location for this [tenant].
     */
    fun getLocationIdentifier(tenant: Tenant, identifiers: List<Identifier>): Identifier

    /**
     * Determines the Order System for the [tenant] given a list of identifiers.
     */
    fun getOrderIdentifier(tenant: Tenant, identifiers: List<Identifier>): Identifier

    /**
     * Determines the encounter identifier for the [tenant] given a list of identifiers.
     */
    fun getEncounterIdentifier(tenant: Tenant, identifiers: List<Identifier>): Identifier
}
