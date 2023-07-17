package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.generators.resources.DocumentReferenceGenerator
import com.projectronin.interop.fhir.generators.resources.documentReference
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.resource.observation.subjectReferenceOptions
import com.projectronin.interop.fhir.ronin.generators.util.generateCode
import com.projectronin.interop.fhir.ronin.generators.util.generateCodeableConcept
import com.projectronin.interop.fhir.ronin.generators.util.generateReference
import com.projectronin.interop.fhir.ronin.generators.util.generateRequiredCodeableConceptList
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

fun rcdmDocumentReference(tenant: String, block: DocumentReferenceGenerator.() -> Unit): DocumentReference {
    return documentReference {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.DOCUMENT_REFERENCE, tenant) {}
        extension of tenantDocumentReferenceTypeSourceExtension
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        status of generateCode(status.generate(), possibleDocumentReferenceStatusCodes.random())
        type of generateCodeableConcept(type.generate(), possibleDocumentReferenceTypeCodes.random())
        category of generateRequiredCodeableConceptList(category.generate(), possibleDocumentReferenceCategoryCodes.random())
        subject of generateReference(subject.generate(), subjectReferenceOptions, tenant, "Patient")
    }
}

fun Patient.rcdmDocumentReference(block: DocumentReferenceGenerator.() -> Unit): DocumentReference {
    val data = this.referenceData()
    return rcdmDocumentReference(data.tenantId) {
        block.invoke(this)
        subject of generateReference(
            subject.generate(),
            subjectReferenceOptions,
            data.tenantId,
            "Patient",
            data.udpId
        )
    }
}

private val tenantDocumentReferenceTypeSourceExtension = listOf(
    Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.value),
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            CodeableConcept(
                text = "Tenant Note".asFHIR(),
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Invalid Note".asFHIR(),
                        code = Code("invalid-note")
                    )
                )
            )
        )
    )
)

val possibleDocumentReferenceStatusCodes = listOf(
    Code("current"),
    Code("superseded"),
    Code("entered-in-error")
)

val possibleDocumentReferenceCategoryCodes = listOf(
    // per RCDM: The US Core DocumentReferences Type Value Set is a starter set
    // of categories supported for fetching and storing clinical notes.
    coding {
        system of CodeSystem.DOCUMENT_REFERENCE_CATEGORY.uri
        code of Code("clinical-note")
        display of "Clinical Note"
    }
)

val possibleDocumentReferenceTypeCodes = listOf(
    // per RCDM: All LOINC values whose SCALE is DOC in the LOINC database
    // and the HL7 v3 Code System NullFlavor concept 'unknown'. Use "UNK" plus
    // a short extract from USCore featuring keywords "cancer", "oncology", etc.
    coding {
        system of CodeSystem.NULL_FLAVOR.uri
        code of Code("UNK")
        display of "Unknown"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100029-8")
        display of "Cancer related multigene analysis in Plasma cell-free DNA by Molecular genetics method"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100213-8")
        display of "Prostate cancer multigene analysis in Blood or Tissue by Molecular genetics method"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100215-3")
        display of "Episode of care medical records Document Transplant surgery"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100217-9")
        display of "Surgical oncology synoptic report"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100455-5")
        display of "Clinical pathology Outpatient Progress note"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100468-8")
        display of "Gynecologic oncology Outpatient Progress note"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100474-6")
        display of "Hematology+Medical oncology Outpatient Progress note"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100496-9")
        display of "Oncology Outpatient Progress note"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100525-5")
        display of "Radiation oncology Outpatient Progress note"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100526-3")
        display of "Radiology Outpatient Progress note"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100553-7")
        display of "Blood banking and transfusion medicine Hospital Progress note"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100563-6")
        display of "Clinical pathology Hospital Progress note"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100604-8")
        display of "Oncology Hospital Progress note"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100631-1")
        display of "Radiation oncology Hospital Progress note"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("100719-4")
        display of "Surgical oncology Discharge summary"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("101136-0")
        display of "Radiation oncology End of treatment letter"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("11486-8")
        display of "Chemotherapy records"
    },
    coding {
        system of CodeSystem.LOINC.uri
        code of Code("18776-5")
        display of "Plan of care note"
    }
)

val authorReferenceOptions = listOf(
    "Patient",
    "Practitioner",
    "PractitionerRole",
    "Organization"
)

val authenticatorReferenceOptions = listOf(
    "Practitioner",
    "Organization",
    "PractitionerRole"
)

val custodianReferenceOptions = listOf(
    "Organization"
)
