package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.resources.ObservationGenerator
import com.projectronin.interop.fhir.generators.resources.observation
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.generators.util.generateCode
import com.projectronin.interop.fhir.ronin.generators.util.generateExtension
import com.projectronin.interop.fhir.ronin.generators.util.generateSubject
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

/**
 * Helps generate ronin staging related observation profile  applies meta and randomly generates an
 * acceptable code from the [possibleStagingRelatedCodes] list  category is generated by base-observation
 */
fun rcdmObservationStagingRelated(tenant: String, block: ObservationGenerator.() -> Unit): Observation {
    return observation {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.OBSERVATION_STAGING_RELATED, tenant) {}
        identifier of identifier.generate() + rcdmIdentifiers(tenant, identifier)
        extension of generateExtension(extension.generate(), tenantSourceExtension)
        category of listOf(
            codeableConcept {
                coding of stagingRelatedCategory
            }
        )
        code of generateCode(code.generate(), possibleStagingRelatedCodes.random())
        subject of generateSubject(subject.generate(), subjectStagingReferenceOptions)
    }
}

private val stagingRelatedCategory = listOf(
    coding {
        system of Uri("staging-related-uri")
        code of Code("staging-related")
    }
)

val possibleStagingRelatedCodes = listOf(
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222770000")
        display of "American Joint Committee on Cancer stage IIC (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222734003")
        display of "American Joint Committee on Cancer stage I:0 (qualifier value)"
    },
    coding {
        system of "http://loinc.org"
        version of "2.74"
        code of Code("21908-9")
        display of "Stage group.clinical Cancer"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222812008")
        display of "American Joint Committee on Cancer stage III:1 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("60333009")
        display of "Clinical stage II (finding)"
    },
    coding {
        system of "http://snomed.info/sctersion "
        version of "2023-03-01"
        code of Code("1222779004")
        display of "American Joint Committee on Cancer stage II:6 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222752003")
        display of "American Joint Committee on Cancer stage I:13 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222870008")
        display of "American Joint Committee on Cancer stage IV:25 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222741009")
        display of "American Joint Committee on Cancer stage I:4 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222778007")
        display of "American Joint Committee on Cancer stage II:5 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222822002")
        display of "American Joint Committee on Cancer stage III:11 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222797007")
        display of "American Joint Committee on Cancer stage II:22 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222860006")
        display of "American Joint Committee on Cancer stage IV:16 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222742002")
        display of "American Joint Committee on Cancer stage I:5 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222824001")
        display of "American Joint Committee on Cancer stage III:13 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222790009")
        display of "American Joint Committee on Cancer stage II:16 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222830001")
        display of "American Joint Committee on Cancer stage III:19 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("2640006")
        display of "Clinical stage IV (finding)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222761003")
        display of "American Joint Committee on Cancer stage I:22 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222762005")
        display of "American Joint Committee on Cancer stage I:23 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222766008")
        display of "American Joint Committee on Cancer stage IIA (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222813003")
        display of "American Joint Committee on Cancer stage III:2 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222788008")
        display of "American Joint Committee on Cancer stage II:14 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222815005")
        display of "American Joint Committee on Cancer stage III:4 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222777002")
        display of "American Joint Committee on Cancer stage II:4 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222804000")
        display of "American Joint Committee on Cancer stage IIIA2 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222775005")
        display of "American Joint Committee on Cancer stage II:2 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222866000")
        display of "American Joint Committee on Cancer stage IV:21 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222847000")
        display of "American Joint Committee on Cancer stage IV:4 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222819004")
        display of "American Joint Committee on Cancer stage III:8 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222810000")
        display of "American Joint Committee on Cancer stage III:0 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222763000")
        display of "American Joint Committee on Cancer stage I:24 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222781002")
        display of "American Joint Committee on Cancer stage II:8 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222827008")
        display of "American Joint Committee on Cancer stage III:16 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222758004")
        display of "American Joint Committee on Cancer stage I:19 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222862003")
        display of "American Joint Committee on Cancer stage IV:18 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222737005")
        display of "American Joint Committee on Cancer stage I:1 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222820005")
        display of "American Joint Committee on Cancer stage III:9 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222791008")
        display of "American Joint Committee on Cancer stage II:17 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222816006")
        display of "American Joint Committee on Cancer stage III:5 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222851003")
        display of "American Joint Committee on Cancer stage IV:8 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("260878002")
        display of "T - Tumor stage (attribute)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222834005")
        display of "American Joint Committee on Cancer stage III:23 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222746004")
        display of "American Joint Committee on Cancer stage I:7 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222818007")
        display of "American Joint Committee on Cancer stage III:7 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222849002")
        display of "American Joint Committee on Cancer stage IV:6 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("260767000")
        display of "N - Regional lymph node stage (attribute)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222869007")
        display of "American Joint Committee on Cancer stage IV:24 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222792001")
        display of "American Joint Committee on Cancer stage II:18 (qualifier value)"
    },
    coding {
        system of "http://snomed.info/sct"
        version of "2023-03-01"
        code of Code("1222832009")
        display of "American Joint Committee on Cancer stage III:21 (qualifier value)"
    }
)
