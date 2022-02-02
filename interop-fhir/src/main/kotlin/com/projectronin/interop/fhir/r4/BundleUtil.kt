package com.projectronin.interop.fhir.r4

import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.resource.Bundle

/**
 * Creates a Bundle from the supplied [source] and [addition]. All values on the returned bundle are based off the
 * source, except for the total count and entry, which will include those elements from the source and the addition.
 */
fun mergeBundles(source: Bundle, addition: Bundle): Bundle =
    Bundle(
        id = source.id,
        meta = source.meta,
        implicitRules = source.implicitRules,
        language = source.language,
        identifier = source.identifier,
        type = source.type,
        timestamp = source.timestamp,
        total = UnsignedInt((source.total?.value ?: 0) + (addition.total?.value ?: 0)),
        link = source.link,
        entry = source.entry + addition.entry,
        signature = source.signature
    )
