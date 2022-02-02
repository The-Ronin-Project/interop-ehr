package com.projectronin.interop.fhir.r4.valueset

import com.fasterxml.jackson.annotation.JsonValue
import com.projectronin.interop.common.enums.CodedEnum

/**
 * See [FHIR Spec](http://www.hl7.org/fhir/valueset-bundle-type.html)
 */
enum class BundleType(@JsonValue override val code: String) : CodedEnum<BundleType> {
    /**
     * The bundle is a document. The first resource is a Composition.
     */
    DOCUMENT("document"),

    /**
     * The bundle is a message. The first resource is a MessageHeader.
     */
    MESSAGE("message"),

    /**
     * The bundle is a transaction - intended to be processed by a server as an atomic commit.
     */
    TRANSACTION("transaction"),

    /**
     * The bundle is a transaction response. Because the response is a transaction response, the transaction has succeeded, and all responses are error free.
     */
    TRANSACTION_RESPONSE("transaction-response"),

    /**
     * The bundle is a set of actions - intended to be processed by a server as a group of independent actions.
     */
    BATCH("batch"),

    /**
     * The bundle is a batch response. Note that as a batch, some responses may indicate failure and others success.
     */
    BATCH_RESPONSE("batch-response"),

    /**
     * The bundle is a list of resources from a history interaction on a server.
     */
    HISTORY("history"),

    /**
     * The bundle is a list of resources returned as a result of a search/query interaction, operation, or message.
     */
    SEARCHSET("searchset"),

    /**
     * The bundle is a set of resources collected into a single package for ease of distribution that imposes no processing obligations or behavioral rules beyond persistence.
     */
    COLLECTION("collection")
}
