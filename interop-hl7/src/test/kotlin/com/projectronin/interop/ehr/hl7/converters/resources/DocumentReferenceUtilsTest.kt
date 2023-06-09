package com.projectronin.interop.ehr.hl7.converters.resources

import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContent
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceRelatesTo
import com.projectronin.interop.fhir.r4.valueset.CompositionStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentReferenceStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentRelationshipType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Base64

class DocumentReferenceUtilsTest {
    val completedDocument = DocumentReference(
        status = Code(DocumentReferenceStatus.CURRENT.code),
        relatesTo = listOf(
            DocumentReferenceRelatesTo(
                code = Code(DocumentRelationshipType.APPENDS.code),
                target = Reference(reference = "DocumentReference/parentID".asFHIR())
            )
        ),
        content = listOf(
            DocumentReferenceContent(
                attachment = Attachment(
                    data = Base64Binary(
                        value = Base64.getEncoder().encodeToString("Cool Note!".toByteArray())
                    )
                )
            )
        )
    )
    val inProgressDocument = DocumentReference(
        status = Code(DocumentReferenceStatus.CURRENT.code),
        docStatus = Code(CompositionStatus.PRELIMINARY.code)
    )

    @Test
    fun `toConfidentialityStatus status works`() {
        assertEquals("U", completedDocument.toConfidentialityStatus())
    }

    @Test
    fun `toCompleteStatus status works`() {
        assertEquals("AU", completedDocument.toCompleteStatus())
        assertEquals("IP", inProgressDocument.toCompleteStatus())
    }

    @Test
    fun `toAvailableStatus status works`() {
        assertEquals("UN", inProgressDocument.toAvailableStatus())
        assertEquals("AV", completedDocument.toAvailableStatus())
    }

    @Test
    fun `getParentNoteID works`() {
        assertEquals("parentID", completedDocument.getParentNoteID())
    }

    @Test
    fun `getNote works for one line`() {
        assertEquals("Cool Note!", completedDocument.getNote())
    }

    @Test
    fun `getNote works for multiple lines`() {
        val document = DocumentReference(
            status = Code(DocumentReferenceStatus.CURRENT.code),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        data = Base64Binary(
                            value = Base64.getEncoder().encodeToString("Cool Note!".toByteArray())
                        )
                    )
                ),
                DocumentReferenceContent(
                    attachment = Attachment(
                        data = Base64Binary(
                            value = Base64.getEncoder().encodeToString("Second Line".toByteArray())
                        )
                    )
                )
            )
        )
        assertEquals("Cool Note!\nSecond Line", document.getNote())
    }
}
