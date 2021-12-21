package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](http://hl7.org/fhir/R4/valueset-related-artifact-type.html)
 */
enum class RelatedArtifactType(@JsonValue override val code: String) : CodedEnum<RelatedArtifactType> {
    /**
     * Additional documentation for the knowledge resource. This would include additional instructions on usage as well
     * as additional information on clinical context or appropriateness.
     */
    DOCUMENTATION("documentation"),

    /**
     * A summary of the justification for the knowledge resource including supporting evidence, relevant guidelines, or
     * other clinically important information. This information is intended to provide a way to make the justification
     * for the knowledge resource available to the consumer of interventions or results produced by the knowledge resource.
     */
    JUSTIFICATION("justification"),

    /**
     * Bibliographic citation for papers, references, or other relevant material for the knowledge resource. This is
     * intended to allow for citation of related material, but that was not necessarily specifically prepared in
     * connection with this knowledge resource.
     */
    CITATION("citation"),

    /**
     * The previous version of the knowledge resource.
     */
    PREDECESSOR("predecessor"),

    /**
     * The next version of the knowledge resource.
     */
    SUCCESSOR("successor"),

    /**
     * The knowledge resource is derived from the related artifact. This is intended to capture the relationship in which
     * a particular knowledge resource is based on the content of another artifact, but is modified to capture either a
     * different set of overall requirements, or a more specific set of requirements such as those involved in a particular
     * institution or clinical setting.
     */
    DERIVED_FROM("derived-from"),

    /**
     * The knowledge resource depends on the given related artifact.
     */
    DEPENDS_ON("depends-on"),

    /**
     * The knowledge resource is composed of the given related artifact.
     */
    COMPOSED_OF("composed-of")
}
