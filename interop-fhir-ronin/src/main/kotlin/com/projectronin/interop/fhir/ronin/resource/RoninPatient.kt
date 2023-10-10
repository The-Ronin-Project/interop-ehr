package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.ehr.factory.EHRFactory
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
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.NameUse
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.element.RoninContactPoint
import com.projectronin.interop.fhir.ronin.hasDataAbsentReason
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.toFhirIdentifier
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.ronin.util.dataAbsentReasonExtension
import com.projectronin.interop.fhir.ronin.util.dataAuthorityIdentifier
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and Transformer for the Ronin Patient profile.
 */
@Component
class RoninPatient(
    private val ehrFactory: EHRFactory,
    private val contactPoint: RoninContactPoint,
    normalizer: Normalizer,
    localizer: Localizer
) : USCoreBasedProfile<Patient>(R4PatientValidator, RoninProfile.PATIENT.value, normalizer, localizer) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 3

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
    private val invalidBirthDateError = FHIRError(
        code = "RONIN_PAT_004",
        severity = ValidationIssueSeverity.ERROR,
        description = "Birth date is invalid",
        location = LocationContext(Patient::birthDate)
    )
    private val invalidOfficialNameError = FHIRError(
        code = "RONIN_PAT_005",
        severity = ValidationIssueSeverity.ERROR,
        description = "A name for official use must be present",
        location = LocationContext(Patient::name)
    )
    private val requiredIdentifierSystemValueError = FHIRError(
        code = "RONIN_PAT_006",
        severity = ValidationIssueSeverity.ERROR,
        description = "Identifier system or data absent reason is required",
        location = LocationContext(Identifier::system)
    )

    override fun validateRonin(element: Patient, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            val mrnIdentifier = element.identifier.find { it.system == CodeSystem.RONIN_MRN.uri }
            checkNotNull(mrnIdentifier, requiredMrnIdentifierError, parentContext)

            ifNotNull(mrnIdentifier) {
                mrnIdentifier.type?.let { type ->
                    checkTrue(type == CodeableConcepts.RONIN_MRN, wrongMrnIdentifierTypeError, parentContext)
                }

                checkNotNull(mrnIdentifier.value, requiredMRNIdentifierValueError, parentContext)
            }

            element.identifier.forEachIndexed { index, identifier ->
                val context = parentContext.append(LocationContext("", "identifier[$index]"))
                checkTrue(
                    identifier.system?.value != null || identifier.system.hasDataAbsentReason(),
                    requiredIdentifierSystemValueError,
                    context
                )
            }

            checkNotNull(element.birthDate, requiredBirthDateError, parentContext)
            element.birthDate?.value?.let {
                checkTrue(it.length == 10, invalidBirthDateError, parentContext)
            }

            val nameList = element.name.find { it.use?.value == NameUse.OFFICIAL.code }
            checkNotNull(nameList, invalidOfficialNameError, parentContext)

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
                // system is checked by Ronin
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
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<Patient>?, Validation> {
        val maritalStatus = normalized.maritalStatus ?: CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.NULL_FLAVOR.uri,
                    code = Code("NI"),
                    display = FHIRString("NoInformation")
                )
            )
        )
        val gender = normalized.gender.takeIf { !it.hasDataAbsentReason() } ?: Code(
            AdministrativeGender.UNKNOWN.code,
            normalized.gender!!.id,
            normalized.gender!!.extension
        )
        val validation = Validation()

        val telecoms =
            contactPoint.transform(
                normalized.telecom,
                normalized,
                tenant,
                parentContext,
                validation,
                forceCacheReloadTS
            ).let {
                validation.merge(it.second)
                it.first
            }

        if (telecoms.size != normalized.telecom.size) {
            logger.info { "${normalized.telecom.size - telecoms.size} telecoms removed from Patient ${normalized.id?.value} due to failed transformations" }
        }

        val normalizedIdentifiers = normalizeIdentifierSystems(normalized.identifier)

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            gender = gender,
            identifier = normalizedIdentifiers + tenant.toFhirIdentifier() + getRoninIdentifiers(
                normalized,
                tenant
            ) + dataAuthorityIdentifier,
            maritalStatus = maritalStatus,
            telecom = telecoms
        )
        return Pair(TransformResponse(transformed), validation)
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
            val existingMRN =
                ehrFactory.getVendorFactory(tenant).identifierService.getMRNIdentifier(tenant, patient.identifier)
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

    private fun normalizeIdentifierSystems(identifiers: List<Identifier>): List<Identifier> {
        return identifiers.map {
            if (it.system?.value == null) {
                updateIdentifierWithDAR(it)
            } else {
                it
            }
        }
    }

    private fun updateIdentifierWithDAR(identifier: Identifier): Identifier {
        return identifier.copy(system = Uri(value = null, extension = dataAbsentReasonExtension))
    }
}
