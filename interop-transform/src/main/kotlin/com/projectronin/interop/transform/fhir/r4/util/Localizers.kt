package com.projectronin.interop.transform.fhir.r4.util

import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.AvailableTime
import com.projectronin.interop.fhir.r4.datatype.BackboneElement
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Communication
import com.projectronin.interop.fhir.r4.datatype.Contact
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Element
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.NotAvailable
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.PatientLink
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Qualification
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.tenant.config.model.Tenant

// Localizers for Strings and String-like wrappers

/**
 * Localizes the String relative to the [tenant]
 */
fun String.localize(tenant: Tenant) = "${tenant.mnemonic}-$this"

/**
 * Localizes the Id relative to the [tenant]
 */
fun Id.localize(tenant: Tenant) = Id(value.localize(tenant))

// This regex is provided by the FHIR spec in this section: http://hl7.org/fhir/R4/references.html#literal
// and edited to remove length restrictions
private val FHIR_RESOURCE_REGEX = Regex(
    """((http|https):\/\/([A-Za-z0-9\-\\\.\:\%${"\$"}]*\/)+)?(Account|ActivityDefinition|AdverseEvent|AllergyIntolerance|Appointment|AppointmentResponse|AuditEvent|Basic|Binary|BiologicallyDerivedProduct|BodyStructure|Bundle|CapabilityStatement|CarePlan|CareTeam|CatalogEntry|ChargeItem|ChargeItemDefinition|Claim|ClaimResponse|ClinicalImpression|CodeSystem|Communication|CommunicationRequest|CompartmentDefinition|Composition|ConceptMap|Condition|Consent|Contract|Coverage|CoverageEligibilityRequest|CoverageEligibilityResponse|DetectedIssue|Device|DeviceDefinition|DeviceMetric|DeviceRequest|DeviceUseStatement|DiagnosticReport|DocumentManifest|DocumentReference|EffectEvidenceSynthesis|Encounter|Endpoint|EnrollmentRequest|EnrollmentResponse|EpisodeOfCare|EventDefinition|Evidence|EvidenceVariable|ExampleScenario|ExplanationOfBenefit|FamilyMemberHistory|Flag|Goal|GraphDefinition|Group|GuidanceResponse|HealthcareService|ImagingStudy|Immunization|ImmunizationEvaluation|ImmunizationRecommendation|ImplementationGuide|InsurancePlan|Invoice|Library|Linkage|List|Location|Measure|MeasureReport|Media|Medication|MedicationAdministration|MedicationDispense|MedicationKnowledge|MedicationRequest|MedicationStatement|MedicinalProduct|MedicinalProductAuthorization|MedicinalProductContraindication|MedicinalProductIndication|MedicinalProductIngredient|MedicinalProductInteraction|MedicinalProductManufactured|MedicinalProductPackaged|MedicinalProductPharmaceutical|MedicinalProductUndesirableEffect|MessageDefinition|MessageHeader|MolecularSequence|NamingSystem|NutritionOrder|Observation|ObservationDefinition|OperationDefinition|OperationOutcome|Organization|OrganizationAffiliation|Patient|PaymentNotice|PaymentReconciliation|Person|PlanDefinition|Practitioner|PractitionerRole|Procedure|Provenance|Questionnaire|QuestionnaireResponse|RelatedPerson|RequestGroup|ResearchDefinition|ResearchElementDefinition|ResearchStudy|ResearchSubject|RiskAssessment|RiskEvidenceSynthesis|Schedule|SearchParameter|ServiceRequest|Slot|Specimen|SpecimenDefinition|StructureDefinition|StructureMap|Subscription|Substance|SubstanceNucleicAcid|SubstancePolymer|SubstanceProtein|SubstanceReferenceInformation|SubstanceSourceMaterial|SubstanceSpecification|SupplyDelivery|SupplyRequest|Task|TerminologyCapabilities|TestReport|TestScript|ValueSet|VerificationResult|VisionPrescription)\/([A-Za-z0-9\-\.]+)(\/_history\/[A-Za-z0-9\-\.]+)?"""
)

/**
 * Localizes the [reference](http://hl7.org/fhir/R4/references.html) contained by this String relative to the [tenant].
 * If this String does not represent a reference, the original String will be returned.
 */
fun String.localizeReference(tenant: Tenant): String {
    val matchResult = FHIR_RESOURCE_REGEX.matchEntire(this) ?: return this

    // Should we localize if there's a history?
    val (_, _, _, type, id, history) = matchResult.destructured
    return "$type/${id.localize(tenant)}$history"
}

// Common utilities that can assist with localizations

/**
 * Returns true if this list of [Element] update pairs contains any pair indicating an update occurred.
 */
fun <T : Element> List<Pair<T, Boolean>>.hasUpdates(): Boolean = any { it.second }

/**
 * Returns a list of the [Element]s from this list.
 */
fun <T : Element> List<Pair<T, Boolean>>.values(): List<T> = map { it.first }

private fun getUpdatedExtensions(element: Element, tenant: Tenant): List<Extension> {
    return getUpdatedExtensions(element.extension, tenant)
}

private fun getUpdatedModifierExtensions(element: BackboneElement, tenant: Tenant): List<Extension> {
    return getUpdatedExtensions(element.modifierExtension, tenant)
}

private fun getUpdatedExtensions(extensions: List<Extension>, tenant: Tenant): List<Extension> {
    val extensionLocalizations = extensions.map { it.localizePair(tenant) }
    return if (extensionLocalizations.hasUpdates()) extensionLocalizations.values() else listOf()
}

// Element Localizers

/**
 * Localizes this Address relative to the [tenant]. If this Address does not contain any localizable information, the
 * current Address will be returned.
 */
fun Address.localize(tenant: Tenant): Address {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val periodPair = period?.localizePair(tenant) ?: Pair(null, false)
    if (updatedExtensions.isNotEmpty() || periodPair.second) {
        return Address(
            id,
            updatedExtensions.ifEmpty { extension },
            use,
            type,
            text,
            line,
            city,
            district,
            state,
            postalCode,
            country,
            periodPair.first
        )
    }

    return this
}

/**
 * Localizes this Attachment relative to the [tenant]. If this Attachment does not contain any localizable information,
 * the current Attachment will be returned.
 */
fun Attachment.localize(tenant: Tenant): Attachment {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    if (updatedExtensions.isNotEmpty()) {
        return Attachment(id, updatedExtensions, contentType, language, data, url, size, hash, title, creation)
    }

    return this
}

/**
 * Localizes this AvailableTime relative to the [tenant]. If this AvailableTime does not contain any localizable
 * information, the current AvailableTime will be returned.
 */
fun AvailableTime.localize(tenant: Tenant): AvailableTime {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val updatedModifierExtensions = getUpdatedModifierExtensions(this, tenant)

    if (updatedExtensions.isNotEmpty() || updatedModifierExtensions.isNotEmpty()) {
        return AvailableTime(
            id,
            updatedExtensions.ifEmpty { extension },
            updatedModifierExtensions.ifEmpty { modifierExtension },
            daysOfWeek,
            allDay,
            availableStartTime,
            availableEndTime
        )
    }

    return this
}

/**
 * Localizes this CodeableConcept relative to the [tenant]. If this CodeableConcept does not contain any localizable
 * information, the current CodeableConcept will be returned.
 */
fun CodeableConcept.localize(tenant: Tenant): CodeableConcept = localizePair(tenant).first

/**
 * Attempts to localize this CodeableConcept relative to the [tenant] and returns the localized CodeableConcept and its
 * update status as a pair. If this CodeableConcept does not contain any localizable information, a Pair containing the
 * current CodeableConcept and false will be returned.
 */
fun CodeableConcept.localizePair(tenant: Tenant): Pair<CodeableConcept, Boolean> {
    val updatedExtensions = getUpdatedExtensions(this, tenant)

    val codingLocalizations = coding.map { it.localizePair(tenant) }
    val updatedCodings = codingLocalizations.hasUpdates()
    if (updatedExtensions.isNotEmpty() || updatedCodings) {
        return Pair(
            CodeableConcept(id, updatedExtensions.ifEmpty { extension }, codingLocalizations.values(), text),
            true
        )
    }

    return Pair(this, false)
}

/**
 * Attempts to localize this Coding relative to the [tenant] and returns the localized Coding and its update
 * status as a pair. If this Coding does not contain any localizable information, a Pair containing the current
 * Coding and false will be returned.
 */
fun Coding.localizePair(tenant: Tenant): Pair<Coding, Boolean> {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    if (updatedExtensions.isNotEmpty()) {
        return Pair(Coding(id, updatedExtensions, system, version, code, display, userSelected), true)
    }

    return Pair(this, false)
}

/**
 * Localizes this [Communication] relative to the [tenant].  If this [Communication] does not contain any localizable
 * information, the current [Communication] will be returned.
 */
fun Communication.localize(tenant: Tenant): Communication {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val updatedModifierExtensions = getUpdatedModifierExtensions(this, tenant)
    val updatedLanguage = language.localize(tenant)

    if (updatedExtensions.isNotEmpty() || updatedModifierExtensions.isNotEmpty() || updatedLanguage !== language) {
        return Communication(
            id = id,
            extension = updatedExtensions.ifEmpty { extension },
            modifierExtension = updatedModifierExtensions.ifEmpty { modifierExtension },
            language = updatedLanguage,
            preferred = preferred
        )
    }

    return this
}

/**
 * Localizes this [Contact] relative to the [tenant].  If this [Contact] does not contain any localizable information,
 * the current [Contact] will be returned.
 */
fun Contact.localize(tenant: Tenant): Contact {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val updatedModifierExtensions = getUpdatedModifierExtensions(this, tenant)
    val updatedName = name?.localize(tenant)
    val telecomLocalizations = telecom.map { it.localizePair(tenant) }
    val updatedTelecom = telecomLocalizations.hasUpdates()
    val updatedOrganization = organization?.localize(tenant)
    val updatedPeriod = period?.localize(tenant)

    if (updatedExtensions.isNotEmpty() ||
        updatedModifierExtensions.isNotEmpty() ||
        updatedName !== name ||
        updatedTelecom ||
        updatedOrganization !== organization ||
        updatedPeriod !== period
    ) {
        return Contact(
            id = id,
            extension = updatedExtensions.ifEmpty { extension },
            modifierExtension = updatedModifierExtensions.ifEmpty { modifierExtension },
            relationship = relationship,
            name = updatedName,
            telecom = telecomLocalizations.values(),
            address = address,
            gender = gender,
            organization = updatedOrganization,
            period = updatedPeriod
        )
    }

    return this
}

/**
 * Localizes this ContactPoint relative to the [tenant]. If this ContactPoint does not contain any localizable
 * information, the current ContactPoint will be returned.
 */
fun ContactPoint.localize(tenant: Tenant): ContactPoint {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val periodPair = period?.localizePair(tenant) ?: Pair(null, false)
    if (updatedExtensions.isNotEmpty() || periodPair.second) {
        return ContactPoint(id, updatedExtensions.ifEmpty { extension }, system, value, use, rank, periodPair.first)
    }

    return this
}

/**
 * Attempts to localize this [ContactPoint] relative to the [tenant] and returns the localized [ContactPoint] and its
 * update status as a pair. If this Coding does not contain any localizable information, a Pair containing the current
 * [ContactPoint] and false will be returned.
 */
fun ContactPoint.localizePair(tenant: Tenant): Pair<ContactPoint, Boolean> {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val periodPair = period?.localizePair(tenant) ?: Pair(null, false)
    if (updatedExtensions.isNotEmpty() || periodPair.second) {
        return Pair(
            ContactPoint(
                id,
                updatedExtensions.ifEmpty { extension },
                system,
                value,
                use,
                rank,
                periodPair.first
            ),
            true
        )
    }

    return Pair(this, false)
}

/**
 * Localizes this Extension relative to the [tenant]. If this Extension does not contain any localizable information,
 * the current Extension will be returned.
 */
fun Extension.localize(tenant: Tenant): Extension = localizePair(tenant).first

/**
 * Attempts to localize this Extension relative to the [tenant] and returns the localized Extension and its update
 * status as a pair. If this Extension does not contain any localizable information, a Pair containing the current
 * Extension and false will be returned.
 */
fun Extension.localizePair(tenant: Tenant): Pair<Extension, Boolean> {
    val updatedExtensions = getUpdatedExtensions(this, tenant)

    val updateValue = value?.type == DynamicValueType.REFERENCE
    if (updatedExtensions.isNotEmpty() || updateValue) {
        val extensions = updatedExtensions.ifEmpty { extension }
        val newValue: DynamicValue<Any>? = if (updateValue)
            DynamicValue(value!!.type, (value!!.value as Reference).localize(tenant)) else value

        return Pair(Extension(id, extensions, url, newValue), true)
    }

    return Pair(this, false)
}

/**
 * Localizes this HumanName relative to the [tenant]. If this HumanName does not contain any localizable information,
 * the current HumanName will be returned.
 */
fun HumanName.localize(tenant: Tenant): HumanName {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val periodPair = period?.localizePair(tenant) ?: Pair(null, false)
    if (updatedExtensions.isNotEmpty() || periodPair.second) {
        return HumanName(
            id,
            updatedExtensions.ifEmpty { extension },
            use,
            text,
            family,
            given,
            prefix,
            suffix,
            periodPair.first
        )
    }
    return this
}

/**
 * Localizes this Identifier relative to the [tenant]. If this Identifier does not contain any localizable information,
 * the current Identifier will be returned.
 */
fun Identifier.localize(tenant: Tenant): Identifier = localizePair(tenant).first

/**
 * Attempts to localize this Identifier relative to the [tenant] and returns the localized Identifier and its update
 * status as a pair. If this Identifier does not contain any localizable information, a Pair containing the current
 * Identifier and false will be returned.
 */
fun Identifier.localizePair(tenant: Tenant): Pair<Identifier, Boolean> {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val typePair = type?.localizePair(tenant) ?: Pair(null, false)
    val periodPair = period?.localizePair(tenant) ?: Pair(null, false)
    val assignerPair = assigner?.localizePair(tenant) ?: Pair(null, false)
    if (updatedExtensions.isNotEmpty() || typePair.second || periodPair.second || assignerPair.second) {
        return Pair(
            Identifier(
                id,
                updatedExtensions.ifEmpty { extension },
                use,
                typePair.first,
                system,
                value,
                periodPair.first,
                assignerPair.first
            ),
            true
        )
    }

    return Pair(this, false)
}

/**
 * Localizes this [PatientLink] relative to the [tenant].  If this [PatientLink] does not contain any localizable
 * information, the current [PatientLink] will be returned.
 */
fun PatientLink.localize(tenant: Tenant): PatientLink {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val updatedModifierExtensions = getUpdatedModifierExtensions(this, tenant)
    val updatedOther = other.localize(tenant)

    if (updatedExtensions.isNotEmpty() || updatedModifierExtensions.isNotEmpty() || updatedOther !== other) {
        return PatientLink(
            id = id,
            extension = updatedExtensions.ifEmpty { extension },
            modifierExtension = updatedModifierExtensions.ifEmpty { modifierExtension },
            other = updatedOther,
            type = type
        )
    }

    return this
}

/**
 * Localizes this Meta relative to the [tenant]. If this Meta does not contain any localizable information, the current
 * Meta will be returned.
 */
fun Meta.localize(tenant: Tenant): Meta {
    val updatedExtensions = getUpdatedExtensions(this, tenant)

    val securityLocalizations = security.map { it.localizePair(tenant) }
    val updatedSecurities = securityLocalizations.hasUpdates()

    val tagLocalizations = tag.map { it.localizePair(tenant) }
    val updatedTags = tagLocalizations.hasUpdates()
    if (updatedExtensions.isNotEmpty() || updatedSecurities || updatedTags) {
        return Meta(
            id,
            updatedExtensions.ifEmpty { extension },
            versionId,
            lastUpdated,
            source,
            profile,
            securityLocalizations.values(),
            tagLocalizations.values()
        )
    }

    return this
}

/**
 * Localizes this Narrative relative to the [tenant]. If this Narrative does not contain any localizable information,
 * the current Narrative will be returned.
 */
fun Narrative.localize(tenant: Tenant): Narrative {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    if (updatedExtensions.isNotEmpty()) {
        return Narrative(id, updatedExtensions, status, div)
    }

    return this
}

/**
 * Localizes this NotAvailable relative to the [tenant]. If this NotAvailable does not contain any localizable
 * information, the current NotAvailable will be returned.
 */
fun NotAvailable.localize(tenant: Tenant): NotAvailable {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val updatedModifierExtensions = getUpdatedModifierExtensions(this, tenant)
    val duringPair = during?.localizePair(tenant) ?: Pair(null, false)

    if (updatedExtensions.isNotEmpty() || updatedModifierExtensions.isNotEmpty() || duringPair.second) {
        return NotAvailable(
            id,
            updatedExtensions.ifEmpty { extension },
            updatedModifierExtensions.ifEmpty { modifierExtension },
            description,
            duringPair.first
        )
    }

    return this
}

/**
 * Localizes this [Participant] relative to the [tenant].  If this [Participant] does not contain any localizable
 * information, the current [Participant] will be returned.
 */
fun Participant.localize(tenant: Tenant): Participant {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val updatedModifierExtensions = getUpdatedModifierExtensions(this, tenant)

    val typeLocalizations = type.map { it.localizePair(tenant) }
    val updatedType = typeLocalizations.hasUpdates()

    val updatedActor = actor?.localizePair(tenant)

    val updatedPeriod = period?.localize(tenant)

    if (updatedExtensions.isNotEmpty() ||
        updatedModifierExtensions.isNotEmpty() ||
        updatedType ||
        updatedActor?.first !== actor ||
        updatedPeriod !== period
    ) {
        return Participant(
            id = id,
            extension = updatedExtensions.ifEmpty { extension },
            modifierExtension = updatedModifierExtensions.ifEmpty { modifierExtension },
            type = typeLocalizations.values(),
            actor = updatedActor?.first,
            required = required,
            status = status,
            period = updatedPeriod
        )
    }

    return this
}

/**
 * Localizes this Period relative to the [tenant]. If this Period does not contain any localizable information, the
 * current Period will be returned.
 */
fun Period.localize(tenant: Tenant): Period = localizePair(tenant).first

/**
 * Attempts to localize this Period relative to the [tenant] and returns the localized Period and its update
 * status as a pair. If this Period does not contain any localizable information, a Pair containing the current
 * Period and false will be returned.
 */
fun Period.localizePair(tenant: Tenant): Pair<Period, Boolean> {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    if (updatedExtensions.isNotEmpty()) {
        return Pair(Period(id, updatedExtensions, start, end), true)
    }

    return Pair(this, false)
}

/**
 * Localizes this Qualification relative to the [tenant]. If this Qualification does not contain any localizable
 * information, the current Qualification will be returned.
 */
fun Qualification.localize(tenant: Tenant): Qualification {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    val updatedModifierExtensions = getUpdatedModifierExtensions(this, tenant)

    val identifierLocalizations = identifier.map { it.localizePair(tenant) }
    val updatedIdentifiers = identifierLocalizations.hasUpdates()

    val codePair = code.localizePair(tenant)
    val periodPair = period?.localizePair(tenant) ?: Pair(null, false)
    val issuerPair = issuer?.localizePair(tenant) ?: Pair(null, false)

    if (updatedExtensions.isNotEmpty() || updatedModifierExtensions.isNotEmpty() || updatedIdentifiers || codePair.second || periodPair.second || issuerPair.second) {
        return Qualification(
            id,
            updatedExtensions.ifEmpty { extension },
            updatedModifierExtensions.ifEmpty { modifierExtension },
            identifierLocalizations.values(),
            codePair.first,
            periodPair.first,
            issuerPair.first
        )
    }

    return this
}

/**
 * Localizes this Reference relative to the [tenant]. If this Reference does not contain any localizable information,
 * the current Reference will be returned.
 */
fun Reference.localize(tenant: Tenant): Reference = localizePair(tenant).first

/**
 * Attempts to localize this Reference relative to the [tenant] and returns the localized Reference and its update
 * status as a pair. If this Reference does not contain any localizable information, a Pair containing the current
 * Reference and false will be returned.
 */
fun Reference.localizePair(tenant: Tenant): Pair<Reference, Boolean> {
    val updatedExtensions = getUpdatedExtensions(this, tenant)
    if (updatedExtensions.isNotEmpty() || reference != null) {
        return Pair(
            Reference(
                id,
                updatedExtensions.ifEmpty { extension },
                reference?.localizeReference(tenant),
                type,
                identifier,
                display
            ),
            true
        )
    }

    return Pair(this, false)
}
