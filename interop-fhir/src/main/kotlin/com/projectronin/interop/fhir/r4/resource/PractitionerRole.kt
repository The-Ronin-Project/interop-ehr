package com.projectronin.interop.fhir.r4.resource

import com.projectronin.interop.fhir.r4.datatype.AvailableTime
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.NotAvailable
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * A specific set of Roles/Locations/specialties/services that a practitioner may perform at an organization for a period of time.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/practitionerrole.html)
 */
data class PractitionerRole(
    override val id: Id? = null,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    override val text: Narrative? = null,
    override val contained: List<ContainedResource> = listOf(),
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val identifier: List<Identifier> = listOf(),
    val active: Boolean? = null,
    val period: Period? = null,
    val practitioner: Reference? = null,
    val organization: Reference? = null,
    val code: List<CodeableConcept> = listOf(),
    val specialty: List<CodeableConcept> = listOf(),
    val location: List<Reference> = listOf(),
    val healthcareService: List<Reference> = listOf(),
    val telecom: List<ContactPoint> = listOf(),
    val availableTime: List<AvailableTime> = listOf(),
    val notAvailable: List<NotAvailable> = listOf(),
    val availabilityExceptions: String? = null,
    val endpoint: List<Reference> = listOf()
) : DomainResource
