package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.valueset.NameUse
import com.projectronin.test.data.generator.collection.ListDataGenerator

fun rcdmName(names: ListDataGenerator<HumanName>): List<HumanName> {
    val generatedNames = names.generate()
    // If list of names has no official name, set first name without a use code to be official name and return it
    // If list of names all have use codes but no official names, return original list, will hit validation error
    if (generatedNames.any { it.use == Code(NameUse.OFFICIAL.code) }) {
        return generatedNames
    } else {
        val setOfficial: HumanName? = generatedNames.firstOrNull { it.use?.value.isNullOrEmpty() }
        return if (setOfficial != null) {
            generatedNames +
                HumanName(
                    use = Code(NameUse.OFFICIAL.code),
                    text = setOfficial.text,
                    family = setOfficial.family,
                    given = setOfficial.given,
                    prefix = setOfficial.prefix,
                    suffix = setOfficial.suffix,
                    period = setOfficial.period,
                )
        } else {
            generatedNames
        }
    }
}
