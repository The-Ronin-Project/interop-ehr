package com.projectronin.interop.ehr.util

import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Resource

/**
 * Converts an R4 [Bundle] into a list of [Resource]s of a single type, filtering if necessary.
 * Infers return type if possible, otherwise requires a Resource type passed explicitly.
 */
inline fun <reified R : Resource<R>> Bundle.toListOfType(): List<R> =
    this.entry.mapNotNull { it.resource }.filterIsInstance<R>()
