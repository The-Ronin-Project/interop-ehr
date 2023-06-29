package com.projectronin.interop.fhir.ronin.generators.resource.condition

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.resources.ConditionGenerator
import com.projectronin.interop.fhir.generators.resources.condition
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.ronin.generators.util.encounterDiagnosisCategory
import com.projectronin.interop.fhir.ronin.generators.util.generateCodeableConcept
import com.projectronin.interop.fhir.ronin.generators.util.generateExtension
import com.projectronin.interop.fhir.ronin.generators.util.generateSubject
import com.projectronin.interop.fhir.ronin.generators.util.possibleConditionCodes
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.generators.util.subjectOptions
import com.projectronin.interop.fhir.ronin.generators.util.tenantSourceConditionExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

fun rcdmConditionEncounterDiagnosis(tenant: String, block: ConditionGenerator.() -> Unit): Condition {
    return condition {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS, tenant) {}
        extension of generateExtension(extension.generate(), tenantSourceConditionExtension)
        identifier of identifier.generate() + rcdmIdentifiers(tenant, identifier)
        category of category.generate() + listOf(codeableConcept { coding of listOf(encounterDiagnosisCategory) })
        code.generate()?.let { generateCodeableConcept(it, possibleConditionCodes.random()) }
            ?: (code of CodeableConcept(coding = listOf(possibleConditionCodes.random())))
        subject of generateSubject(subject.generate(), subjectOptions)
    }
}
