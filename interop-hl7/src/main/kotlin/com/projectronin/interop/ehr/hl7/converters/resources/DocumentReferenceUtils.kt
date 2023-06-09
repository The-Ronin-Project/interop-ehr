package com.projectronin.interop.ehr.hl7.converters.resources

import com.projectronin.interop.fhir.r4.datatype.primitive.asEnum
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.valueset.CompositionStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentRelationshipType
import java.util.Base64

fun DocumentReference.getNote(): String {
    val notes = this.content.map { String(Base64.getDecoder().decode(it.attachment?.data?.value)) }
    return if (notes.size == 1) {
        notes.first()
    } else {
        notes.joinToString("\n")
    }
}

fun DocumentReference.toCompleteStatus(): String =
    when (this.docStatus.asEnum<CompositionStatus>()) {
        CompositionStatus.PRELIMINARY -> "IP"
        else -> "AU"
    }

fun DocumentReference.toConfidentialityStatus(): String = "U"

fun DocumentReference.toAvailableStatus(): String = if (this.toCompleteStatus() == "IP") "UN" else "AV"
fun DocumentReference.getParentNoteID(): String? {
    val parent = this.relatesTo.firstOrNull {
        it.code.asEnum<DocumentRelationshipType>() == DocumentRelationshipType.APPENDS
    }
    return parent?.target?.decomposedId()
}
