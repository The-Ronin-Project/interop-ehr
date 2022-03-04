package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.exception.UnsupportedDynamicValueTypeException
import com.projectronin.interop.ehr.model.Annotation
import com.projectronin.interop.ehr.model.Annotation.Author
import com.projectronin.interop.ehr.model.Annotation.ReferenceAuthor
import com.projectronin.interop.ehr.model.Annotation.StringAuthor
import com.projectronin.interop.ehr.model.base.JSONElement
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.Annotation as R4Annotation

class EpicAnnotation(override val element: R4Annotation) : JSONElement(element), Annotation {
    override val time: String? = element.time?.value
    override val text: String = element.text.value

    override val author: Author<out Any>? by lazy {
        element.author?.let {
            when (it.type) {
                DynamicValueType.STRING -> StringAuthor(it.value as String)
                DynamicValueType.REFERENCE -> ReferenceAuthor(EpicReference(it.value as Reference))
                else -> throw UnsupportedDynamicValueTypeException("${it.type} is not a supported type for annotation author")
            }
        }
    }
}
