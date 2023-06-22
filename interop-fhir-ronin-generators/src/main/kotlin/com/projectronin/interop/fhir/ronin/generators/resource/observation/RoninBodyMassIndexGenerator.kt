package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.resources.ObservationGenerator
import com.projectronin.interop.fhir.generators.resources.observation
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.generators.util.generateCodeableConcept
import com.projectronin.interop.fhir.ronin.generators.util.generateEffectiveDateTime
import com.projectronin.interop.fhir.ronin.generators.util.generateExtension
import com.projectronin.interop.fhir.ronin.generators.util.generateSubject
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

/**
 * Helps generate ronin body mass index observation profile, applies meta and randomly generates an
 * acceptable code from the [possibleBodyMassIndexCodes] list, category is generated by base-vital-signs
 */
fun rcdmObservationBodyMassIndex(tenant: String, block: ObservationGenerator.() -> Unit): Observation {
    return observation {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.OBSERVATION_BODY_MASS_INDEX, tenant) {}
        extension of generateExtension(extension.generate(), tenantSourceExtension)
        identifier of identifier.generate() + rcdmIdentifiers(tenant, identifier)
        category of listOf(codeableConcept { coding of vitalSignsCategory })
        code of generateCodeableConcept(code.generate(), possibleBodyMassIndexCodes.random())
        subject of generateSubject(subject.generate(), subjectReferenceOptions)
        effective of generateEffectiveDateTime(effective.generate(), possibleDateTime)
    }
}

val possibleBodyMassIndexCodes = listOf(
    coding {
        system of "http://loinc.org"
        version of "2.74"
        code of Code("39156-5")
        display of "Body mass index (BMI) [Ratio]"
    }
)
