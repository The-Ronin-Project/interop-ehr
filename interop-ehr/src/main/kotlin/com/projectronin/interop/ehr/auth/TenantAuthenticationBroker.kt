package com.projectronin.interop.ehr.auth

import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.common.auth.BrokeredAuthenticator
import com.projectronin.interop.tenant.config.model.Tenant

class TenantAuthenticationBroker(val service: AuthenticationService, val tenant: Tenant) :
    BrokeredAuthenticator() {
    override fun reloadAuthentication(): Authentication =
        service.getAuthentication(tenant)
            ?: throw IllegalStateException("Unable to retrieve authentication for ${tenant.mnemonic}")
}
