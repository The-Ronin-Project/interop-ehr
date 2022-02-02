package com.projectronin.interop.fhir.r4.resource.base

import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Communication
import com.projectronin.interop.fhir.r4.datatype.Contact
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.PatientLink
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender

/**
 * Base class representing a FHIR R4 Patient.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/R4/patient.html)
 */
abstract class BasePatient {
    val resourceType: String = "Patient"

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
    abstract val gender: AdministrativeGender?
    abstract val birthDate: Date?
    abstract val deceased: DynamicValue<Any>?
    abstract val address: List<Address>
    abstract val maritalStatus: CodeableConcept?
    abstract val multipleBirth: DynamicValue<Any>?
    abstract val photo: List<Attachment>
    abstract val contact: List<Contact>
    abstract val communication: List<Communication>
    abstract val generalPractitioner: List<Reference>
    abstract val managingOrganization: Reference?
    abstract val link: List<PatientLink>

    companion object {
        val acceptedDeceasedTypes = listOf(DynamicValueType.BOOLEAN, DynamicValueType.DATE_TIME)
        val acceptedMultipleBirthTypes = listOf(DynamicValueType.BOOLEAN, DynamicValueType.INTEGER)
    }

    protected fun validate() {
        deceased?.let {
            require(acceptedDeceasedTypes.contains(it.type)) { "Bad dynamic value indicating if the patient is deceased" }
        }

        multipleBirth?.let {
            require(acceptedMultipleBirthTypes.contains(it.type)) { "Bad dynamic value indicating whether the patient was part of a multiple birth" }
        }

        require(
            contact.all {
                (it.name != null) or (it.telecom.isNotEmpty()) or (it.address != null) or (it.organization != null)
            }
        ) { "[pat-1](https://www.hl7.org/fhir/R4/patient.html#invs): contact SHALL at least contain a contact's details or a reference to an organization" }
    }
}
