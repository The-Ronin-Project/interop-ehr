package com.projectronin.interop.fhir.r4.valueset

import com.projectronin.interop.common.enums.CodedEnum

/**
 * HTTP verbs (in the HTTP command line)
 *
 * See [FHIR Spec](http://www.hl7.org/fhir/valueset-http-verb.html)
 */
enum class HttpVerb : CodedEnum<HttpVerb> {
    /**
     * HTTP GET Command.
     */
    GET,

    /**
     * HTTP HEAD Command.
     */
    HEAD,

    /**
     * HTTP POST Command.
     */
    POST,

    /**
     * HTTP PUT Command.
     */
    PUT,

    /**
     * HTTP DELETE Command.
     */
    DELETE,

    /**
     * HTTP PATCH Command.
     */
    PATCH;

    override val code = this.name
}
