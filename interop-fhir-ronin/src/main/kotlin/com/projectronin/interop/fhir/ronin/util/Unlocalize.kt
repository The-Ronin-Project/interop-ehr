package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Un-localizes the String relative to the [tenant]
 */
fun String.unlocalize(tenant: Tenant) = this.removePrefix("${tenant.mnemonic}-")

/**
 * Un-localizes the Id relative to the [tenant]
 */
fun Id.unlocalize(tenant: Tenant) = Id(value?.unlocalize(tenant), id, extension)

/**
 * Un-localizes each String value in the list, relative to the [tenant]
 */
fun List<String>.unlocalize(tenant: Tenant) = this.map { it.unlocalize(tenant) }
