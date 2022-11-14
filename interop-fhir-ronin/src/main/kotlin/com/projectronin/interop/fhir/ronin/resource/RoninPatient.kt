package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.validate.resource.R4PatientValidator
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
import com.projectronin.interop.fhir.ronin.hasDataAbsentReason
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.toFhirIdentifier
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Validator and Transformer for the Ronin Patient profile.
 */
class RoninPatient private constructor(private val identifierService: IdentifierService) :
    USCoreBasedProfile<Patient>(R4PatientValidator, RoninProfile.PATIENT.value) {
    companion object {
        /**
         * Creates a RoninPatient with the supplied [identifierService].
         */
        fun create(identifierService: IdentifierService): RoninPatient =
            RoninPatient(identifierService)
    }

    private val requiredBirthDateError = RequiredFieldError(Patient::birthDate)

    private val requiredMrnIdentifierError = FHIRError(
        code = "RONIN_PAT_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "MRN identifier is required",
        location = LocationContext(Patient::identifier)
    )
    private val wrongMrnIdentifierTypeError = FHIRError(
        code = "RONIN_PAT_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "MRN identifier type defined without proper CodeableConcept",
        location = LocationContext(Patient::identifier)
    )
    private val requiredTenantIdentifierValueError = FHIRError(
        code = "RONIN_PAT_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "MRN identifier value is required",
        location = LocationContext(Patient::identifier)
    )

    override fun validateRonin(element: Patient, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)

            val mrnIdentifier = element.identifier.find { it.system == RoninCodeSystem.MRN.uri }
            checkNotNull(mrnIdentifier, requiredMrnIdentifierError, parentContext)

            ifNotNull(mrnIdentifier) {
                mrnIdentifier.type?.let { type ->
                    checkTrue(type == RoninCodeableConcepts.MRN, wrongMrnIdentifierTypeError, parentContext)
                }

                checkNotNull(mrnIdentifier.value, requiredTenantIdentifierValueError, parentContext)
            }

            checkNotNull(element.birthDate, requiredBirthDateError, parentContext)

            // TODO: RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM, check Ronin IG re: extensions
            // TODO: RoninExtension.TENANT_SOURCE_TELECOM_USE, check Ronin IG re: extensions

            // the gender required value set inherits validation from R4
        }
    }

    private val requiredGenderError = RequiredFieldError(Patient::gender)

    private val requiredNameError = FHIRError(
        code = "USCORE_PAT_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "At least one name must be provided",
        location = LocationContext(Patient::name)
    )
    private val requiredFamilyOrGivenError = FHIRError(
        code = "USCORE_PAT_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Either Patient.name.given and/or Patient.name.family SHALL be present or a Data Absent Reason Extension SHALL be present.",
        location = LocationContext(Patient::name)
    )

    private val requiredIdentifierSystemError = RequiredFieldError(Identifier::system)
    private val requiredIdentifierValueError = RequiredFieldError(Identifier::value)

    private val requiredTelecomSystemError = RequiredFieldError(ContactPoint::system)
    private val requiredTelecomValueError = RequiredFieldError(ContactPoint::value)

    override fun validateUSCore(element: Patient, parentContext: LocationContext, validation: Validation) {
        validation.apply {

            checkTrue(element.name.isNotEmpty(), requiredNameError, parentContext)
            // Each human name should have a first or last name populated, otherwise a data absent reason.
            element.name.forEachIndexed { index, humanName ->
                checkTrue(
                    ((humanName.family != null) or (humanName.given.isNotEmpty())) xor humanName.hasDataAbsentReason(),
                    requiredFamilyOrGivenError, parentContext.append(LocationContext("Patient", "name[$index]"))
                )
            }

            checkNotNull(element.gender, requiredGenderError, parentContext)

            // A patient identifier is also required, but Ronin has already checked for a MRN, so we will bypass the checks here.
            element.identifier.forEachIndexed { index, identifier ->
                val currentContext = parentContext.append(LocationContext("Patient", "identifier[$index]"))
                checkNotNull(identifier.system, requiredIdentifierSystemError, currentContext)
                checkTrue(
                    identifier.value != null || identifier.valueData?.extension?.isNotEmpty() == true,
                    requiredIdentifierValueError,
                    currentContext
                )
            }

            element.telecom.forEachIndexed { index, telecom ->
                val currentContext = parentContext.append(LocationContext("Patient", "telecom[$index]"))
                checkNotNull(telecom.system, requiredTelecomSystemError, currentContext)
                checkNotNull(telecom.value, requiredTelecomValueError, currentContext)
            }

            // the Patient BackboneElements link and communication inherit validation from R4

            // these required value sets inherit validation from R4:
            // name.use, telecom.system, telecom.value, address.use, address.type, contact.gender, link.type
        }
    }

    override fun transformInternal(
        normalized: Patient,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Patient?, Validation> {
        // TODO: RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM, check Ronin IG re: extension, concept maps for telecom.status
        // TODO: RoninExtension.TENANT_SOURCE_TELECOM_USE, check Ronin IG re: extension, concept maps for telecom.use

        val maritalStatus = normalized.maritalStatus ?: CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/v3-NullFlavor"),
                    code = Code("NI"),
                    display = "NoInformation"
                )
            )
        )

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + tenant.toFhirIdentifier() +
                getRoninIdentifiers(normalized, tenant),
            maritalStatus = maritalStatus,
        )
        return Pair(transformed, Validation())
    }

    fun getRoninIdentifiers(patient: Patient, tenant: Tenant): List<Identifier> {
        val roninIdentifiers = mutableListOf<Identifier>()
        patient.id?.toFhirIdentifier()?.let {
            roninIdentifiers.add(it)
        }

        try {
            val existingMRN = identifierService.getMRNIdentifier(tenant, patient.identifier)
            roninIdentifiers.add(
                Identifier(
                    value = existingMRN.value,
                    system = RoninCodeSystem.MRN.uri,
                    type = RoninCodeableConcepts.MRN
                )
            )
        } catch (e: Exception) {
            // We will handle this during validation.
        }
        return roninIdentifiers
    }
}
