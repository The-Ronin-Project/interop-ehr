package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.inputs.EHRMessageInput
import com.projectronin.interop.tenant.config.model.Tenant

interface MessageService {
    /**
     * Sends a specified message, [messageInput], to the specified [tenant] and returns the EHR id for the message.
     */
    fun sendMessage(
        tenant: Tenant,
        messageInput: EHRMessageInput,
    ): String
}
