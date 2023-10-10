package com.projectronin.interop.fhir.ronin.normalization

import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.ConceptMap
import com.projectronin.interop.fhir.r4.resource.ConceptMapDependsOn
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.ValueSet
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.ronin.normalization.dependson.DependsOnEvaluator
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import com.projectronin.interop.fhir.ronin.validation.ValueSetMetadata
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class NormalizationRegistryClientTest {
    private val ociClient = mockk<OCIClient>()
    private val registryPath = "/DataNormalizationRegistry/v2/registry.json"
    private val medicationDependsOnEvaluator = mockk<DependsOnEvaluator<Medication>> {
        every { resourceType } returns Medication::class
    }

    private val client = NormalizationRegistryClient(ociClient, listOf(medicationDependsOnEvaluator), registryPath)

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val sourceAtoTargetAAA =
        SourceConcept(
            element = setOf(
                SourceKey(
                    value = "valueA",
                    system = "systemA"
                )
            )
        ) to listOf(
            TargetConcept(
                text = "textAAA",
                element = listOf(
                    TargetValue(
                        "targetValueAAA",
                        "targetSystemAAA",
                        "targetDisplayAAA",
                        "targetVersionAAA"
                    )
                )
            )
        )
    private val sourceBtoTargetBBB =
        SourceConcept(
            element = setOf(
                SourceKey(
                    value = "valueB",
                    system = "systemB"
                )
            )
        ) to listOf(
            TargetConcept(
                text = "textBBB",
                element = listOf(
                    TargetValue(
                        "targetValueBBB",
                        "targetSystemBBB",
                        "targetDisplayBBB",
                        "targetVersionBBB"
                    )
                )
            )
        )

    private val mapA = mapOf(sourceAtoTargetAAA)
    private val mapAB = mapOf(sourceAtoTargetAAA, sourceBtoTargetBBB)

    private val testConceptMap = """
            {
              "resourceType": "ConceptMap",
              "title": "Test Observations Mashup (for Dev Testing ONLY)",
              "id": "TestObservationsMashup-id",
              "name": "TestObservationsMashup-name",
              "contact": [
                {
                  "name": "Interops (for Dev Testing ONLY)"
                }
              ],
              "url": "http://projectronin.io/fhir/StructureDefinition/ConceptMap/TestObservationsMashup",
              "description": "Interops  (for Dev Testing ONLY)",
              "purpose": "Testing",
              "publisher": "Project Ronin",
              "experimental": true,
              "date": "2023-05-26",
              "version": 1,
              "group": [
                {
                  "source": "http://projectronin.io/fhir/CodeSystem/test/TestObservationsMashup",
                  "sourceVersion": "1.0",
                  "target": "http://loinc.org",
                  "targetVersion": "0.0.1",
                  "element": [
                    {
                      "id": "06c19b8e4718f1bf6e81f992cfc12c1e",
                      "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"72166-2\", \"display\": null, \"system\": \"http://loinc.org\"}]}}",
                      "display": "Tobacco smoking status",
                      "target": [
                        {
                          "id": "836cc342c39afcc7a8dee6277abc7b75",
                          "code": "72166-2",
                          "display": "Tobacco smoking status",
                          "equivalence": "equivalent",
                          "comment": null
                        }
                      ]
                    },
                    {
                      "id": "64be7ece9ab75a3eff827188c55c64db",
                      "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"363905002\", \"display\": \"Details of alcohol drinking behavior (observable entity)\", \"system\": \"http://snomed.info/sct\"}]}}",
                      "display": "Details of alcohol drinking behavior (observable entity)",
                      "target": [
                        {
                          "id": "8a3e6071d30318b9fbc23f3a1cd7de5a",
                          "code": "74013-4",
                          "display": "Alcoholic drinks per day",
                          "equivalence": "narrower",
                          "comment": "source-is-broader-than-target"
                        }
                      ]
                    },
                    {
                      "id": "8015a7419c2d19d6f515ccec9fb86c94",
                      "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"88028-6\", \"display\": null, \"system\": \"http://loinc.org\"}]}}",
                      "display": "Tobacco",
                      "target": [
                        {
                          "id": "751254b27af4ca8f6ea012292dc864cc",
                          "code": "88028-6",
                          "display": "Tobacco use panel",
                          "equivalence": "equivalent",
                          "comment": null
                        }
                      ]
                    },
                    {
                      "id": "921618c5505606cff5626f3468d4b396",
                      "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"85354-9\", \"display\": \"Blood pressure panel with all children optional\", \"system\": \"http://loinc.org\"}]}}",
                      "display": "Blood pressure",
                      "target": [
                        {
                          "id": "51afcac380447929e3f8d493590ded03",
                          "code": "85354-9",
                          "display": "Blood pressure panel with all children optional",
                          "equivalence": "equivalent",
                          "comment": null
                        },
                        {
                          "id": "d7eb5bea0cbb03b5a4d43b3ef663e39f",
                          "code": "3141-9",
                          "display": "Body weight Measured",
                          "equivalence": "equivalent",
                          "comment": null
                        },
                        {
                          "id": "e2495acfcd6ab8ebc776a3a2ae8bd6ce",
                          "code": "55284-4",
                          "display": "Blood pressure systolic and diastolic",
                          "equivalence": "equivalent",
                          "comment": null
                        }
                      ]
                    },
                    {
                      "id": "5aec5a329873f3842748e2beeb69643b",
                      "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"21704910\", \"display\": \"Potassium Level\", \"system\": \"https://fhir.cerner.com/ec2458f2-1e24-41c8-b71b-0e701af7583d/codeSet/72\"},{\"code\": \"2823-3\", \"display\": null, \"system\": \"http://loinc.org\"}]}}",
                      "display": "Potassium Level",
                      "target": [
                        {
                          "id": "e194363d552c9edf5c106fffc01236e1",
                          "code": "2823-3",
                          "display": "Potassium [Moles/volume] in Serum or Plasma",
                          "equivalence": "equivalent",
                          "comment": null
                        }
                      ]
                    }
                  ]
                }
              ],
              "extension": [
                {
                  "url": "http://projectronin.io/fhir/StructureDefinition/Extension/ronin-conceptMapSchema",
                  "valueString": "1.0.0"
                }
              ],
              "meta": {
                "lastUpdated": "2023-05-26T12:49:56.285403+00:00"
              }
        }
    """.trimIndent()

    private val testStagingConceptMap = """
        {
          "resourceType": "ConceptMap",
          "title": "TEST Staging Observation (Code) to Ronin Cancer Staging",
          "id": "TestObservationsStaging-id",
          "name": "TestObservationsStaging-name",
          "contact": [
            {
              "name": "Interops TEST Staging (for Dev Testing ONLY)"
            }
          ],
          "url": "http://projectronin.io/fhir/StructureDefinition/ConceptMap/TestObservationsStaging",
          "description": "This concept map is mapping from Staging Observations to value set Ronin Condition Code for data normalization",
          "purpose": "TEST for derivation service, used primarily by INFX and DP",
          "publisher": "Project Ronin",
          "experimental": false,
          "date": "2023-05-26",
          "version": 2,
          "group": [
            {
              "source": "http://projectronin.io/fhir/CodeSystem/v7r1eczk/PSJStagingRelatedObservationsCode",
              "sourceVersion": "1.0",
              "target": "http://snomed.info/sct",
              "targetVersion": "2023-03-01",
              "element": [
                {
                  "id": "195a31953334437c3e8742cd36c8b5a7",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#385355006\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#42394\", \"display\": \"residual tumor (R)\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - RESIDUAL TUMOR (R)",
                  "target": [
                    {
                      "id": "341901d82797f62a91d9416d297f8465",
                      "code": "1222601005",
                      "display": "American Joint Committee on Cancer residual tumor allowable value (qualifier value)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "5c21f8ce34b34b2ad1dbc84c293c68e0",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#385002007\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#42761\", \"display\": \"Gleason tertiary pattern\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - PROGNOSTIC INDICATORS - GLEASON TERTIARY PATTERN",
                  "target": [
                    {
                      "id": "db524dba8f3db52711a6a63dbd1d13d2",
                      "code": "385002007",
                      "display": "Tertiary Gleason pattern (observable entity)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "4951ec609b239add67ce50c5740643fd",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#399651003\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#46451\", \"display\": \"stage date\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - STAGE DATE",
                  "target": [
                    {
                      "id": "45c01a327811f45697b914959615b3f6",
                      "code": "399651003",
                      "display": "Date of report (observable entity)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "d2a2fdf0239f391f2b1156ac63df51f4",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#385419004\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#42393\", \"display\": \"venous invasion (V)\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - VENOUS INVASION (V)",
                  "target": [
                    {
                      "id": "0b2fc24e824f01d50160e3d419bf86e3",
                      "code": "385419004",
                      "display": "Venous (large vessel)/lymphatic (small vessel) tumor invasion finding (finding)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "f79339795b4cfb11d85751cbeda0483f",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#384995005\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#42760\", \"display\": \"Gleason secondary pattern\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - PROGNOSTIC INDICATORS - GLEASON SECONDARY PATTERN",
                  "target": [
                    {
                      "id": "a7cc0d1dc555890dadf4f63a97aa0c04",
                      "code": "384995005",
                      "display": "Secondary Gleason pattern (observable entity)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "1258c3fc9f48cf6a60a64ffef8948606",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#384994009\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#42759\", \"display\": \"Gleason primary pattern\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - PROGNOSTIC INDICATORS - GLEASON PRIMARY PATTERN",
                  "target": [
                    {
                      "id": "18e6f75c5971c968fa8a28120ead584c",
                      "code": "384994009",
                      "display": "Primary Gleason pattern (observable entity)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "7cb1019a0db3479dd40cba16dc5899ab",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#260878002\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#42383\", \"display\": \"primary tumor (T)\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - TNM CLASSIFICATION - AJCC T - PRIMARY TUMOR (T)",
                  "target": [
                    {
                      "id": "32b2cdd11d772b39c58bc85efbd20968",
                      "code": "260878002",
                      "display": "T - Tumor stage (attribute)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "0b0f965e675ec9b46724ee8ce6a4ec0c",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#263605001\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#42381\", \"display\": \"tumor size (mm)\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - TUMOR SIZE (MM)",
                  "target": [
                    {
                      "id": "6829e2f2dd4f21c350adcfda80dff455",
                      "code": "263605001",
                      "display": "Length dimension of neoplasm (observable entity)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "8ba61a5649cbd2f326d526acdcc459ff",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#260767000\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#42384\", \"display\": \"regional lymph nodes (N)\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - TNM CLASSIFICATION - AJCC N - REGIONAL LYMPH NODES (N)",
                  "target": [
                    {
                      "id": "73cb15052d834f3ffe2fb2484c2d7ef9",
                      "code": "260767000",
                      "display": "N - Regional lymph node stage (attribute)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "24d99f648dcf773d706ec493524ee906",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#260875004\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#42385\", \"display\": \"distant metastasis (M)\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - TNM CLASSIFICATION - AJCC M - DISTANT METASTASIS (M)",
                  "target": [
                    {
                      "id": "28cc218ca61ae7e73cb051162532a621",
                      "code": "260875004",
                      "display": "M - Distant metastasis stage (attribute)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "c03a8b85e707dfb39c984466273724e8",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#385348009\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#55984\", \"display\": \"Breslow depth (mm)\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - BRESLOW DEPTH (MM)",
                  "target": [
                    {
                      "id": "7cac4422ef11b1b4165d31395b4cc2c4",
                      "code": "385348009",
                      "display": "Breslow depth finding for melanoma (finding)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "939ea57bf2755aa6053b4c514582bfb7",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#385414009\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#42392\", \"display\": \"lymphatic vessel invasion (L)\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - LYMPHATIC VESSEL INVASION (L)",
                  "target": [
                    {
                      "id": "82318ec7d7623993740dcb3a7cc8ea2b",
                      "code": "385414009",
                      "display": "Lymphatic (small vessel) tumor invasion finding (finding)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "972b1bb1122980f015626063d44c950f",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"EPIC#44065\", \"display\": \"Clark's level\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - PROGNOSTIC INDICATORS - CLARK'S LEVEL",
                  "target": [
                    {
                      "id": "325f634512313c32178c27b5e9a53929",
                      "code": "385347004",
                      "display": "Clark melanoma level finding (finding)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "58ca30808a5de848ab6f0e33f8f5e1e3",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"EPIC#31000073346\", \"display\": \"WHO/ISUP grade (low/high)\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - WHO/ISUP GRADE (LOW/HIGH)",
                  "target": [
                    {
                      "id": "e7674b7ff6e68a57d7859c1b643264f1",
                      "code": "396659000",
                      "display": "Histologic grade of urothelial carcinoma by World Health Organization and International Society of Urological Pathology technique (observable entity)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "827f75db7803e5fc9ba811c19ccf4fbb",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"EPIC#42391\", \"display\": \"lymph-vascular invasion (LVI)\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - LYMPH-VASCULAR INVASION (LVI)",
                  "target": [
                    {
                      "id": "aa883e707553eb434789bb77ffb823bb",
                      "code": "371512006",
                      "display": "Presence of direct invasion by primary malignant neoplasm to lymphatic vessel and/or small blood vessel (observable entity)",
                      "equivalence": "narrower",
                      "comment": "source-is-broader-than-target"
                    }
                  ]
                },
                {
                  "id": "dcc915b3ac8b4b154ccc051847a8db43",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"EPIC#45453\", \"display\": \"estrogen receptor status\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - PROGNOSTIC INDICATORS - ESTROGEN RECEPTOR STATUS",
                  "target": [
                    {
                      "id": "8b51ed3859b702324993dfa734f3b3dc",
                      "code": "445028008",
                      "display": "Presence of estrogen receptor in neoplasm (observable entity)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "fc8e25ae3ed568f4dbf4b5d447452a18",
                  "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"SNOMED#246111003\", \"display\": null, \"system\": \"http://snomed.info/sct\"},{\"code\": \"EPIC#42388\", \"display\": \"anatomic stage/prognostic group\", \"system\": \"urn:oid:1.2.840.114350.1.13.297.2.7.2.727688\"}]}}",
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP",
                  "target": [
                    {
                      "id": "ae2c9f8e00c7426b0557cb1f4f0dded5",
                      "code": "246111003",
                      "display": "Prognostic score (attribute)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                }
              ]
            }
          ],
          "extension": [
            {
              "url": "http://projectronin.io/fhir/StructureDefinition/Extension/ronin-conceptMapSchema",
              "valueString": "1.0.0"
            }
          ],
          "meta": {
            "lastUpdated": "2023-05-26T17:20:40.696664+00:00"
          }
        }        
    """.trimIndent()
    private val conceptMapMetadata = ConceptMapMetadata(
        registryEntryType = "concept-map",
        conceptMapName = "test-concept-map",
        conceptMapUuid = "573b456efca5-03d51d53-1a31-49a9-af74",
        version = "1"
    )
    private val valueSetMetadata = ValueSetMetadata(
        registryEntryType = "value_set",
        valueSetName = "test-value-set",
        valueSetUuid = "03d51d53-1a31-49a9-af74-573b456efca5",
        version = "2"
    )

    @BeforeEach
    fun setUp() {
        mockkObject(JacksonUtil)
    }

    @AfterEach
    fun tearDown() {
        client.itemLastUpdated.clear()
        client.conceptMapCache.invalidateAll()
        client.valueSetCache.invalidateAll()
        unmockkAll()
    }

    @Test
    fun `getConceptMapping for Coding with no matching registry`() {
        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "phone")
        val mapping =
            client.getConceptMapping(
                tenant,
                "Patient.telecom.system",
                coding,
                mockk<Patient>()
            )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMapping for Coding pulls new registry and maps`() {
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "12345",
                filename = "file1.json",
                concept_map_name = "AppointmentStatus-tenant",
                concept_map_uuid = "cm-111",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Patient.telecom.use",
                registry_uuid = "67890",
                filename = "file2.json",
                concept_map_name = "PatientTelecomUse-tenant",
                concept_map_uuid = "cm-222",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ext2",
                resource_type = "Patient",
                tenant_id = "test"
            )
        )
        val mockkMap1 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystemAAA"
                    every { targetVersion?.value } returns "targetVersionAAA"
                    every { source?.value } returns "sourceSystemA"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueA"
                            every { display?.value } returns "targetTextAAA"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueAAA"
                                    every { display?.value } returns "targetDisplayAAA"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueB"
                            every { display?.value } returns "targetTextBBB"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueBBB"
                                    every { display?.value } returns "targetDisplayBBB"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        val mockkMap2 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem222"
                    every { targetVersion?.value } returns "targetVersion222"
                    every { source?.value } returns "sourceSystem2"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValue2"
                            every { display?.value } returns "targetText222"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValue222"
                                    every { display?.value } returns "targetDisplay222"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        val coding1 = Coding(
            code = Code(value = "sourceValueA"),
            system = Uri(value = "sourceSystemA")
        )
        val mapping1 = client.getConceptMapping(
            tenant,
            "Appointment.status",
            coding1,
            mockk<Appointment>()
        )!!
        assertEquals(mapping1.coding.code!!.value, "targetValueAAA")
        assertEquals(mapping1.coding.system!!.value, "targetSystemAAA")
        assertEquals(mapping1.extension.url!!.value, "ext1")
        assertEquals(mapping1.extension.value!!.value, coding1)
        val coding2 = Coding(
            code = Code(value = "sourceValue2"),
            system = Uri(value = "sourceSystem2")
        )
        val mapping2 =
            client.getConceptMapping(
                tenant,
                "Patient.telecom.use",
                coding2,
                mockk<Patient>()
            )!!
        assertEquals(mapping2.coding.code!!.value, "targetValue222")
        assertEquals(mapping2.coding.system!!.value, "targetSystem222")
        assertEquals(mapping2.extension.url!!.value, "ext2")
        assertEquals(mapping2.extension.value!!.value, coding2)
    }

    @Test
    fun `getConceptMappingForEnum with no matching registry - and source value is bad for enum - tries registry and fails`() {
        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "MyPhone")
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class,
                RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                mockk<Patient>()
            )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMappingForEnum with no matching registry - and source value is good for enum - returns enum as Coding`() {
        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "phone")
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class,
                RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                mockk<Patient>()
            )
        assertNotNull(mapping)
        mapping!!
        assertEquals(
            coding,
            mapping.coding
        )
        assertEquals(
            Extension(
                url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                value = DynamicValue(DynamicValueType.CODING, value = coding)
            ),
            mapping.extension
        )
    }

    @Test
    fun `getConceptMappingForEnum with match found in registry - returns target and extension`() {
        val registry1 = ConceptMapItem(
            source_extension_url = "ext1",
            map = mapOf(
                SourceConcept(
                    element = setOf(
                        SourceKey(
                            value = "MyPhone",
                            system = "http://projectronin.io/fhir/CodeSystem/ContactPointSystem"
                        )
                    )
                ) to listOf(
                    TargetConcept(
                        element = listOf(
                            TargetValue(
                                "good-or-bad-for-enum",
                                "good-or-bad-for-enum",
                                "good-or-bad-for-enum",
                                "1"
                            )
                        ),
                        text = "good-or-bad-for-enum, not validated here"
                    )
                )
            ),
            metadata = listOf(conceptMapMetadata)
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Patient.telecom.system",
            "test"
        )
        client.conceptMapCache.put(key, registry1)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key] = LocalDateTime.now()
        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "MyPhone")
        val mapping = client.getConceptMappingForEnum(
            tenant,
            "Patient.telecom.system",
            coding,
            ContactPointSystem::class,
            RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
            mockk<Patient>()
        )!!
        assertEquals(
            Coding(
                system = Uri("good-or-bad-for-enum"),
                code = Code("good-or-bad-for-enum"),
                display = "good-or-bad-for-enum".asFHIR(),
                version = "1".asFHIR()
            ),
            mapping.coding
        )
        assertEquals(
            Extension(
                url = Uri("ext1"),
                value = DynamicValue(DynamicValueType.CODING, value = coding)
            ),
            mapping.extension
        )
    }

    @Test
    fun `getValueSet with no matching registry`() {
        val mapping =
            client.getValueSet(
                "Patient.telecom.system",
                "specialAppointment"
            )
        assertTrue(mapping.codes.isEmpty())
    }

    @Test
    fun `getValueSet pulls registry and returns set`() {
        val vsTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "01234",
                filename = "file3.json",
                value_set_name = "AppointmentStatus",
                value_set_uuid = "vs-333",
                registry_entry_type = "value_set",
                version = "1",
                resource_type = "Appointment",
                profile_url = "specialAppointment"
            ),
            NormalizationRegistryItem(
                data_element = "Patient.telecom.use",
                registry_uuid = "56789",
                filename = "file4.json",
                value_set_name = "PatientTelecomUse",
                value_set_uuid = "vs-4444",
                registry_entry_type = "value_set",
                version = "1",
                resource_type = "Patient",
                profile_url = "specialPatient"
            )
        )
        val valueSetMetadata1 = ValueSetMetadata(
            registryEntryType = "value_set",
            valueSetName = "AppointmentStatus",
            valueSetUuid = "vs-333",
            version = "1"
        )
        val valueSetMetadata2 = ValueSetMetadata(
            registryEntryType = "value_set",
            valueSetName = "PatientTelecomUse",
            valueSetUuid = "vs-4444",
            version = "1"
        )
        val mockkSet1 = mockk<ValueSet> {
            every { expansion?.contains } returns listOf(
                mockk {
                    every { system?.value.toString() } returns "system1"
                    every { version?.value.toString() } returns "version1"
                    every { code?.value.toString() } returns "code1"
                    every { display?.value.toString() } returns "display1"
                }
            )
        }
        val mockkSet2 = mockk<ValueSet> {
            every { expansion?.contains } returns listOf(
                mockk {
                    every { system?.value.toString() } returns "system2"
                    every { version?.value.toString() } returns "version2"
                    every { code?.value.toString() } returns "code2"
                    every { display?.value.toString() } returns "display2"
                }
            )
        }
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns vsTestRegistry
        every { ociClient.getObjectFromINFX("file3.json") } returns "setJson1"
        every { JacksonUtil.readJsonObject("setJson1", ValueSet::class) } returns mockkSet1
        every { ociClient.getObjectFromINFX("file4.json") } returns "setJson2"
        every { JacksonUtil.readJsonObject("setJson2", ValueSet::class) } returns mockkSet2

        val valueSet1 = client.getValueSet("Appointment.status", "specialAppointment")
        val expectedCoding1 = ValueSetList(
            listOf(
                Coding(
                    system = Uri(value = "system1"),
                    code = Code(value = "code1"),
                    display = FHIRString(value = "display1"),
                    version = FHIRString(value = "version1")
                )
            ),
            valueSetMetadata1
        )
        assertEquals(valueSet1, expectedCoding1)

        val valueSet2 = client.getValueSet("Patient.telecom.use", "specialPatient")
        val expectedCoding2 = ValueSetList(
            listOf(
                Coding(
                    system = Uri(value = "system2"),
                    code = Code(value = "code2"),
                    display = FHIRString(value = "display2"),
                    version = FHIRString(value = "version2")
                )
            ),
            valueSetMetadata2
        )
        assertEquals(valueSet2, expectedCoding2)
    }

    @Test
    fun `getValueSet with special profile match`() {
        val registry1 = ValueSetItem(
            set = listOf(TargetValue("code1", "system1", "display1", "version1")),
            metadata = valueSetMetadata
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ValueSet,
            "Patient.telecom.system",
            null,
            "specialPatient"
        )
        client.valueSetCache.put(key, registry1)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key] = LocalDateTime.now()

        val mapping =
            client.getValueSet(
                "Patient.telecom.system",
                "specialPatient"
            )
        assertEquals(1, mapping.codes.size)
        assertEquals(Code("code1"), mapping.codes[0].code)
    }

    @Test
    fun `universal getRequiredValueSet with profile match`() {
        val registry1 = ValueSetItem(
            set = listOf(TargetValue("code1", "system1", "display1", "version1")),
            metadata = valueSetMetadata
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ValueSet,
            "Patient.telecom.system",
            null,
            "specialPatient"
        )
        client.valueSetCache.put(key, registry1)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key] = LocalDateTime.now()
        val actualValueSet =
            client.getRequiredValueSet(
                "Patient.telecom.system",
                "specialPatient"
            )
        assertEquals(1, actualValueSet.codes.size)
        assertEquals(Code("code1"), actualValueSet.codes[0].code)
    }

    @Test
    fun `ensure getRequiredValueSet fails when value set is not found`() {
        every { JacksonUtil.readJsonList(any(), NormalizationRegistryItem::class) } returns listOf()

        val exception = assertThrows<MissingNormalizationContentException> {
            client.getRequiredValueSet("Patient.telecom.system", "specialPatient")
        }
        assertEquals("Required value set for specialPatient and Patient.telecom.system not found", exception.message)
    }

    @Test
    fun `getConceptMapping for Coding with no system`() {
        val registry = ConceptMapItem(
            source_extension_url = "ext-AB",
            map = mapAB,
            metadata = listOf(conceptMapMetadata)
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key, registry)
        client.itemLastUpdated[key] = LocalDateTime.now()

        val sourceCoding1 = Coding(
            code = Code("valueA")
        )

        val mappedResult1 = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceCoding1,
            mockk<Observation>()
        )
        assertNull(mappedResult1)
    }

    @Test
    fun `getConceptMapping for Coding with no value`() {
        val registry = ConceptMapItem(
            source_extension_url = "ext-AB",
            map = mapAB,
            metadata = listOf(conceptMapMetadata)
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key, registry)
        client.itemLastUpdated[key] = LocalDateTime.now()

        val sourceCoding1 = Coding(
            system = Uri("system")
        )

        val mappedResult1 = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceCoding1,
            mockk<Observation>()
        )
        assertNull(mappedResult1)
    }

    @Test
    fun `getConceptMapping for Coding - correctly selects 1 entry from many in same map`() {
        val registry = ConceptMapItem(
            source_extension_url = "ext-AB",
            map = mapAB,
            metadata = listOf(conceptMapMetadata)
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key] = LocalDateTime.now()

        val sourceCoding1 = Coding(
            system = Uri("systemA"),
            code = Code("valueA")
        )
        val targetCoding1 = Coding(
            system = Uri("targetSystemAAA"),
            code = Code("targetValueAAA"),
            display = "targetDisplayAAA".asFHIR(),
            version = "targetVersionAAA".asFHIR()
        )
        val targetSourceExtension1 = Extension(
            url = Uri(value = "ext-AB"),
            value = DynamicValue(
                type = DynamicValueType.CODING,
                value = sourceCoding1
            )
        )
        val mappedResult1 = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceCoding1,
            mockk<Observation>()
        )
        assertEquals(
            targetCoding1,
            mappedResult1?.coding
        )
        assertEquals(
            targetSourceExtension1,
            mappedResult1?.extension
        )

        val sourceCoding2 = Coding(
            system = Uri("systemB"),
            code = Code("valueB")
        )
        val targetCoding2 = Coding(
            system = Uri("targetSystemBBB"),
            code = Code("targetValueBBB"),
            display = "targetDisplayBBB".asFHIR(),
            version = "targetVersionBBB".asFHIR()
        )
        val targetSourceExtension2 = Extension(
            url = Uri(value = "ext-AB"),
            value = DynamicValue(
                type = DynamicValueType.CODING,
                value = sourceCoding2
            )
        )
        val mappedResult2 = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceCoding2,
            mockk<Observation>()
        )
        assertEquals(
            targetCoding2,
            mappedResult2?.coding
        )
        assertEquals(
            targetSourceExtension2,
            mappedResult2?.extension
        )
    }

    @Test
    fun `getConceptMapping for Coding - map found - contains no matching code`() {
        val registry1 = ConceptMapItem(
            source_extension_url = "sourceExtensionUrl",
            map = mapA,
            metadata = listOf(conceptMapMetadata)
        )
        val key1hr = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key1hr, registry1)

        val sourceCoding = Coding(
            system = Uri("systemB"),
            code = Code("valueB")
        )

        val mappedResult = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceCoding,
            mockk<Observation>()
        )
        assertNull(mappedResult)
    }

    @Test
    fun `getConceptMapping for CodeableConcept with no matching registry`() {
        val coding = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", "phone")
        val concept = CodeableConcept(coding = listOf(coding))
        val mapping =
            client.getConceptMapping(
                tenant,
                "Patient.telecom.system",
                concept,
                mockk<Patient>()
            )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMapping for CodeableConcept pulls new registry and maps`() {
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "12345",
                filename = "file1.json",
                concept_map_name = "AppointmentStatus-tenant",
                concept_map_uuid = "cm-111",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Patient.telecom.use",
                registry_uuid = "67890",
                filename = "file2.json",
                concept_map_name = "PatientTelecomUse-tenant",
                concept_map_uuid = "cm-222",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ext2",
                resource_type = "Patient",
                tenant_id = "test"
            )
        )
        val mockkMap1 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystemAAA"
                    every { targetVersion?.value } returns "targetVersionAAA"
                    every { source?.value } returns "sourceSystemA"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueA"
                            every { display?.value } returns "targetTextAAA"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueAAA"
                                    every { display?.value } returns "targetDisplayAAA"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueB"
                            every { display?.value } returns "targetTextBBB"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueBBB"
                                    every { display?.value } returns "targetDisplayBBB"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        val mockkMap2 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem222"
                    every { targetVersion?.value } returns "targetVersion222"
                    every { source?.value } returns "sourceSystem2"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValue2"
                            every { display?.value } returns "targetText222"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValue222"
                                    every { display?.value } returns "targetDisplay222"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        val coding1 = Coding(
            code = Code(value = "sourceValueA"),
            system = Uri(value = "sourceSystemA")
        )
        val concept1 = CodeableConcept(
            text = "ignore-me-1".asFHIR(),
            coding = listOf(coding1)
        )
        val mapping1 = client.getConceptMapping(
            tenant,
            "Appointment.status",
            concept1,
            mockk<Appointment>()
        )!!
        assertEquals(mapping1.codeableConcept.coding.first().code!!.value, "targetValueAAA")
        assertEquals(mapping1.codeableConcept.coding.first().system!!.value, "targetSystemAAA")
        assertEquals(mapping1.extension.url!!.value, "ext1")
        assertEquals(mapping1.extension.value!!.value, concept1)
        val coding2 = Coding(
            code = Code(value = "sourceValue2"),
            system = Uri(value = "sourceSystem2")
        )
        val concept2 = CodeableConcept(
            text = "ignore-me-2".asFHIR(),
            coding = listOf(coding2)
        )
        val mapping2 = client.getConceptMapping(
            tenant,
            "Patient.telecom.use",
            concept2,
            mockk<Patient>()
        )!!
        assertEquals(mapping2.codeableConcept.coding.first().code!!.value, "targetValue222")
        assertEquals(mapping2.codeableConcept.coding.first().system!!.value, "targetSystem222")
        assertEquals(mapping2.extension.url!!.value, "ext2")
        assertEquals(mapping2.extension.value!!.value, concept2)
    }

    @Test
    fun `getConceptMapping for CodeableConcept - correctly selects 1 entry from many in same map`() {
        val registry = ConceptMapItem(
            source_extension_url = "ext-AB",
            map = mapAB,
            metadata = listOf(conceptMapMetadata)
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key] = LocalDateTime.now()

        val sourceCoding1 = Coding(
            system = Uri("systemA"),
            code = Code("valueA")
        )
        val sourceConcept1 = CodeableConcept(coding = listOf(sourceCoding1))
        val targetCoding1 = Coding(
            system = Uri("targetSystemAAA"),
            code = Code("targetValueAAA"),
            display = "targetDisplayAAA".asFHIR(),
            version = "targetVersionAAA".asFHIR()
        )
        val targetConcept1 = CodeableConcept(
            text = "textAAA".asFHIR(),
            coding = listOf(targetCoding1)
        )
        val targetSourceExtension1 = Extension(
            url = Uri(value = "ext-AB"),
            value = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = sourceConcept1
            )
        )
        val mappedResult1 = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceConcept1,
            mockk<Observation>()
        )
        assertEquals(
            targetConcept1,
            mappedResult1?.codeableConcept
        )
        assertEquals(
            targetSourceExtension1,
            mappedResult1?.extension
        )

        val sourceCoding2 = Coding(
            system = Uri("systemB"),
            code = Code("valueB")
        )
        val sourceConcept2 = CodeableConcept(coding = listOf(sourceCoding2))
        val targetCoding2 = Coding(
            system = Uri("targetSystemBBB"),
            code = Code("targetValueBBB"),
            display = "targetDisplayBBB".asFHIR(),
            version = "targetVersionBBB".asFHIR()
        )
        val targetConcept2 = CodeableConcept(
            text = "textBBB".asFHIR(),
            coding = listOf(targetCoding2)
        )
        val targetSourceExtension2 = Extension(
            url = Uri(value = "ext-AB"),
            value = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = sourceConcept2
            )
        )
        val mappedResult2 = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceConcept2,
            mockk<Observation>()
        )
        assertEquals(
            targetConcept2,
            mappedResult2?.codeableConcept
        )
        assertEquals(
            targetSourceExtension2,
            mappedResult2?.extension
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - map found - contains no matching code`() {
        val registry1 = ConceptMapItem(
            source_extension_url = "sourceExtensionUrl",
            map = mapA,
            metadata = listOf(conceptMapMetadata)
        )
        val key1hr = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key1hr, registry1)

        val sourceCoding = Coding(
            system = Uri("systemB"),
            code = Code("valueB")
        )
        val sourceConcept = CodeableConcept(coding = listOf(sourceCoding))

        val mappedResult = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceConcept,
            mockk<Observation>()
        )
        assertNull(mappedResult)
    }

    @Test
    fun `getConceptMapping for CodeableConcept - map found - target text replaces non-empty source text`() {
        val registry1 = ConceptMapItem(
            source_extension_url = "extl",
            map = mapOf(
                SourceConcept(
                    element = setOf(
                        SourceKey(
                            value = "valueA",
                            system = "systemA"
                        )
                    )
                ) to listOf(
                    TargetConcept(
                        text = "replaced-it",
                        element = listOf(
                            TargetValue(
                                "AAA",
                                "AAA",
                                "AAA",
                                "AAA"
                            )
                        )
                    )
                )
            ),
            metadata = listOf(conceptMapMetadata)
        )
        val key1 = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key1, registry1)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key1] = LocalDateTime.now()

        val sourceCoding = Coding(
            system = Uri("systemA"),
            code = Code("valueA")
        )
        val sourceConcept = CodeableConcept(
            text = "to-be-replaced".asFHIR(),
            coding = listOf(sourceCoding)
        )
        val targetCoding = Coding(
            system = Uri("AAA"),
            code = Code("AAA"),
            display = "AAA".asFHIR(),
            version = "AAA".asFHIR()
        )
        val targetConcept = CodeableConcept(
            text = "replaced-it".asFHIR(),
            coding = listOf(targetCoding)
        )
        val targetSourceExtension = Extension(
            url = Uri(value = "extl"),
            value = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = sourceConcept
            )
        )

        val mappedResult = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceConcept,
            mockk<Observation>()
        )
        assertEquals(
            targetConcept,
            mappedResult?.codeableConcept
        )
        assertEquals(
            targetSourceExtension,
            mappedResult?.extension
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept concatenates multiple matching concept maps`() {
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "11111",
                filename = "file1.json",
                concept_map_name = "Staging-test-1",
                concept_map_uuid = "cm-111",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-1",
                resource_type = "Observation",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "22222",
                filename = "file2.json",
                concept_map_name = "AllVitals-test-2",
                concept_map_uuid = "cm-222",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-1",
                resource_type = "Observation",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "33333",
                filename = "file3.json",
                concept_map_name = "HeartRate-test-3",
                concept_map_uuid = "cm-333",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-1",
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        val mockkMap1 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem-1"
                    every { targetVersion?.value } returns "targetVersion-1"
                    every { source?.value } returns "system-Staging-1"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueA"
                            every { display?.value } returns "targetTextAAA"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueAAA"
                                    every { display?.value } returns "targetDisplayAAA"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueB"
                            every { display?.value } returns "targetTextBBB"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueBBB"
                                    every { display?.value } returns "targetDisplayBBB"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        val mockkMap2 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem-2"
                    every { targetVersion?.value } returns "targetVersion-2"
                    every { source?.value } returns "system-AllVitals-2"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueX"
                            every { display?.value } returns "targetTextXXX"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueXXX"
                                    every { display?.value } returns "targetDisplayXXX"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueY"
                            every { display?.value } returns "targetTextYYY"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueYYY"
                                    every { display?.value } returns "targetDisplayYYY"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueZ"
                            every { display?.value } returns "targetTextZZZ"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueZZZ"
                                    every { display?.value } returns "targetDisplayZZZ"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        val mockkMap3 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem-3"
                    every { targetVersion?.value } returns "targetVersion-3"
                    every { source?.value } returns "system-HeartRate-3"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueC"
                            every { display?.value } returns "targetTextCCC"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueCCC"
                                    every { display?.value } returns "targetDisplayCCC"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueD"
                            every { display?.value } returns "targetTextDDD"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueDDD"
                                    every { display?.value } returns "targetDisplayDDD"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        every { ociClient.getObjectFromINFX("file3.json") } returns "mapJson3"
        every { JacksonUtil.readJsonObject("mapJson3", ConceptMap::class) } returns mockkMap3
        val coding1 = Coding(
            code = Code(value = "sourceValueB"),
            system = Uri(value = "system-Staging-1")
        )
        val concept1 = CodeableConcept(
            text = "ignore-me-1".asFHIR(),
            coding = listOf(coding1)
        )
        val mapping1 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept1,
            mockk<Observation>()
        )!!
        assertEquals("targetTextBBB", mapping1.codeableConcept.text!!.value)
        assertEquals(1, mapping1.codeableConcept.coding.size)
        assertEquals("targetSystem-1", mapping1.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueBBB", mapping1.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayBBB", mapping1.codeableConcept.coding[0].display!!.value)
        assertEquals("ObservationCode-1", mapping1.extension.url!!.value)
        assertEquals(concept1, mapping1.extension.value!!.value)
        val coding2 = Coding(
            code = Code(value = "sourceValueZ"),
            system = Uri(value = "system-AllVitals-2")
        )
        val concept2 = CodeableConcept(
            text = "ignore-me-2".asFHIR(),
            coding = listOf(coding2)
        )
        val mapping2 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept2,
            mockk<Observation>()
        )!!
        assertEquals("targetTextZZZ", mapping2.codeableConcept.text!!.value)
        assertEquals(1, mapping2.codeableConcept.coding.size)
        assertEquals("targetSystem-2", mapping2.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueZZZ", mapping2.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayZZZ", mapping2.codeableConcept.coding[0].display!!.value)
        assertEquals("ObservationCode-1", mapping2.extension.url!!.value)
        assertEquals(concept2, mapping2.extension.value!!.value)
        val coding3 = Coding(
            code = Code(value = "sourceValueC"),
            system = Uri(value = "system-HeartRate-3")
        )
        val concept3 = CodeableConcept(
            text = "ignore-me-3".asFHIR(),
            coding = listOf(coding3)
        )
        val mapping3 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept3,
            mockk<Observation>()
        )!!
        assertEquals("targetTextCCC", mapping3.codeableConcept.text!!.value)
        assertEquals(1, mapping3.codeableConcept.coding.size)
        assertEquals("targetSystem-3", mapping3.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueCCC", mapping3.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayCCC", mapping3.codeableConcept.coding[0].display!!.value)
        assertEquals("ObservationCode-1", mapping3.extension.url!!.value)
        assertEquals(concept3, mapping3.extension.value!!.value)
    }

    @Test
    fun `getConceptMapping for CodeableConcept concatenates multiple matching concept maps - excludes non-matching entries`() {
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "11111",
                filename = "file1.json",
                concept_map_name = "Staging-test-1",
                concept_map_uuid = "cm-111",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-1",
                resource_type = "Observation",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "22222",
                filename = "file2.json",
                concept_map_name = "AllVitals-test-2",
                concept_map_uuid = "cm-222",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-1",
                resource_type = "Observation",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "33333",
                filename = "file3.json",
                concept_map_name = "Appointment-status-test",
                concept_map_uuid = "cm-333",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "Appointment-status-test",
                resource_type = "Appointment",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "33333",
                filename = "file3.json",
                concept_map_name = "HeartRate-test-3",
                concept_map_uuid = "cm-333",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-1",
                resource_type = "Observation",
                tenant_id = "other" // wrong tenantId
            ),
            NormalizationRegistryItem(
                data_element = "Appointment.status", // wrong elementName
                registry_uuid = "44444",
                filename = "file4.json",
                concept_map_name = "Appointment-status-test",
                concept_map_uuid = "cm-444",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "Appointment-status-test",
                resource_type = "Appointment",
                tenant_id = "test"
            )
        )
        val mockkMap1 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem-1"
                    every { targetVersion?.value } returns "targetVersion-1"
                    every { source?.value } returns "system-Staging-1"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueA"
                            every { display?.value } returns "targetTextAAA"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueAAA"
                                    every { display?.value } returns "targetDisplayAAA"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueB"
                            every { display?.value } returns "targetTextBBB"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueBBB"
                                    every { display?.value } returns "targetDisplayBBB"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        val mockkMap2 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem-2"
                    every { targetVersion?.value } returns "targetVersion-2"
                    every { source?.value } returns "system-AllVitals-2"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueX"
                            every { display?.value } returns "targetTextXXX"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueXXX"
                                    every { display?.value } returns "targetDisplayXXX"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueY"
                            every { display?.value } returns "targetTextYYY"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueYYY"
                                    every { display?.value } returns "targetDisplayYYY"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueZ"
                            every { display?.value } returns "targetTextZZZ"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueZZZ"
                                    every { display?.value } returns "targetDisplayZZZ"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        val coding1 = Coding(
            code = Code(value = "sourceValueB"),
            system = Uri(value = "system-Staging-1")
        )
        val concept1 = CodeableConcept(
            text = "ignore-me-1".asFHIR(),
            coding = listOf(coding1)
        )
        val mapping1 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept1,
            mockk<Observation>()
        )!!
        assertEquals("targetTextBBB", mapping1.codeableConcept.text!!.value)
        assertEquals(1, mapping1.codeableConcept.coding.size)
        assertEquals("targetSystem-1", mapping1.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueBBB", mapping1.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayBBB", mapping1.codeableConcept.coding[0].display!!.value)
        assertEquals("ObservationCode-1", mapping1.extension.url!!.value)
        assertEquals(concept1, mapping1.extension.value!!.value)
        val coding2 = Coding(
            code = Code(value = "sourceValueZ"),
            system = Uri(value = "system-AllVitals-2")
        )
        val concept2 = CodeableConcept(
            text = "ignore-me-2".asFHIR(),
            coding = listOf(coding2)
        )
        val mapping2 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept2,
            mockk<Observation>()
        )!!
        assertEquals("targetTextZZZ", mapping2.codeableConcept.text!!.value)
        assertEquals(1, mapping2.codeableConcept.coding.size)
        assertEquals("targetSystem-2", mapping2.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueZZZ", mapping2.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayZZZ", mapping2.codeableConcept.coding[0].display!!.value)
        assertEquals("ObservationCode-1", mapping2.extension.url!!.value)
        assertEquals(concept2, mapping2.extension.value!!.value)
        val coding3 = Coding(
            code = Code(value = "sourceValueC"),
            system = Uri(value = "system-HeartRate-3")
        )
        val concept3 = CodeableConcept(
            text = "ignore-me-3".asFHIR(),
            coding = listOf(coding3)
        )
        val mapping3 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept3,
            mockk<Observation>()
        )
        assertNull(mapping3)
        val coding4 = Coding(
            code = Code(value = "arrived"),
            system = Uri(value = "AppointmentStatus-4")
        )
        val concept4 = CodeableConcept(
            text = "ignore-me-4".asFHIR(),
            coding = listOf(coding4)
        )
        val mapping4 = client.getConceptMapping(
            tenant,
            "Appointment.status",
            concept4,
            mockk<Appointment>()
        )
        assertNull(mapping4)
    }

    @Test
    fun `getConceptMapping for CodeableConcept concatenates multiple matching concept maps - multiple entries in target Coding lists`() {
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "11111",
                filename = "file1.json",
                concept_map_name = "Staging-test-1",
                concept_map_uuid = "cm-111",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-1",
                resource_type = "Observation",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "22222",
                filename = "file2.json",
                concept_map_name = "AllVitals-test-2",
                concept_map_uuid = "cm-222",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-1",
                resource_type = "Observation",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "33333",
                filename = "file3.json",
                concept_map_name = "HeartRate-test-3",
                concept_map_uuid = "cm-333",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-1",
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        val mockkMap1 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem-1"
                    every { targetVersion?.value } returns "targetVersion-1"
                    every { source?.value } returns "system-Staging-1"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueAB"
                            every { display?.value } returns "targetTextAB"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueAAA"
                                    every { display?.value } returns "targetDisplayAAA"
                                    every { dependsOn } returns emptyList()
                                },
                                mockk {
                                    every { code?.value } returns "targetValueBBB"
                                    every { display?.value } returns "targetDisplayBBB"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        val mockkMap2 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem-2"
                    every { targetVersion?.value } returns "targetVersion-2"
                    every { source?.value } returns "system-AllVitals-2"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueXYZ"
                            every { display?.value } returns "targetTextXYZ"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueXXX"
                                    every { display?.value } returns "targetDisplayXXX"
                                    every { dependsOn } returns emptyList()
                                },
                                mockk {
                                    every { code?.value } returns "targetValueYYY"
                                    every { display?.value } returns "targetDisplayYYY"
                                    every { dependsOn } returns emptyList()
                                },
                                mockk {
                                    every { code?.value } returns "targetValueZZZ"
                                    every { display?.value } returns "targetDisplayZZZ"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        val mockkMap3 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem-3"
                    every { targetVersion?.value } returns "targetVersion-3"
                    every { source?.value } returns "system-HeartRate-3"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueCD"
                            every { display?.value } returns "targetTextCD"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueCCC"
                                    every { display?.value } returns "targetDisplayCCC"
                                    every { dependsOn } returns emptyList()
                                },
                                mockk {
                                    every { code?.value } returns "targetValueDDD"
                                    every { display?.value } returns "targetDisplayDDD"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        every { ociClient.getObjectFromINFX("file3.json") } returns "mapJson3"
        every { JacksonUtil.readJsonObject("mapJson3", ConceptMap::class) } returns mockkMap3
        val coding1 = Coding(
            code = Code(value = "sourceValueAB"),
            system = Uri(value = "system-Staging-1")
        )
        val concept1 = CodeableConcept(
            text = "ignore-me-1".asFHIR(),
            coding = listOf(coding1)
        )
        val mapping1 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept1,
            mockk<Observation>()
        )!!
        assertEquals("targetTextAB", mapping1.codeableConcept.text!!.value)
        assertEquals(2, mapping1.codeableConcept.coding.size)
        assertEquals("targetSystem-1", mapping1.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueAAA", mapping1.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayAAA", mapping1.codeableConcept.coding[0].display!!.value)
        assertEquals("targetSystem-1", mapping1.codeableConcept.coding[1].system!!.value)
        assertEquals("targetValueBBB", mapping1.codeableConcept.coding[1].code!!.value)
        assertEquals("targetDisplayBBB", mapping1.codeableConcept.coding[1].display!!.value)
        assertEquals("ObservationCode-1", mapping1.extension.url!!.value)
        assertEquals(concept1, mapping1.extension.value!!.value)
        val coding2 = Coding(
            code = Code(value = "sourceValueXYZ"),
            system = Uri(value = "system-AllVitals-2")
        )
        val concept2 = CodeableConcept(
            text = "ignore-me-2".asFHIR(),
            coding = listOf(coding2)
        )
        val mapping2 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept2,
            mockk<Observation>()
        )!!
        assertEquals("targetTextXYZ", mapping2.codeableConcept.text!!.value)
        assertEquals(3, mapping2.codeableConcept.coding.size)
        assertEquals("targetSystem-2", mapping2.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueXXX", mapping2.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayXXX", mapping2.codeableConcept.coding[0].display!!.value)
        assertEquals("targetSystem-2", mapping2.codeableConcept.coding[1].system!!.value)
        assertEquals("targetValueYYY", mapping2.codeableConcept.coding[1].code!!.value)
        assertEquals("targetDisplayYYY", mapping2.codeableConcept.coding[1].display!!.value)
        assertEquals("targetSystem-2", mapping2.codeableConcept.coding[2].system!!.value)
        assertEquals("targetValueZZZ", mapping2.codeableConcept.coding[2].code!!.value)
        assertEquals("targetDisplayZZZ", mapping2.codeableConcept.coding[2].display!!.value)
        assertEquals("ObservationCode-1", mapping2.extension.url!!.value)
        assertEquals(concept2, mapping2.extension.value!!.value)
        val coding3 = Coding(
            code = Code(value = "sourceValueCD"),
            system = Uri(value = "system-HeartRate-3")
        )
        val concept3 = CodeableConcept(
            text = "ignore-me-3".asFHIR(),
            coding = listOf(coding3)
        )
        val mapping3 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept3,
            mockk<Observation>()
        )!!
        assertEquals("targetTextCD", mapping3.codeableConcept.text!!.value)
        assertEquals(2, mapping3.codeableConcept.coding.size)
        assertEquals("targetSystem-3", mapping3.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueCCC", mapping3.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayCCC", mapping3.codeableConcept.coding[0].display!!.value)
        assertEquals("targetSystem-3", mapping3.codeableConcept.coding[1].system!!.value)
        assertEquals("targetValueDDD", mapping3.codeableConcept.coding[1].code!!.value)
        assertEquals("targetDisplayDDD", mapping3.codeableConcept.coding[1].display!!.value)
        assertEquals("ObservationCode-1", mapping3.extension.url!!.value)
        assertEquals(concept3, mapping3.extension.value!!.value)
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - source Coding has 1 member - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "registry-uuid",
                filename = "file1.json",
                concept_map_name = "TestObservationsMashup",
                concept_map_uuid = "TestObservationsMashup-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "72166-2"),
                    system = Uri(value = "http://loinc.org")
                )
            )
        )
        val mapping = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept,
            mockk<Observation>()
        )!!
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("72166-2"),
                        system = Uri("http://loinc.org"),
                        version = "0.0.1".asFHIR(),
                        display = "Tobacco smoking status".asFHIR()
                    )
                ),
                text = "Tobacco smoking status".asFHIR()
            ),
            mapping.codeableConcept
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept)
            ),
            mapping.extension
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - source Coding has 2 members in order - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                filename = "file1.json",
                concept_map_name = "TestObservationsMashup",
                concept_map_uuid = "TestObservationsMashup-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "21704910"),
                    system = Uri(value = "https://fhir.cerner.com/ec2458f2-1e24-41c8-b71b-0e701af7583d/codeSet/72")
                ),
                Coding(
                    code = Code(value = "2823-3"),
                    system = Uri(value = "http://loinc.org")
                )
            )
        )
        val mapping = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept,
            mockk<Observation>()
        )!!
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("2823-3"),
                        system = Uri("http://loinc.org"),
                        version = "0.0.1".asFHIR(),
                        display = "Potassium [Moles/volume] in Serum or Plasma".asFHIR()
                    )
                ),
                text = "Potassium Level".asFHIR()
            ),
            mapping.codeableConcept
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept)
            ),
            mapping.extension
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - source Coding has 2 members out of order - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                filename = "file1.json",
                concept_map_name = "Cerncodeobservationstoloinc",
                concept_map_uuid = "ef731708-e333-4933-af74-6bf97cb4077e",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "2823-3"),
                    system = Uri(value = "http://loinc.org")
                ),
                Coding(
                    code = Code(value = "21704910"),
                    system = Uri(value = "https://fhir.cerner.com/ec2458f2-1e24-41c8-b71b-0e701af7583d/codeSet/72")
                )
            )
        )
        val mapping = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept,
            mockk<Observation>()
        )!!
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("2823-3"),
                        system = Uri("http://loinc.org"),
                        version = "0.0.1".asFHIR(),
                        display = "Potassium [Moles/volume] in Serum or Plasma".asFHIR()
                    )
                ),
                text = "Potassium Level".asFHIR()
            ),
            mapping.codeableConcept
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept)
            ),
            mapping.extension
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - source Coding has partial match but too few members - no match`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                filename = "file1.json",
                concept_map_name = "TestObservationsMashup",
                concept_map_uuid = "TestObservationsMashup-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "21704910"),
                    system = Uri(value = "https://fhir.cerner.com/ec2458f2-1e24-41c8-b71b-0e701af7583d/codeSet/72")
                )
            )
        )
        val mapping = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept,
            mockk<Observation>()
        )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - source Coding has partial match but too many members - no match`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                filename = "file1.json",
                concept_map_name = "TestObservationsMashup",
                concept_map_uuid = "TestObservationsMashup-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "21704910"),
                    system = Uri(value = "https://fhir.cerner.com/ec2458f2-1e24-41c8-b71b-0e701af7583d/codeSet/72")
                ),
                Coding(
                    code = Code(value = "2823-3"),
                    system = Uri(value = "http://loinc.org")
                ),
                Coding(
                    code = Code(value = "72166-2"),
                    system = Uri(value = "http://loinc.org")
                )
            )
        )
        val mapping = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept,
            mockk<Observation>()
        )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - match found - multiple targets are returned correctly`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "registry-uuid",
                filename = "file1.json",
                concept_map_name = "TestObservationsMashup",
                concept_map_uuid = "TestObservationsMashup-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "85354-9"),
                    system = Uri(value = "http://loinc.org")
                )
            )
        )
        val mapping = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept,
            mockk<Observation>()
        )!!
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("85354-9"),
                        system = Uri("http://loinc.org"),
                        version = "0.0.1".asFHIR(),
                        display = "Blood pressure panel with all children optional".asFHIR()
                    ),
                    Coding(
                        code = Code("3141-9"),
                        system = Uri("http://loinc.org"),
                        version = "0.0.1".asFHIR(),
                        display = "Body weight Measured".asFHIR()
                    ),
                    Coding(
                        code = Code("55284-4"),
                        system = Uri("http://loinc.org"),
                        version = "0.0.1".asFHIR(),
                        display = "Blood pressure systolic and diastolic".asFHIR()
                    )
                ),
                text = "Blood pressure".asFHIR()
            ),
            mapping.codeableConcept
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept)
            ),
            mapping.extension
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element codes from a staging map - source Coding has 1 member - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "registry-uuid",
                filename = "file1.json",
                concept_map_name = "TestObservationsStaging",
                concept_map_uuid = "TestObservationsStaging-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testStagingConceptMap

        // ' in Coding.display, ' in text
        // {[{EPIC#44065, Clark's level, urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - PROGNOSTIC INDICATORS - CLARK'S LEVEL}"
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "EPIC#44065"),
                    system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688")
                )
            )
        )
        val mapping = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept,
            mockk<Observation>()
        )!!
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("385347004"),
                        system = Uri("http://snomed.info/sct"),
                        version = "2023-03-01".asFHIR(),
                        display = "Clark melanoma level finding (finding)".asFHIR()
                    )
                ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - PROGNOSTIC INDICATORS - CLARK'S LEVEL".asFHIR()
            ),
            mapping.codeableConcept
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept)
            ),
            mapping.extension
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element codes from a staging map - source Coding has 2 members in order - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                filename = "file1.json",
                concept_map_name = "TestObservationsStaging",
                concept_map_uuid = "TestObservationsStaging-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testStagingConceptMap

        // / in Coding.display, 2 Coding
        // {[{SNOMED#246111003, null, http://snomed.info/sct}, {EPIC#42388, anatomic stage/prognostic group, urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP}
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "SNOMED#246111003"),
                    system = Uri(value = "http://snomed.info/sct")
                ),
                Coding(
                    code = Code(value = "EPIC#42388"),
                    system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688")
                )
            )
        )
        val mapping = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept,
            mockk<Observation>()
        )!!
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("246111003"),
                        system = Uri("http://snomed.info/sct"),
                        version = "2023-03-01".asFHIR(),
                        display = "Prognostic score (attribute)".asFHIR()
                    )
                ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP".asFHIR()
            ),
            mapping.codeableConcept
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept)
            ),
            mapping.extension
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element codes from a staging map - source Coding has 2 members out of order - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                filename = "file1.json",
                concept_map_name = "TestObservationsStaging",
                concept_map_uuid = "TestObservationsStaging-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testStagingConceptMap

        // / in Coding.display, 2 Coding out of order
        // {[{SNOMED#246111003, null, http://snomed.info/sct}, {EPIC#42388, anatomic stage/prognostic group, urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP}
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "EPIC#42388"),
                    system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688")
                ),
                Coding(
                    code = Code(value = "SNOMED#246111003"),
                    system = Uri(value = "http://snomed.info/sct")
                )
            )
        )
        val mapping = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept,
            mockk<Observation>()
        )!!
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("246111003"),
                        system = Uri("http://snomed.info/sct"),
                        version = "2023-03-01".asFHIR(),
                        display = "Prognostic score (attribute)".asFHIR()
                    )
                ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP".asFHIR()
            ),
            mapping.codeableConcept
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept)
            ),
            mapping.extension
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element codes from a staging map - more punctuation cases - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                filename = "file1.json",
                concept_map_name = "TestObservationsStaging",
                concept_map_uuid = "TestObservationsStaging-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testStagingConceptMap

        // () in Coding.display, () in text
        // {[{EPIC#42391, lymph-vascular invasion (LVI), urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - LYMPH-VASCULAR INVASION (LVI)}
        val concept1 = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "EPIC#42391"),
                    system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688")
                )
            )
        )
        val mapping1 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept1,
            mockk<Observation>()
        )!!
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("371512006"),
                        system = Uri("http://snomed.info/sct"),
                        version = "2023-03-01".asFHIR(),
                        display = "Presence of direct invasion by primary malignant neoplasm to lymphatic vessel and/or small blood vessel (observable entity)".asFHIR()
                    )
                ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - LYMPH-VASCULAR INVASION (LVI)".asFHIR()
            ),
            mapping1.codeableConcept
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept1)
            ),
            mapping1.extension
        )

        // (/) in Coding.display, / (/) in ntext
        // {[{EPIC#31000073346, WHO/ISUP grade (low/high), urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - WHO/ISUP GRADE (LOW/HIGH)}
        val concept2 = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "EPIC#31000073346"),
                    system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688")
                )
            )
        )
        val mapping2 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept2,
            mockk<Observation>()
        )!!
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("396659000"),
                        system = Uri("http://snomed.info/sct"),
                        version = "2023-03-01".asFHIR(),
                        display = "Histologic grade of urothelial carcinoma by World Health Organization and International Society of Urological Pathology technique (observable entity)".asFHIR()
                    )
                ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - WHO/ISUP GRADE (LOW/HIGH)".asFHIR()
            ),
            mapping2.codeableConcept
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept2)
            ),
            mapping2.extension
        )

        // () and - in Coding.display, 2 Coding, nothing found in map, / () in text
        // unexpected spacing within group.element.code
        //    "code": " { [ { SNOMED#260767000 ,null , http://snomed.info/sct} ,{EPIC#42384     , regional lymph nodes (N),urn:oid:1.2.840.114350.1.13.297.2.7.2.727688   } ] , FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - TNM CLASSIFICATION - AJCC N - REGIONAL LYMPH NODES (N)   }   "
        val concept3 = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "SNOMED#260767000"),
                    system = Uri(value = "http://snomed.info/sct")
                ),
                Coding(
                    code = Code(value = "EPIC#42384"),
                    // code = Code(value = "EPIC#442384"),
                    system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688")
                )
            )
        )
        val mapping3 = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept3,
            mockk<Observation>()
        )!!
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("260767000"),
                        system = Uri("http://snomed.info/sct"),
                        version = "2023-03-01".asFHIR(),
                        display = "N - Regional lymph node stage (attribute)".asFHIR()
                    )

                ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - TNM CLASSIFICATION - AJCC N - REGIONAL LYMPH NODES (N)".asFHIR()
            ),
            mapping3.codeableConcept
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept3)
            ),
            mapping3.extension
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element codes from a staging map - source Coding has 2 members in order - match not found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                filename = "file1.json",
                concept_map_name = "TestObservationsStaging",
                concept_map_uuid = "TestObservationsStaging-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testStagingConceptMap

        // / in Coding.display, 2 Coding, 1 with typo in code value
        // {[{SNOMED#246111003, null, http://snomed.info/sct}, {EPIC#42388, anatomic stage/prognostic group, urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP}
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "SNOMED#246111003"),
                    system = Uri(value = "http://snomed.info/sct")
                ),
                Coding(
                    // deliberate typo (hard to see)
                    code = Code(value = "EPIC#442388"),
                    system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688")
                )
            )
        )
        val mapping = client.getConceptMapping(
            tenant,
            "Observation.code",
            concept,
            mockk<Observation>()
        )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMapping for CodeableConcept concatenates multiple matching concept maps - errors if URLs do not agree`() {
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "11111",
                filename = "file1.json",
                concept_map_name = "Staging-test-1",
                concept_map_uuid = "cm-111",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-1",
                resource_type = "Observation",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "22222",
                filename = "file2.json",
                concept_map_name = "AllVitals-test-2",
                concept_map_uuid = "cm-222",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-2",
                resource_type = "Observation",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "33333",
                filename = "file3.json",
                concept_map_name = "HeartRate-test-3",
                concept_map_uuid = "cm-333",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ObservationCode-3",
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        val mockkMap1 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem-1"
                    every { targetVersion?.value } returns "targetVersion-1"
                    every { source?.value } returns "system-Staging-1"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueA"
                            every { display?.value } returns "targetTextAAA"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueAAA"
                                    every { display?.value } returns "targetDisplayAAA"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueB"
                            every { display?.value } returns "targetTextBBB"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueBBB"
                                    every { display?.value } returns "targetDisplayBBB"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        val mockkMap2 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem-2"
                    every { targetVersion?.value } returns "targetVersion-2"
                    every { source?.value } returns "system-AllVitals-2"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueX"
                            every { display?.value } returns "targetTextXXX"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueXXX"
                                    every { display?.value } returns "targetDisplayXXX"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueY"
                            every { display?.value } returns "targetTextYYY"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueYYY"
                                    every { display?.value } returns "targetDisplayYYY"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueZ"
                            every { display?.value } returns "targetTextZZZ"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueZZZ"
                                    every { display?.value } returns "targetDisplayZZZ"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        val mockkMap3 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem-3"
                    every { targetVersion?.value } returns "targetVersion-3"
                    every { source?.value } returns "system-HeartRate-3"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueC"
                            every { display?.value } returns "targetTextCCC"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueCCC"
                                    every { display?.value } returns "targetDisplayCCC"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueD"
                            every { display?.value } returns "targetTextDDD"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueDDD"
                                    every { display?.value } returns "targetDisplayDDD"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        every { ociClient.getObjectFromINFX("file3.json") } returns "mapJson3"
        every { JacksonUtil.readJsonObject("mapJson3", ConceptMap::class) } returns mockkMap3
        val coding = Coding(
            code = Code(value = "sourceValueB"),
            system = Uri(value = "system-Staging-1")
        )
        val concept = CodeableConcept(
            text = "ignore-me-1".asFHIR(),
            coding = listOf(coding)
        )
        val exception = assertThrows<MissingNormalizationContentException> {
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>()
            )
        }

        assertEquals(
            "Concept map(s) for tenant 'test' and Observation.code have missing or inconsistent source extension URLs",
            exception.message
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - group element code with no system`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "registry-uuid",
                filename = "file1.json",
                concept_map_name = "TestObservationsMashup",
                concept_map_uuid = "TestObservationsMashup-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns """
            {
              "resourceType": "ConceptMap",
              "title": "Test Observations Mashup (for Dev Testing ONLY)",
              "id": "TestObservationsMashup-id",
              "name": "TestObservationsMashup-name",
              "contact": [
                {
                  "name": "Interops (for Dev Testing ONLY)"
                }
              ],
              "url": "http://projectronin.io/fhir/StructureDefinition/ConceptMap/TestObservationsMashup",
              "description": "Interops  (for Dev Testing ONLY)",
              "purpose": "Testing",
              "publisher": "Project Ronin",
              "experimental": true,
              "date": "2023-05-26",
              "version": 1,
              "group": [
                {
                  "source": "http://projectronin.io/fhir/CodeSystem/test/TestObservationsMashup",
                  "sourceVersion": "1.0",
                  "target": "http://loinc.org",
                  "targetVersion": "0.0.1",
                  "element": [
                    {
                      "id": "06c19b8e4718f1bf6e81f992cfc12c1e",
                      "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"72166-2\", \"display\": null, \"system\": null}]}}",
                      "display": "Tobacco smoking status",
                      "target": [
                        {
                          "id": "836cc342c39afcc7a8dee6277abc7b75",
                          "code": "72166-2",
                          "display": "Tobacco smoking status",
                          "equivalence": "equivalent",
                          "comment": null
                        }
                      ]
                    }
                  ]
                }
              ],
              "extension": [
                {
                  "url": "http://projectronin.io/fhir/StructureDefinition/Extension/ronin-conceptMapSchema",
                  "valueString": "1.0.0"
                }
              ],
              "meta": {
                "lastUpdated": "2023-05-26T12:49:56.285403+00:00"
              }
        }
        """.trimIndent()
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "85354-9"),
                    system = Uri(value = "http://loinc.org")
                )
            )
        )
        val exception =
            assertThrows<IllegalStateException> {
                client.getConceptMapping(
                    tenant,
                    "Observation.code",
                    concept,
                    mockk<Observation>()
                )
            }
        assertEquals(
            """Could not create SourceConcept from {"valueCodeableConcept": {"coding": [{"code": "72166-2", "display": null, "system": null}]}}""",
            exception.message
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - group element code with no code`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "registry-uuid",
                filename = "file1.json",
                concept_map_name = "TestObservationsMashup",
                concept_map_uuid = "TestObservationsMashup-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns """
            {
              "resourceType": "ConceptMap",
              "title": "Test Observations Mashup (for Dev Testing ONLY)",
              "id": "TestObservationsMashup-id",
              "name": "TestObservationsMashup-name",
              "contact": [
                {
                  "name": "Interops (for Dev Testing ONLY)"
                }
              ],
              "url": "http://projectronin.io/fhir/StructureDefinition/ConceptMap/TestObservationsMashup",
              "description": "Interops  (for Dev Testing ONLY)",
              "purpose": "Testing",
              "publisher": "Project Ronin",
              "experimental": true,
              "date": "2023-05-26",
              "version": 1,
              "group": [
                {
                  "source": "http://projectronin.io/fhir/CodeSystem/test/TestObservationsMashup",
                  "sourceVersion": "1.0",
                  "target": "http://loinc.org",
                  "targetVersion": "0.0.1",
                  "element": [
                    {
                      "id": "06c19b8e4718f1bf6e81f992cfc12c1e",
                      "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": null, \"display\": null, \"system\": \"system\"}]}}",
                      "display": "Tobacco smoking status",
                      "target": [
                        {
                          "id": "836cc342c39afcc7a8dee6277abc7b75",
                          "code": "72166-2",
                          "display": "Tobacco smoking status",
                          "equivalence": "equivalent",
                          "comment": null
                        }
                      ]
                    }
                  ]
                }
              ],
              "extension": [
                {
                  "url": "http://projectronin.io/fhir/StructureDefinition/Extension/ronin-conceptMapSchema",
                  "valueString": "1.0.0"
                }
              ],
              "meta": {
                "lastUpdated": "2023-05-26T12:49:56.285403+00:00"
              }
        }
        """.trimIndent()
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "85354-9"),
                    system = Uri(value = "http://loinc.org")
                )
            )
        )
        val exception =
            assertThrows<IllegalStateException> {
                client.getConceptMapping(
                    tenant,
                    "Observation.code",
                    concept,
                    mockk<Observation>()
                )
            }
        assertEquals(
            """Could not create SourceConcept from {"valueCodeableConcept": {"coding": [{"code": null, "display": null, "system": "system"}]}}""",
            exception.message
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - source code contains no valueCodeableConcept attribute wrapper`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Observation.code",
                registry_uuid = "registry-uuid",
                filename = "file1.json",
                concept_map_name = "TestObservationsMashup",
                concept_map_uuid = "TestObservationsMashup-uuid",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = sourceUrl,
                resource_type = "Observation",
                tenant_id = "test"
            )
        )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns """
            {
              "resourceType": "ConceptMap",
              "title": "Test Observations Mashup (for Dev Testing ONLY)",
              "id": "TestObservationsMashup-id",
              "name": "TestObservationsMashup-name",
              "contact": [
                {
                  "name": "Interops (for Dev Testing ONLY)"
                }
              ],
              "url": "http://projectronin.io/fhir/StructureDefinition/ConceptMap/TestObservationsMashup",
              "description": "Interops  (for Dev Testing ONLY)",
              "purpose": "Testing",
              "publisher": "Project Ronin",
              "experimental": true,
              "date": "2023-05-26",
              "version": 1,
              "group": [
                {
                  "source": "http://projectronin.io/fhir/CodeSystem/test/TestObservationsMashup",
                  "sourceVersion": "1.0",
                  "target": "http://loinc.org",
                  "targetVersion": "0.0.1",
                  "element": [
                    {
                      "id": "06c19b8e4718f1bf6e81f992cfc12c1e",
                      "code": "{\"coding\": [{\"code\": \"363905002\", \"display\": \"Details of alcohol drinking behavior (observable entity)\", \"system\": \"http://snomed.info/sct\"}]}",
                      "display": "Tobacco smoking status",
                      "target": [
                        {
                          "id": "836cc342c39afcc7a8dee6277abc7b75",
                          "code": "72166-2",
                          "display": "Tobacco smoking status",
                          "equivalence": "equivalent",
                          "comment": null
                        }
                      ]
                    }
                  ]
                }
              ],
              "extension": [
                {
                  "url": "http://projectronin.io/fhir/StructureDefinition/Extension/ronin-conceptMapSchema",
                  "valueString": "1.0.0"
                }
              ],
              "meta": {
                "lastUpdated": "2023-05-26T12:49:56.285403+00:00"
              }
        }
        """.trimIndent()
        val concept = CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code(value = "363905002"),
                    system = Uri(value = "http://snomed.info/sct")
                )
            )
        )

        val mapping = client.getConceptMapping(tenant, "Observation.code", concept, mockk<Observation>())
        assertNotNull(mapping)
        assertEquals("72166-2", mapping?.codeableConcept?.coding?.first()?.code?.value)
    }

    @Test
    fun `getConceptMapping for Coding pulls new registry and maps when a single map is out-of-date`() {
        val cmTestRegistry = listOf(
            NormalizationRegistryItem(
                data_element = "Appointment.status",
                registry_uuid = "12345",
                filename = "file1.json",
                concept_map_name = "AppointmentStatus-tenant",
                concept_map_uuid = "cm-111",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ext1",
                resource_type = "Appointment",
                tenant_id = "test"
            ),
            NormalizationRegistryItem(
                data_element = "Patient.telecom.use",
                registry_uuid = "67890",
                filename = "file2.json",
                concept_map_name = "PatientTelecomUse-tenant",
                concept_map_uuid = "cm-222",
                registry_entry_type = "concept_map",
                version = "1",
                source_extension_url = "ext2",
                resource_type = "Patient",
                tenant_id = "test"
            )
        )
        val mockkMap1 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystemAAA"
                    every { targetVersion?.value } returns "targetVersionAAA"
                    every { source?.value } returns "sourceSystemA"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValueA"
                            every { display?.value } returns "targetTextAAA"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueAAA"
                                    every { display?.value } returns "targetDisplayAAA"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        },
                        mockk {
                            every { code?.value } returns "sourceValueB"
                            every { display?.value } returns "targetTextBBB"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValueBBB"
                                    every { display?.value } returns "targetDisplayBBB"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        val mockkMap2 = mockk<ConceptMap> {
            every { group } returns listOf(
                mockk {
                    every { target?.value } returns "targetSystem222"
                    every { targetVersion?.value } returns "targetVersion222"
                    every { source?.value } returns "sourceSystem2"
                    every { element } returns listOf(
                        mockk {
                            every { code?.value } returns "sourceValue2"
                            every { display?.value } returns "targetText222"
                            every { target } returns listOf(
                                mockk {
                                    every { code?.value } returns "targetValue222"
                                    every { display?.value } returns "targetDisplay222"
                                    every { dependsOn } returns emptyList()
                                }
                            )
                        }
                    )
                }
            )
        }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every { JacksonUtil.readJsonList("registryJson", NormalizationRegistryItem::class) } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        val key1 = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Appointment.status",
            "test"
        )
        client.conceptMapCache.put(key1, mockk(relaxed = true))
        val key1LastUpdated = LocalDateTime.now().minusMinutes(25)
        client.itemLastUpdated[key1] = key1LastUpdated

        val key2 = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Patient.telecom.use",
            "test"
        )
        client.conceptMapCache.put(key2, mockk(relaxed = true))
        val key2LastUpdated = LocalDateTime.now().minusDays(2)
        client.itemLastUpdated[key2] = key2LastUpdated

        val registryLastUpdated = LocalDateTime.now().minusHours(1)
        client.registryLastUpdated = registryLastUpdated

        val coding1 = Coding(
            code = Code(value = "sourceValueA"),
            system = Uri(value = "sourceSystemA")
        )

        // We don't care about the mapping for this test -- really this is because we're using a mock registry, and thus nothing of actual value is present.
        client.getConceptMapping(
            tenant,
            "Appointment.status",
            coding1,
            mockk<Appointment>()
        )

        // Appointment.status did not trigger an item reload.
        assertEquals(key1LastUpdated, client.itemLastUpdated[key1])
        assertEquals(key2LastUpdated, client.itemLastUpdated[key2])
        assertEquals(registryLastUpdated, client.registryLastUpdated)

        val coding2 = Coding(
            code = Code(value = "sourceValue2"),
            system = Uri(value = "sourceSystem2")
        )
        val mapping2 =
            client.getConceptMapping(
                tenant,
                "Patient.telecom.use",
                coding2,
                mockk<Patient>()
            )!!
        assertEquals(mapping2.coding.code!!.value, "targetValue222")
        assertEquals(mapping2.coding.system!!.value, "targetSystem222")
        assertEquals(mapping2.extension.url!!.value, "ext2")
        assertEquals(mapping2.extension.value!!.value, coding2)

        // Patient.telecom.use did trigger an item reload.
        assertNull(client.itemLastUpdated[key1])
        assertTrue(client.itemLastUpdated[key2]!!.isAfter(key2LastUpdated))
        assertTrue(client.registryLastUpdated.isAfter(registryLastUpdated))
    }

    private val dependsOn = ConceptMapDependsOn(
        property = Uri("medication.form"),
        value = FHIRString("some-form-value")
    )
    private val dependsOn2 = ConceptMapDependsOn(
        property = Uri("medication.amount"),
        value = FHIRString("some-amount-value")
    )
    private val mappingWithDependsOnTargets =
        SourceConcept(
            element = setOf(
                SourceKey(
                    value = "valueA",
                    system = "systemA"
                )
            )
        ) to listOf(
            TargetConcept(
                text = "textAAA",
                element = listOf(
                    TargetValue(
                        "targetValueAAA",
                        "targetSystemAAA",
                        "targetDisplayAAA",
                        "targetVersionAAA",
                        listOf(dependsOn)
                    )
                )
            ),
            TargetConcept(
                text = "textBBB",
                element = listOf(
                    TargetValue(
                        "targetValueBBB",
                        "targetSystemBBB",
                        "targetDisplayBBB",
                        "targetVersionBBB",
                        listOf(dependsOn2)
                    )
                )
            )
        )

    @Test
    fun `dependsOnEvaluator not found for target with no dependsOn data`() {
        val registry = ConceptMapItem(
            source_extension_url = "ext-AB",
            map = mapAB,
            metadata = listOf(conceptMapMetadata)
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key] = LocalDateTime.now()

        val sourceCoding = Coding(
            system = Uri("systemA"),
            code = Code("valueA")
        )
        val targetCoding = Coding(
            system = Uri("targetSystemAAA"),
            code = Code("targetValueAAA"),
            display = "targetDisplayAAA".asFHIR(),
            version = "targetVersionAAA".asFHIR()
        )
        val targetSourceExtension = Extension(
            url = Uri(value = "ext-AB"),
            value = DynamicValue(
                type = DynamicValueType.CODING,
                value = sourceCoding
            )
        )
        val mappedResult = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceCoding,
            mockk<Observation>()
        )
        assertEquals(
            targetCoding,
            mappedResult?.coding
        )
        assertEquals(
            targetSourceExtension,
            mappedResult?.extension
        )

        verify(exactly = 0) { medicationDependsOnEvaluator.meetsDependsOn(any(), any()) }
    }

    @Test
    fun `dependsOnEvaluator found for target with no dependsOn data`() {
        val registry = ConceptMapItem(
            source_extension_url = "ext-AB",
            map = mapAB,
            metadata = listOf(conceptMapMetadata)
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Medication.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key] = LocalDateTime.now()

        val sourceCoding = Coding(
            system = Uri("systemA"),
            code = Code("valueA")
        )
        val targetCoding = Coding(
            system = Uri("targetSystemAAA"),
            code = Code("targetValueAAA"),
            display = "targetDisplayAAA".asFHIR(),
            version = "targetVersionAAA".asFHIR()
        )
        val targetSourceExtension = Extension(
            url = Uri(value = "ext-AB"),
            value = DynamicValue(
                type = DynamicValueType.CODING,
                value = sourceCoding
            )
        )
        val mappedResult = client.getConceptMapping(
            tenant,
            "Medication.code",
            sourceCoding,
            mockk<Medication>()
        )
        assertEquals(
            targetCoding,
            mappedResult?.coding
        )
        assertEquals(
            targetSourceExtension,
            mappedResult?.extension
        )

        verify(exactly = 0) { medicationDependsOnEvaluator.meetsDependsOn(any(), any()) }
    }

    @Test
    fun `dependsOnEvaluator not found for target with dependsOn data`() {
        val registry = ConceptMapItem(
            source_extension_url = "ext-AB",
            map = mapOf(mappingWithDependsOnTargets),
            metadata = listOf(conceptMapMetadata)
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key] = LocalDateTime.now()

        val sourceCoding = Coding(
            system = Uri("systemA"),
            code = Code("valueA")
        )
        val mappedResult = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceCoding,
            mockk<Observation>()
        )
        assertNull(mappedResult)

        verify(exactly = 0) { medicationDependsOnEvaluator.meetsDependsOn(any(), any()) }
    }

    @Test
    fun `dependsOnEvaluator found and no target dependsOn is met`() {
        val registry = ConceptMapItem(
            source_extension_url = "ext-AB",
            map = mapOf(mappingWithDependsOnTargets),
            metadata = listOf(conceptMapMetadata)
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key] = LocalDateTime.now()

        val medication = mockk<Medication>()

        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) } returns false
        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) } returns false

        val sourceCoding = Coding(
            system = Uri("systemA"),
            code = Code("valueA")
        )
        val mappedResult = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceCoding,
            medication
        )
        assertNull(mappedResult)

        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) }
        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) }
    }

    @Test
    fun `dependsOnEvaluator found and single target dependsOn is met`() {
        val registry = ConceptMapItem(
            source_extension_url = "ext-AB",
            map = mapOf(mappingWithDependsOnTargets),
            metadata = listOf(conceptMapMetadata)
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key] = LocalDateTime.now()

        val medication = mockk<Medication>()

        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) } returns true
        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) } returns false

        val sourceCoding = Coding(
            system = Uri("systemA"),
            code = Code("valueA")
        )
        val targetCoding = Coding(
            system = Uri("targetSystemAAA"),
            code = Code("targetValueAAA"),
            display = "targetDisplayAAA".asFHIR(),
            version = "targetVersionAAA".asFHIR()
        )
        val targetSourceExtension = Extension(
            url = Uri(value = "ext-AB"),
            value = DynamicValue(
                type = DynamicValueType.CODING,
                value = sourceCoding
            )
        )
        val mappedResult = client.getConceptMapping(
            tenant,
            "Observation.code",
            sourceCoding,
            medication
        )
        assertEquals(
            targetCoding,
            mappedResult?.coding
        )
        assertEquals(
            targetSourceExtension,
            mappedResult?.extension
        )

        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) }
        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) }
    }

    @Test
    fun `dependsOnEvaluator found and multiple target dependsOn are met`() {
        val registry = ConceptMapItem(
            source_extension_url = "ext-AB",
            map = mapOf(mappingWithDependsOnTargets),
            metadata = listOf(conceptMapMetadata)
        )
        val key = CacheKey(
            NormalizationRegistryItem.RegistryType.ConceptMap,
            "Observation.code",
            tenant.mnemonic
        )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()
        client.itemLastUpdated[key] = LocalDateTime.now()

        val medication = mockk<Medication>()

        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) } returns true
        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) } returns true

        val sourceCoding = Coding(
            system = Uri("systemA"),
            code = Code("valueA")
        )
        val targetCoding = Coding(
            system = Uri("targetSystemAAA"),
            code = Code("targetValueAAA"),
            display = "targetDisplayAAA".asFHIR(),
            version = "targetVersionAAA".asFHIR()
        )
        val targetSourceExtension = Extension(
            url = Uri(value = "ext-AB"),
            value = DynamicValue(
                type = DynamicValueType.CODING,
                value = sourceCoding
            )
        )
        val exception = assertThrows<IllegalStateException> {
            client.getConceptMapping(
                tenant,
                "Observation.code",
                sourceCoding,
                medication
            )
        }
        assertTrue(exception.message?.startsWith("Multiple qualified TargetConcepts found for") ?: false)

        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) }
        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) }
    }
}
