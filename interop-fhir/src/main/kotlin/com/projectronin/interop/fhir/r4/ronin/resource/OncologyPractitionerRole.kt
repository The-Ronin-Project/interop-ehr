package com.projectronin.interop.fhir.r4.ronin.resource

import com.fasterxml.jackson.annotation.JsonTypeName
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
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.base.BasePractitionerRole

/**
 * Project Ronin definition of an Oncology PractitionerRole.
 *
 * See [Project Ronin Profile Spec](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-practitionerrole.html)
 */
@JsonTypeName("PractitionerRole")
data class OncologyPractitionerRole(
    override val id: Id? = null,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    override val text: Narrative? = null,
    override val contained: List<ContainedResource> = listOf(),
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    override val identifier: List<Identifier>,
    override val active: Boolean? = null,
    override val period: Period? = null,
    override val practitioner: Reference,
    override val organization: Reference,
    override val code: List<CodeableConcept> = listOf(),
    override val specialty: List<CodeableConcept> = listOf(),
    override val location: List<Reference> = listOf(),
    override val healthcareService: List<Reference> = listOf(),
    override val telecom: List<ContactPoint> = listOf(),
    override val availableTime: List<AvailableTime> = listOf(),
    override val notAvailable: List<NotAvailable> = listOf(),
    override val availabilityExceptions: String? = null,
    override val endpoint: List<Reference> = listOf()
) : RoninDomainResource, BasePractitionerRole() {
    init {
        validate()

        requireTenantIdentifier(identifier)

        require(telecom.all { it.system != null && it.value != null }) { "All telecoms must have a system and value" }
    }
}
