package com.projectronin.interop.ehr.auth

import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class AuthenticationBrokerTest {
    private val epicVendor =
        mockk<Epic> {
            every { type } returns VendorType.EPIC
        }

    @Test
    fun `no authentication service for vendor`() {
        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "TENANT"
                every { vendor } returns epicVendor
            }

        // Since we only have 1 vendor today, we have to send this with no authentication service.
        val broker = EHRAuthenticationBroker(listOf())
        val exception =
            assertThrows<IllegalStateException> {
                broker.getAuthenticator(tenant)
            }
        assertEquals("No AuthenticationService registered for EPIC", exception.message)

        verify(exactly = 1) {
            tenant.mnemonic
        }
    }

    @Test
    fun `new authentication not granted`() {
        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "TENANT"
                every { vendor } returns epicVendor
            }

        val authenticationService = mockk<AuthenticationService>()
        every { authenticationService.vendorType } returns VendorType.EPIC
        every { authenticationService.getAuthentication(tenant) } returns null

        val broker = EHRAuthenticationBroker(listOf(authenticationService))
        val authenticator = broker.getAuthenticator(tenant)

        val exception =
            assertThrows<IllegalStateException> {
                val authentication = authenticator.getAuthentication()
            }
        assertEquals(exception.message, "Unable to retrieve authentication for TENANT")

        verify(exactly = 1) {
            authenticationService.getAuthentication(tenant)
        }
    }

    @Test
    fun `new authentication granted`() {
        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "TENANT"
                every { vendor } returns epicVendor
            }

        val authenticationService = mockk<AuthenticationService>()
        every { authenticationService.vendorType } returns VendorType.EPIC

        val mockedAuthentication =
            mockk<Authentication> {
                every { expiresAt } returns Instant.ofEpochMilli(Date().time).plus(1L, ChronoUnit.DAYS)
            }
        every { authenticationService.getAuthentication(tenant) } returns mockedAuthentication

        val broker = EHRAuthenticationBroker(listOf(authenticationService))
        val authenticator = broker.getAuthenticator(tenant)
        val authentication = authenticator.getAuthentication()
        assertEquals(mockedAuthentication, authentication)

        verify(exactly = 1) {
            authenticationService.vendorType
            authenticationService.getAuthentication(tenant)
        }
    }

    @Test
    fun `saved authentication with no expiration`() {
        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "TENANT"
                every { vendor } returns epicVendor
            }

        val authenticationService = mockk<AuthenticationService>()
        every { authenticationService.vendorType } returns VendorType.EPIC

        val mockedAuthentication1 =
            mockk<Authentication> {
                every { expiresAt } returns null
            }
        val mockedAuthentication2 =
            mockk<Authentication> {
                every { expiresAt } returns Instant.ofEpochMilli(Date().time).plus(1L, ChronoUnit.DAYS)
            }
        every { authenticationService.getAuthentication(tenant) } returns mockedAuthentication1 andThen mockedAuthentication2

        val broker = EHRAuthenticationBroker(listOf(authenticationService))

        // This test needs to call in once to ensure the value is set in the Map.
        val authenticator = broker.getAuthenticator(tenant)
        val authentication = authenticator.getAuthentication()
        assertEquals(mockedAuthentication1, authentication)

        // And then a second time to actually read it from the Map.
        val secondAuthenticator = broker.getAuthenticator(tenant)
        val secondAuthentication = secondAuthenticator.getAuthentication()
        assertEquals(authenticator, secondAuthenticator)
        assertEquals(mockedAuthentication2, secondAuthentication)

        verify(exactly = 1) {
            authenticationService.vendorType
        }
        // This gets requested twice because the authentication is expired on our second request
        verify(exactly = 2) {
            authenticationService.getAuthentication(tenant)
        }
    }

    @Test
    fun `expired authentication`() {
        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "TENANT"
                every { vendor } returns epicVendor
            }

        val authenticationService = mockk<AuthenticationService>()
        every { authenticationService.vendorType } returns VendorType.EPIC

        val mockedAuthentication1 =
            mockk<Authentication> {
                every { expiresAt } returns Instant.now().minusSeconds(5)
            }
        val mockedAuthentication2 =
            mockk<Authentication> {
                every { expiresAt } returns Instant.ofEpochMilli(Date().time).plus(1L, ChronoUnit.DAYS)
            }
        every { authenticationService.getAuthentication(tenant) } returns mockedAuthentication1 andThen mockedAuthentication2

        val broker = EHRAuthenticationBroker(listOf(authenticationService))

        // This test needs to call in once to ensure the value is set in the Map.
        val authenticator = broker.getAuthenticator(tenant)
        val authentication = authenticator.getAuthentication()
        assertEquals(mockedAuthentication1, authentication)

        // And then a second time to actually read it from the Map.
        val secondAuthenticator = broker.getAuthenticator(tenant)
        val secondAuthentication = secondAuthenticator.getAuthentication()
        assertEquals(authenticator, secondAuthenticator)
        assertEquals(mockedAuthentication2, secondAuthentication)

        verify(exactly = 1) {
            authenticationService.vendorType
        }

        // This gets requested twice because the authentication is expired on our second request
        verify(exactly = 2) {
            authenticationService.getAuthentication(tenant)
        }
    }

    @Test
    fun `authentication expiring within buffer`() {
        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "TENANT"
                every { vendor } returns epicVendor
            }

        val authenticationService = mockk<AuthenticationService>()
        every { authenticationService.vendorType } returns VendorType.EPIC

        val mockedAuthentication1 =
            mockk<Authentication> {
                every { expiresAt } returns Instant.now().plusSeconds(25)
            }
        val mockedAuthentication2 =
            mockk<Authentication> {
                every { expiresAt } returns Instant.ofEpochMilli(Date().time).plus(1L, ChronoUnit.DAYS)
            }
        every { authenticationService.getAuthentication(tenant) } returns mockedAuthentication1 andThen mockedAuthentication2

        val broker = EHRAuthenticationBroker(listOf(authenticationService))

        // This test needs to call in once to ensure the value is set in the Map.
        val authenticator = broker.getAuthenticator(tenant)
        val authentication = authenticator.getAuthentication()
        assertEquals(mockedAuthentication1, authentication)

        // And then a second time to actually read it from the Map.
        val secondAuthenticator = broker.getAuthenticator(tenant)
        val secondAuthentication = secondAuthenticator.getAuthentication()
        assertEquals(authenticator, secondAuthenticator)
        assertEquals(mockedAuthentication2, secondAuthentication)

        verify(exactly = 1) {
            authenticationService.vendorType
        }
        // This gets requested twice because the authentication is expired on our second request
        verify(exactly = 2) {
            authenticationService.getAuthentication(tenant)
        }
    }

    @Test
    fun `authentication expiring outside buffer`() {
        val tenant =
            mockk<Tenant> {
                every { mnemonic } returns "TENANT"
                every { vendor } returns epicVendor
            }

        val authenticationService = mockk<AuthenticationService>()
        every { authenticationService.vendorType } returns VendorType.EPIC

        val mockedAuthentication =
            mockk<Authentication> {
                every { expiresAt } returns Instant.now().plusSeconds(3600)
            }
        every { authenticationService.getAuthentication(tenant) } returns mockedAuthentication

        val broker = EHRAuthenticationBroker(listOf(authenticationService))

        // This test needs to call in once to ensure the value is set in the Map.
        val authenticator = broker.getAuthenticator(tenant)
        val authentication = authenticator.getAuthentication()
        assertEquals(mockedAuthentication, authentication)

        // And then a second time to actually read it from the Map.
        val secondAuthenticator = broker.getAuthenticator(tenant)
        val secondAuthentication = secondAuthenticator.getAuthentication()
        assertEquals(authenticator, secondAuthenticator)
        assertEquals(mockedAuthentication, secondAuthentication)

        verify(exactly = 1) {
            authenticationService.vendorType
            authenticationService.getAuthentication(tenant)
        }
    }
}
