package com.projectronin.interop.fhir.ronin.code

import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

private val FHIR_CODING_OID_TO_URI = mapOf(
    "2.16.840.1.113883.6.96" to "http://snomed.info/sct",
    "2.16.840.1.113883.6.88" to "http://www.nlm.nih.gov/research/umls/rxnorm",
    "2.16.840.1.113883.6.1" to "http://loinc.org",
    "2.16.840.1.113883.6.8" to "http://unitsofmeasure.org",
    "2.16.840.1.113883.3.26.1.2" to "http://ncimeta.nci.nih.gov",
    "2.16.840.1.113883.6.12" to "http://www.ama-assn.org/go/cpt",
    "2.16.840.1.113883.6.209" to "http://hl7.org/fhir/ndfrt",
    "2.16.840.1.113883.4.9" to "http://fdasis.nlm.nih.gov",
    "2.16.840.1.113883.6.69" to "http://hl7.org/fhir/sid/ndc",
    "2.16.840.1.113883.12.292" to "http://hl7.org/fhir/sid/cvx",
    "1.0.3166.1.2.2" to "urn:iso:std:iso:3166",
    "2.16.840.1.113883.6.344" to "http://hl7.org/fhir/sid/dsm5",
    "2.16.840.1.113883.6.301.5" to "http://www.nubc.org/patient-discharge",
    "2.16.840.1.113883.6.256" to "http://www.radlex.org",
    "2.16.840.1.113883.6.3" to "http://hl7.org/fhir/sid/icd-10",
    "2.16.840.1.113883.6.42" to "http://hl7.org/fhir/sid/icd-9-cm",
    "2.16.840.1.113883.6.90" to "http://hl7.org/fhir/sid/icd-10-cm",
    "2.16.840.1.113883.2.4.4.31.1" to "http://hl7.org/fhir/sid/icpc-1",
    "2.16.840.1.113883.6.139" to "http://hl7.org/fhir/sid/icpc-2",
    "2.16.840.1.113883.6.254" to "http://hl7.org/fhir/sid/icf-nl",
    "1.3.160" to "https://www.gs1.org/gtin",
    "2.16.840.1.113883.6.73" to "http://www.whocc.no/atc",
    "2.16.840.1.113883.6.24" to "urn:iso:std:iso:11073:10101",
    "1.2.840.10008.2.16.4" to "http://dicom.nema.org/resources/ontology/DCM",
    "2.16.840.1.113883.5.1105" to "http://hl7.org/fhir/NamingSystem/ca-hc-din",
    "2.16.840.1.113883.6.101" to "http://nucc.org/provider-taxonomy",
    "2.16.840.1.113883.6.14" to "https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets",
    "2.16.840.1.113883.6.43.1" to "http://terminology.hl7.org/CodeSystem/icd-o-3"
)

private val FHIR_IDENTIFIER_OID_TO_URI = mapOf(
    "2.16.840.1.113883.4.1" to "http://hl7.org/fhir/sid/us-ssn",
    "2.16.840.1.113883.4.6" to "http://hl7.org/fhir/sid/us-npi",
    "2.16.840.1.113883.4.7" to "http://hl7.org/fhir"
)

fun Uri.normalizeCoding(): Uri {
    return Uri(
        FHIR_CODING_OID_TO_URI[this.value]
            ?: FHIR_CODING_OID_TO_URI[this.value?.removePrefix("urn:oid:")]
            ?: this.value
    )
}

fun Uri.normalizeIdentifier(): Uri {
    return Uri(
        FHIR_IDENTIFIER_OID_TO_URI[this.value]
            ?: FHIR_IDENTIFIER_OID_TO_URI[this.value?.removePrefix("urn:oid:")]
            ?: this.value
    )
}
