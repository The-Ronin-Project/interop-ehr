package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.ContactPointGenerator
import com.projectronin.interop.fhir.generators.datatypes.contactPoint
import com.projectronin.interop.fhir.generators.datatypes.extension
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.test.data.generator.collection.ListDataGenerator

fun rcdmContactPoint(tenant: String, contactPoints: ListDataGenerator<ContactPoint>): ListDataGenerator<ContactPoint> {
    val input = contactPoints.generate()
    val contactPointList = ListDataGenerator(0, ContactPointGenerator())
    input.forEach {
        // only set telecoms that have valid system and use values, if do not have valid system or use, null out the extensions and do not add it to return list, telecom is not a required field.
        var sysExt = systemExtension(it.system?.value.toString(), tenant)
        var useExt = useExtension(it.use?.value.toString(), tenant)
        if (listOf(it.system?.value, it.use?.value).any { it == null }) {
            sysExt = Extension()
            useExt = Extension()
        } else {
            if ((it.system?.extension?.contains(sysExt) == false) || (it.use?.extension?.contains(useExt) == false)) {
                contactPointList.plus(
                    contactPoint {
                        value of it.value!!.value!!.asFHIR()
                        system of Code(
                            value = it.system?.value,
                            extension = listOf(sysExt)
                        )
                        use of Code(
                            value = it.use?.value,
                            extension = listOf(useExt)
                        )
                    }
                )
            } else {
                contactPointList.plus(it)
            }
        }
    }
    return contactPointList
}

internal fun systemExtension(sysValue: String, tenant: String): Extension {
    return extension {
        url of Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value)
        value of DynamicValue(
            type = DynamicValueType.CODING,
            value = Coding(
                system = Uri(
                    value = "http://projectronin.io/fhir/CodeSystem/$tenant/ContactPointSystem"
                ),
                code = Code(value = sysValue)
            )
        )
    }
}

internal fun useExtension(useValue: String, tenant: String): Extension {
    return extension {
        url of Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_USE.value)
        value of DynamicValue(
            type = DynamicValueType.CODING,
            value = Coding(
                system = Uri(
                    value = "http://projectronin.io/fhir/CodeSystem/$tenant/ContactPointUse"
                ),
                code = Code(value = useValue)
            )
        )
    }
}
