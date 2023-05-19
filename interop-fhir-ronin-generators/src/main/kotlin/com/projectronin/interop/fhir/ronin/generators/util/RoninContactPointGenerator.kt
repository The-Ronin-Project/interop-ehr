package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.test.data.generator.DataGenerator

class RoninContactPointGenerator : DataGenerator<ContactPoint>() {
    override fun generateInternal(): ContactPoint {
        // TODO so I don't break anything
        return ContactPoint()
    }
}
