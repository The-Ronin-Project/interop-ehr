package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Url

/**
 * This type is for containing or referencing attachments - additional data content defined in other formats. The most
 * common use of this type is to include images or reports in some report format such as PDF. However, it can be used
 * for any data that has a MIME type.
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/datatypes.html#Attachment)
 */
data class Attachment(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val contentType: Code? = null,
    val language: Code? = null,
    val data: Base64Binary? = null,
    val url: Url? = null,
    val size: UnsignedInt? = null,
    val hash: Base64Binary? = null,
    val title: String? = null,
    val creation: DateTime? = null
) : Element {
    init {
        require(data == null || contentType != null) { "If the Attachment has data, it SHALL have a contentType" }
    }
}
