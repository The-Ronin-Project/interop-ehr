package com.projectronin.interop.fhir.r4.resource

import com.fasterxml.jackson.annotation.JsonTypeName
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Qualification
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.base.BasePractitioner
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender

/**
 * A person who is directly or indirectly involved in the provisioning of healthcare.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/practitioner.html)
 */
@JsonTypeName("Practitioner")
data class Practitioner(
    override val id: Id? = null,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    override val text: Narrative? = null,
    override val contained: List<ContainedResource> = listOf(),
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    override val identifier: List<Identifier> = listOf(),
    override val active: Boolean? = null,
    override val name: List<HumanName> = listOf(),
    override val telecom: List<ContactPoint> = listOf(),
    override val address: List<Address> = listOf(),
    override val gender: AdministrativeGender? = null,
    override val birthDate: Date? = null,
    override val photo: List<Attachment> = listOf(),
    override val qualification: List<Qualification> = listOf(),
    override val communication: List<CodeableConcept> = listOf()
) : DomainResource, BasePractitioner() {
    init {
        validate()
    }
}
