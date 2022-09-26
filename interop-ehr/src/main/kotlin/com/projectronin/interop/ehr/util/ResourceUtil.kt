package com.projectronin.interop.ehr.util

import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.stu3.resource.STU3Bundle
import com.projectronin.interop.fhir.stu3.resource.STU3Resource

/**
 * Converts an R4 [Bundle] into a list of [Resource]s of a single type, filtering if necessary.
 * Infers return type if possible, otherwise requires a Resource type passed explicitly.
 */
inline fun <reified R : Resource<R>> Bundle.toListOfType(): List<R> =
    this.entry.mapNotNull { it.resource }.filterIsInstance<R>()

/**
 * Converts a STU3 [Bundle] into a list of [STU3Resource]s of a single type, filtering if necessary.
 * Infers return type if possible, otherwise requires a Resource type passed explicitly.
 */
inline fun <reified R : STU3Resource<R>> STU3Bundle.toListOfSTU3Type(): List<R> =
    this.entry.mapNotNull { it.resource }.filterIsInstance<R>()
