package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.primitives.UriGenerator
import com.projectronin.interop.fhir.generators.resources.MedicationGenerator
import com.projectronin.interop.fhir.generators.resources.medication
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.ronin.generators.util.generateCode
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.test.data.generator.faker.WordGenerator

fun rcdmMedication(
    tenant: String,
    block: MedicationGenerator.() -> Unit,
): Medication {
    val medCode = rcdmMedicationCode()
    return medication {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.MEDICATION, tenant) {}
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        code of medCode
        status of generateCode(status.generate(), possibleMedicationStatusCodes.random())
        // The tenantSourceMedicationCode must contain the code's text
        extension.plus(tenantSourceMedicationCode(medCode))
    }
}

val possibleMedicationStatusCodes =
    listOf(
        Code("active"),
        Code("inactive"),
        Code("entered-in-error"),
    )

fun tenantSourceMedicationCode(codeableConcept: CodeableConcept): Extension {
    return Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
        value =
            DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                codeableConcept,
            ),
    )
}

fun rcdmMedicationCode(): CodeableConcept =
    CodeableConcept(
        coding =
            listOf(
                Coding(
                    system = UriGenerator().generate(),
                    code = Code(WordGenerator().generate()),
                    display = WordGenerator().generate().asFHIR(),
                ),
            ),
        text = WordGenerator().generate().asFHIR(),
    )
