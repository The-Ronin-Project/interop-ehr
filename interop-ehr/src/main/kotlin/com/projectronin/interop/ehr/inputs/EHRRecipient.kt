package com.projectronin.interop.ehr.inputs

/**
 * The [id] recipient of an EHR message.  Alternatively, [isPool] indicates the target is a pool of users.
 */
data class EHRRecipient(val id: String, val isPool: Boolean)
