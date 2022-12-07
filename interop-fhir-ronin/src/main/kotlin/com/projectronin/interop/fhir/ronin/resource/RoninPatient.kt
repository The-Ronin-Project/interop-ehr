package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.validate.resource.R4PatientValidator
import com.projectronin.interop.fhir.ronin.conceptmap.ConceptMapClient
import com.projectronin.interop.fhir.ronin.element.RoninContactPoint
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
class RoninPatient private constructor(
    private val identifierService: IdentifierService,
    private val conceptMapClient: ConceptMapClient,
) : USCoreBasedProfile<Patient>(R4PatientValidator, RoninProfile.PATIENT.value) {
    companion object {
        /**
         * Creates a RoninPatient with the supplied [identifierService] and [conceptMapClient].
         */
        fun create(
            identifierService: IdentifierService,
            conceptMapClient: ConceptMapClient
        ): RoninPatient = RoninPatient(identifierService, conceptMapClient)
    }

    private val contactPoint = RoninContactPoint(conceptMapClient)

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
    private val requiredMRNIdentifierValueError = FHIRError(
        code = "RONIN_PAT_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "MRN identifier value is required",
        location = LocationContext(Patient::identifier)
    )

    override fun validateRonin(element: Patient, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)

            val mrnIdentifier = element.identifier.find { it.system == CodeSystem.RONIN_MRN.uri }
            checkNotNull(mrnIdentifier, requiredMrnIdentifierError, parentContext)

            ifNotNull(mrnIdentifier) {
                mrnIdentifier.type?.let { type ->
                    checkTrue(type == CodeableConcepts.RONIN_MRN, wrongMrnIdentifierTypeError, parentContext)
                }

                checkNotNull(mrnIdentifier.value, requiredMRNIdentifierValueError, parentContext)
            }

            checkNotNull(element.birthDate, requiredBirthDateError, parentContext)

            // the gender required value set inherits validation from R4

            if (element.telecom.isNotEmpty()) {
                contactPoint.validateRonin(element.telecom, parentContext, validation)
            }
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

    override fun validateUSCore(element: Patient, parentContext: LocationContext, validation: Validation) {
        validation.apply {

            checkTrue(element.name.isNotEmpty(), requiredNameError, parentContext)
            // Each human name should have a first or last name populated, otherwise a data absent reason.
            element.name.forEachIndexed { index, humanName ->
                checkTrue(
                    ((humanName.family != null) or (humanName.given.isNotEmpty())) xor humanName.hasDataAbsentReason(),
                    requiredFamilyOrGivenError,
                    parentContext.append(LocationContext("Patient", "name[$index]"))
                )
            }

            checkNotNull(element.gender, requiredGenderError, parentContext)

            // A patient identifier is also required, but Ronin has already checked for a MRN, so we will bypass the checks here.
            element.identifier.forEachIndexed { index, identifier ->
                val currentContext = parentContext.append(LocationContext("Patient", "identifier[$index]"))
                checkNotNull(identifier.system, requiredIdentifierSystemError, currentContext)
                checkNotNull(identifier.value, requiredIdentifierValueError, currentContext)
            }

            if (element.telecom.isNotEmpty()) {
                contactPoint.validateUSCore(element.telecom, parentContext, validation)
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

        val maritalStatus = normalized.maritalStatus ?: CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/v3-NullFlavor"),
                    code = Code("NI"),
                    display = FHIRString("NoInformation")
                )
            )
        )

        val validation = Validation()
        val contactPointTransformed = if (normalized.telecom.isNotEmpty()) {
            contactPoint.transform(normalized.telecom, tenant, LocationContext(Patient::class), validation)
        } else Pair(normalized.telecom, validation)

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + tenant.toFhirIdentifier() + getRoninIdentifiers(normalized, tenant),
            maritalStatus = maritalStatus,
            telecom = contactPointTransformed.first ?: emptyList(),
        )
        return Pair(transformed, contactPointTransformed.second)
    }

    /**
     * Create a FHIR [Identifier] from the FHIR Patient.id. Add that and the MRN [Identifier] to a List and return it.
     * This function is NOT private in this class, because code in other repos besides interop-ehr use it.
     */
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
                    system = CodeSystem.RONIN_MRN.uri,
                    type = CodeableConcepts.RONIN_MRN
                )
            )
        } catch (e: Exception) {
            // We will handle this during validation.
        }
        return roninIdentifiers
    }
}
