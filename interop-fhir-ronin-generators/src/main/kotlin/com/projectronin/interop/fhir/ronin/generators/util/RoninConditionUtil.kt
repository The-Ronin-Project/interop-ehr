package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.normalization.ValueSetList
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.validation.ValueSetMetadata

val tenantSourceConditionExtension = listOf(
    Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_CONDITION_CODE.value),
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            CodeableConcept(
                text = "tenant-source-extension".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("tenant-source-code-extension")
                    )
                )
            )
        )
    )
)

val conditionCodeExtension = Extension(
    url = Uri(RoninExtension.TENANT_SOURCE_CONDITION_CODE.value),
    value = DynamicValue(
        DynamicValueType.CODEABLE_CONCEPT,
        value = CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://snomed.info/sct"),
                    code = Code("1023001"),
                    display = "Apnea".asFHIR()
                )
            )
        )
    )
)

val encounterDiagnosisCategory =
    coding {
        system of CodeSystem.CONDITION_CATEGORY.uri
        code of Code("encounter-diagnosis")
    }

val subjectOptions = listOf("Patient")

val problemListCategory =
    coding {
        system of CodeSystem.CONDITION_CATEGORY.uri
        code of Code("problem-list-item")
    }

val healthConcernCategory =
    coding {
        system of CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri
        code of Code("health-concern")
    }

val possibleConditionCodesList = listOf(
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("S85.401A")
        display of "Unspecified injury of lesser saphenous vein at lower leg level, right leg, initial encounter"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("439365009")
        display of "Closed manual reduction of fracture of posterior malleolus (procedure)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("62850006")
        display of "Medical examination under sedation (procedure)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("722677008")
        display of "Primary mesothelioma of overlapping sites of retroperitoneum, peritoneum and omentum (disorder)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("T46.2X2S")
        display of "Poisoning by other antidysrhythmic drugs, intentional self-harm, sequela"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("283444006")
        display of "Cut of heel (disorder)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("212011007")
        display of "Corrosion of second degree of ankle and foot (disorder)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("H65.04")
        display of "Acute serous otitis media, recurrent, right ear"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("4688008")
        display of "Poisoning caused by griseofulvin (disorder)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("109818006")
        display of "Male frozen pelvis (disorder)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("S82.846P")
        display of "Nondisplaced bimalleolar fracture of unspecified lower leg, subsequent encounter for closed fracture with malunion"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("662911000124103")
        display of "Referral to Affordable Connectivity Program (procedure)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("S20.322")
        display of "Blister (nonthermal) of left front wall of thorax"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("S95.011S")
        display of "Laceration of dorsal artery of right foot, sequela"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("735764005")
        display of "Laceration of thorax without foreign body (disorder)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("704100003")
        display of "Endoscopic extraction of calculus of urinary tract proper (procedure)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("399418000")
        display of "Scleral invasion by tumor cannot be determined (finding)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("16715971000119108")
        display of "Acute infarction of small intestine (disorder)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("238432007")
        display of "Bacillus Calmette-Guerin ulcer (disorder)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("V90.01XA")
        display of "Drowning and submersion due to passenger ship overturning, initial encounter"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("T63.5")
        display of "Toxic effect of contact with venomous fish"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("698887005")
        display of "Main spoken language Aymara (finding)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("S52.333N")
        display of "Displaced oblique fracture of shaft of unspecified radius, subsequent encounter for open fracture type IIIA, IIIB, or IIIC with nonunion"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("1162729000")
        display of "Modification of nutrition intake schedule to limit fasting (regime/therapy)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("233294005")
        display of "Removal of foreign body from arterial graft (procedure)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("T50.903")
        display of "Poisoning by unspecified drugs, medicaments and biological substances, assault"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("S04.71XS")
        display of "Injury of accessory nerve, right side, sequela"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("446722008")
        display of "Determination of coreceptor tropism of Human immunodeficiency virus 1 (procedure)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("105362001")
        display of "Urinalysis, automated, without microscopy (procedure)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("298305003")
        display of "Finding of general balance (finding)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("196333005")
        display of "Mummified pulp (disorder)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("310538001")
        display of "Baby birth weight 2 to 2.5 kilogram (finding)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("34663006")
        display of "Contusion of brain (disorder)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("609468001")
        display of "Induced termination of pregnancy complicated by laceration of broad ligament (disorder)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("219375003")
        display of "War injury due to carbine bullet (disorder)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("88740003")
        display of "Thyrotoxicosis factitia with thyrotoxic crisis (disorder)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("T49.1X1A")
        display of "Poisoning by antipruritics, accidental (unintentional), initial encounter"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("I80.291")
        display of "Phlebitis and thrombophlebitis of other deep vessels of right lower extremity"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("174416009")
        display of "Closure of bowel fistula (procedure)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("T27.0XXS")
        display of "Burn of larynx and trachea, sequela"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("300658002")
        display of "Able to perform activities involved in using transport (finding)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("31957000")
        display of "Superficial injury of nose without infection (disorder)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("I70.448")
        display of "Atherosclerosis of autologous vein bypass graft(s) of the left leg with ulceration of other part of lower leg"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("1003397007")
        display of "Congenital atresia of intestine at multiple levels (disorder)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("V72.9XXD")
        display of "Unspecified occupant of bus injured in collision with two- or three-wheeled motor vehicle in traffic accident, subsequent encounter"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("2065009")
        display of "Dominant hereditary optic atrophy (disorder)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("66424002")
        display of "Manual reduction of closed fracture of acetabulum and skeletal traction (procedure)"
    },
    coding {
        system of "http://hl7.org/fhir/sid/icd-10-cm"
        version of "2023"
        code of Code("T22.361S")
        display of "Burn of third degree of right scapular region, sequela"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("33490001")
        display of "Failed attempted abortion with fat embolism (disorder)"
    },
    coding {
        system of CodeSystem.SNOMED_CT.uri
        version of "2023-03-01"
        code of Code("442327001")
        display of "Twin liveborn born in hospital (situation)"
    }
)
val possibleConditionCodes = ValueSetList(
    possibleConditionCodesList,
    ValueSetMetadata(
        registryEntryType = "value_set",
        valueSetName = "RoninConditionCode",
        valueSetUuid = "201ad507-64f7-4429-810f-94bdbd51f80a",
        version = "4"
    )
)
