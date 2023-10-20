package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

private val tenant = mockk<Tenant> {
    every { mnemonic } returns "test"
}

private val localizer = Localizer()

fun localizeReferenceTest(reference: Reference): Reference? {
    val localizeReferenceMethod = Localizer::class.functions.find { it.name == "localizeReference" }!!
    localizeReferenceMethod.isAccessible = true
    val localized = localizeReferenceMethod.call(localizer, reference, tenant) as? Reference
    localizeReferenceMethod.isAccessible = false
    return localized
}
