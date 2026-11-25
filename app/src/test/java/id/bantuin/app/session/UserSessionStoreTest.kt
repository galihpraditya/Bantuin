package id.bantuin.app.session

import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class UserSessionStoreTest {

    private lateinit var sessionStore: InMemorySessionStore

    @Before
    fun setUp() {
        sessionStore = InMemorySessionStore()
    }

    @Test
    fun defaultSession_initializesWithCustomerRoleAndDemoUser() = runBlocking {
        assertEquals("customer", sessionStore.activeRole.first())
        assertEquals("customer", sessionStore.activeRoleValue)

        val user = sessionStore.currentUser.first()
        assertEquals("user_demo_rani", user?.id)
        assertEquals("Rani Wijaya", user?.fullName)
        assertEquals(sessionStore.currentUserValue?.id, user?.id)
    }

    @Test
    fun setActiveRole_updatesRoleFlowAndValue() = runBlocking {
        sessionStore.setActiveRole("provider")
        assertEquals("provider", sessionStore.activeRole.first())
        assertEquals("provider", sessionStore.activeRoleValue)

        sessionStore.setActiveRole("customer")
        assertEquals("customer", sessionStore.activeRole.first())
        assertEquals("customer", sessionStore.activeRoleValue)
    }

    @Test
    fun saveSession_updatesCurrentUser() = runBlocking {
        val newUser = UserProfile(
            id = "user_budi",
            fullName = "Budi Santoso",
            email = "budi@bantuin.test",
            phone = "08123456789",
            roles = setOf("provider"),
            defaultAddress = "Jl. Bandung No. 1, Malang",
            defaultLocation = GeoPoint(-7.96, 112.62)
        )
        sessionStore.saveSession(newUser)

        val retrieved = sessionStore.currentUser.first()
        assertEquals("user_budi", retrieved?.id)
        assertEquals("Budi Santoso", retrieved?.fullName)
        assertEquals("Jl. Bandung No. 1, Malang", retrieved?.defaultAddress)
        assertEquals(GeoPoint(-7.96, 112.62), retrieved?.defaultLocation)
        assertEquals(newUser, sessionStore.currentUserValue)
    }

    @Test
    fun clearSession_setsCurrentUserToNull() = runBlocking {
        sessionStore.clearSession()
        assertNull(sessionStore.currentUser.first())
        assertNull(sessionStore.currentUserValue)
    }
}
