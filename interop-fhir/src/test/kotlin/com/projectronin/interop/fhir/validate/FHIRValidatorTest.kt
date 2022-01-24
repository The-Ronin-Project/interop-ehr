package com.projectronin.interop.fhir.validate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FHIRValidatorTest {
    companion object {
        // Start up is slow so initialize once, additionally leverage a static release of the IG to ensure consistent results.
        private var validator: FHIRValidator =
            FHIRValidator("4.0.1", "src/test/resources/test_ig_package.tgz")
    }

    @Test
    fun `ensure valid FHIR passes`() {
        val rawString = """
            |     {
            |        "resourceType": "Patient",
            |        "id": "eJzlzKe3KPzAV5TtkxmNivQ3",
            |        "identifier": [
            |          {
            |            "use": "usual",
            |            "system": "urn:oid:2.16.840.1.113883.4.1",
            |            "value": "391-50-5316"
            |          }
            |        ],
            |        "active": true,
            |        "name": [
            |          {
            |            "use": "usual",
            |            "text": "Ali Mychart",
            |            "family": "Mychart",
            |            "given": [
            |              "Ali"
            |            ]
            |          }
            |        ],
            |        "gender": "female",
            |        "birthDate": "1987-01-15",
            |        "deceasedBoolean": false,
            |        "address": [
            |          {
            |            "use": "home",
            |            "line": [
            |              "123 Main St."
            |            ],
            |            "city": "Madison",
            |            "district": "DANE",
            |            "state": "WI",
            |            "postalCode": "53703",
            |            "country": "US"
            |          }
            |        ],
            |        "maritalStatus": {
            |           "coding": [
            |               {
            |                   "code": "M",
            |                   "system": "http://terminology.hl7.org/CodeSystem/v3-MaritalStatus",
            |                   "display": "Married"
            |               }
            |           },
            |          "text": "Married"
            |        },
            |        "generalPractitioner": [
            |          {
            |            "reference": "Practitioner/eM5CWtq15N0WJeuCet5bJlQ3",
            |            "display": "Physician Family Medicine, MD"
            |          },
            |          {
            |            "reference": "Practitioner/ef9TegF2nfECi-0Skirbvpg3",
            |            "display": "Physician One Cardiology, MD"
            |          }
            |        ],
            |        "managingOrganization": {
            |          "reference": "Organization/enRyWnSP963FYDpoks4NHOA3",
            |          "display": "Epic Hospital System"
            |        }
            |      }
        """.trimMargin()
        val result = validator.validate(rawString)

        assertEquals(false, result.hasFatal())
        assertEquals(false, result.hasError())
        assertEquals(false, result.hasWarn())
        // An info of all clear is expected
        assertEquals(true, result.hasInfo())
        assertEquals("All OK", result.getInfo()[0].details)
    }

    @Test
    fun `ensure errors, warning, and info are returned`() {
        val rawString = """
            |     {
            |        "resourceType": "Patient",
            |        "id": "eJzlzKe3KPzAV5TtkxmNivQ3",
            |        "extension": [
            |          {
            |            "extension": [
            |              {
            |                "valueCoding": {
            |                  "system": "http://terminology.hl7.org/CodeSystem/v3-NullFlavor",
            |                  "code": "UNK",
            |                  "display": "Unknown"
            |                },
            |                "url": "ombCategory"
            |              },
            |              {
            |                "valueString": "Unknown",
            |                "url": "text"
            |              }
            |            ],
            |            "url": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race"
            |          },
            |          {
            |            "extension": [
            |              {
            |                "valueString": "Unknown",
            |                "url": "text"
            |              }
            |            ],
            |            "url": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity"
            |          },
            |          {
            |            "valueCodeableConcept": {
            |              "coding": [
            |                {
            |                  "system": "urn:oid:1.2.840.114350.1.13.0.1.7.10.698084.130.657370.19999000",
            |                  "code": "female"
            |                }
            |              ]
            |            },
            |            "url": "http://open.epic.com/FHIR/StructureDefinition/extension/legal-sex"
            |          },
            |          {
            |            "valueCodeableConcept": {
            |              "coding": [
            |                {
            |                  "system": "http://hl7.org/fhir/gender-identity",
            |                  "code": "transgender-female"
            |                }
            |              ]
            |            },
            |            "url": "http://hl7.org/fhir/StructureDefinition/patient-genderIdentity"
            |          },
            |          {
            |            "valueCode": "person",
            |            "url": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex"
            |          }
            |        ],
            |        "identifier": [
            |          {
            |            "use": "usual",
            |            "system": "urn:oid:2.16.840.1.113883.4.1",
            |            "value": "391-50-5316"
            |          },
            |          {
            |            "use": "usual",
            |            "type": {
            |              "text": "MRN"
            |            },
            |            "system": "urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.14",
            |            "value": "202497"
            |          }
            |        ],
            |        "active": true,
            |        "name": [
            |          {
            |            "use": "usual",
            |            "text": "Ali Mychart",
            |            "family": "Mychart",
            |            "given": [
            |              "Ali"
            |            ]
            |          }
            |        ],
            |        "gender": "female",
            |        "birthDate": "1987-01-15",
            |        "deceasedBoolean": false,
            |        "address": [
            |          {
            |            "use": "home",
            |            "line": [
            |              "123 Main St."
            |            ],
            |            "city": "Madison",
            |            "district": "DANE",
            |            "state": "WI",
            |            "postalCode": "53703",
            |            "country": "US"
            |          }
            |        ],
            |        "maritalStatuslkjd;flkjasdl;fj": {
            |          "text": "Significant Other"
            |        },
            |        "generalPractitioner": [
            |          {
            |            "reference": "Practitioner/eM5CWtq15N0WJeuCet5bJlQ3",
            |            "display": "Physician Family Medicine, MD"
            |          },
            |          {
            |            "reference": "Practitioner/ef9TegF2nfECi-0Skirbvpg3",
            |            "display": "Physician One Cardiology, MD"
            |          }
            |        ],
            |        "managingOrganization": {
            |          "reference": "Organization/enRyWnSP963FYDpoks4NHOA3",
            |          "display": "Epic Hospital System"
            |        }
            |      }
        """.trimMargin()
        val result = validator.validate(rawString)

        assertEquals(false, result.hasFatal())
        assertEquals(true, result.hasError())
        assertEquals(3, result.getErrors().size)
        assertEquals(true, result.hasWarn())
        assertEquals(1, result.getWarnings().size)
        assertEquals(true, result.hasInfo())
        assertEquals(1, result.getInfo().size)
    }

    @Test
    fun `ensure fatal errors are returned`() {
        val rawString = """
            |     {
            |        "resourceType": "PatientPerson"
            |      }
        """.trimMargin()
        val result = validator.validate(rawString)

        assertEquals(true, result.hasFatal())
        assertEquals(1, result.getFatal().size)
        assertEquals(false, result.hasError())
        assertEquals(false, result.hasWarn())
        assertEquals(false, result.hasInfo())
    }

    @Test
    fun `ensure long resource ids are supported`() {
        val rawString = """
            |     {
            |        "resourceType": "Patient",
            |        "id": "eJzlzKe3KPzAV5TtkxmNivQ3eJzlzKe3KPzAV5TtkxmNivQ3eJzlzKe3KPzAV5TtkxmNivQ3",
            |        "identifier": [
            |          {
            |            "use": "usual",
            |            "system": "urn:oid:2.16.840.1.113883.4.1",
            |            "value": "391-50-5316"
            |          }
            |        ],
            |        "active": true,
            |        "name": [
            |          {
            |            "use": "usual",
            |            "text": "Ali Mychart",
            |            "family": "Mychart",
            |            "given": [
            |              "Ali"
            |            ]
            |          }
            |        ],
            |        "gender": "female",
            |        "birthDate": "1987-01-15",
            |        "deceasedBoolean": false,
            |        "address": [
            |          {
            |            "use": "home",
            |            "line": [
            |              "123 Main St."
            |            ],
            |            "city": "Madison",
            |            "district": "DANE",
            |            "state": "WI",
            |            "postalCode": "53703",
            |            "country": "US"
            |          }
            |        ],
            |        "maritalStatus": {
            |           "coding": [
            |               {
            |                   "code": "M",
            |                   "system": "http://terminology.hl7.org/CodeSystem/v3-MaritalStatus",
            |                   "display": "Married"
            |               }
            |           },
            |          "text": "Married"
            |        },
            |        "generalPractitioner": [
            |          {
            |            "reference": "Practitioner/eM5CWtq15N0WJeuCet5bJlQ3",
            |            "display": "Physician Family Medicine, MD"
            |          },
            |          {
            |            "reference": "Practitioner/ef9TegF2nfECi-0Skirbvpg3",
            |            "display": "Physician One Cardiology, MD"
            |          }
            |        ],
            |        "managingOrganization": {
            |          "reference": "Organization/enRyWnSP963FYDpoks4NHOA3",
            |          "display": "Epic Hospital System"
            |        }
            |      }
        """.trimMargin()

        val result = validator.validate(rawString)

        assertEquals(false, result.hasFatal())
        assertEquals(false, result.hasError())
        assertEquals(false, result.hasWarn())
        // An info of all clear is expected
        assertEquals(true, result.hasInfo())
        assertEquals("All OK", result.getInfo()[0].details)
    }
}
