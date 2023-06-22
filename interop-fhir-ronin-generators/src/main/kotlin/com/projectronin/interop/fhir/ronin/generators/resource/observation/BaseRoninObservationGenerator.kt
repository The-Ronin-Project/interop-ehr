package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.dateTime
import com.projectronin.interop.fhir.generators.resources.ObservationGenerator
import com.projectronin.interop.fhir.generators.resources.observation
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.generators.util.generateCodeableConcept
import com.projectronin.interop.fhir.ronin.generators.util.generateSubject
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

/**
 * Base ronin observation profile, applies generic category and code for profile
 */
fun rcdmObservation(tenant: String, block: ObservationGenerator.() -> Unit): Observation {
    return observation {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.OBSERVATION, tenant) {}
        identifier of identifier.generate() + rcdmIdentifiers(tenant, identifier)
        category of listOf(
            codeableConcept {
                coding of observationCategory
            }
        )
        code of generateCodeableConcept(code.generate(), observationCode.random())
        subject of generateSubject(subject.generate(), subjectBaseReferenceOptions)
    }
}

private val observationCode = listOf(
    coding {
        system of Uri("http://snomed.info/sct")
        code of Code("160695008")
        display of "Transport too expensive"
    }
)

val vitalSignsCategory = listOf(
    coding {
        system of CodeSystem.OBSERVATION_CATEGORY.uri
        code of Code("vital-signs")
    }
)

val observationCategory = listOf(
    coding {
        system of CodeSystem.OBSERVATION_CATEGORY.uri
        code of Code("social-history")
    }
)

val subjectBaseReferenceOptions = listOf(
    rcdmReference("Patient", "123"),
    rcdmReference("Location", "123")
).random()

val subjectStagingReferenceOptions = listOf(
    rcdmReference("Patient", "123"),
    rcdmReference("Location", "123")
).random()

val subjectReferenceOptions = listOf(
    rcdmReference("Patient", "123")
).random()

val possibleDateTime = DynamicValue(DynamicValueType.DATE_TIME, dateTime { })

val tenantSourceExtension = listOf(
    Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
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
