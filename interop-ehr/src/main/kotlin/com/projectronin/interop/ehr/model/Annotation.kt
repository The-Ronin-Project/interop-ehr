package com.projectronin.interop.ehr.model

/**
 * Representation of an Annotation.
 */
interface Annotation : EHRElement {
    /**
     * Individual responsible for the annotation
     */
    val author: Author<out Any>?

    /**
     * When the annotation was made
     */
    val time: String?

    /**
     * The annotation - text content (as markdown)
     */
    val text: String

    /**
     * Representation of an annotation's author.
     */
    sealed interface Author<T> {
        val value: T
    }

    /**
     * Implementation of an Author based off a [Reference]
     */
    class ReferenceAuthor(override val value: Reference) : Author<Reference>

    /**
     * Implementation of an Author based off a [String]
     */
    class StringAuthor(override val value: String) : Author<String>
}
