package com.projectronin.interop.tenant.config.data.model

import com.projectronin.interop.common.hl7.MessageType
import com.projectronin.interop.common.hl7.ProcessingID
import org.ktorm.entity.Entity

/**
 * Entity definition for the Tenant Server data object.
 * @property id The ID of the backing data store for this Tenant Server
 * @property tenant The tenant
 * @property messageType The type of HL7 messages this server can accept
 * @property address the IP or URL of the serve
 * @property port The port to send messages on
 * @property serverType See https://terminology.hl7.org/CodeSystem-v2-0103.html
 */
interface TenantServerDO : Entity<TenantServerDO> {
    companion object : Entity.Factory<TenantServerDO>()

    var id: Int
    var tenant: TenantDO
    var messageType: MessageType
    var address: String
    var port: Int
    var serverType: ProcessingID
}
