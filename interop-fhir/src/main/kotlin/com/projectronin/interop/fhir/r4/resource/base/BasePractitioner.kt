package com.projectronin.interop.fhir.r4.resource.base

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
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender

/**
 * Base class representing a FHIR R4 Practitioner.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/practitioner.html)
 */
abstract class BasePractitioner {
    val resourceType: String = "Practitioner"

    abstract val id: Id?
    abstract val meta: Meta?
    abstract val implicitRules: Uri?
    abstract val language: Code?
    abstract val text: Narrative?
    abstract val contained: List<ContainedResource>
    abstract val extension: List<Extension>
    abstract val modifierExtension: List<Extension>
    abstract val identifier: List<Identifier>
    abstract val active: Boolean?
    abstract val name: List<HumanName>
    abstract val telecom: List<ContactPoint>
    abstract val address: List<Address>
    abstract val gender: AdministrativeGender?
    abstract val birthDate: Date?
    abstract val photo: List<Attachment>
    abstract val qualification: List<Qualification>
    abstract val communication: List<CodeableConcept>

    protected fun validate() {}
}
