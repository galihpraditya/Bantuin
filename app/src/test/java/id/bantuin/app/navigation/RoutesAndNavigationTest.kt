package id.bantuin.app.navigation

import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.common.bottomBarRoutes
import id.bantuin.app.ui.common.customerNavTabs
import id.bantuin.app.ui.common.providerNavTabs
import id.bantuin.app.ui.navigation.Routes
import id.bantuin.app.ui.navigation.activeBottomBarRoutesFor
import id.bantuin.app.ui.navigation.homeRouteFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesAndNavigationTest {

    @Test
    fun customerBottomBar_containsExactlyFourStreamlinedTabs() {
        val routes = customerNavTabs().map { it.route }
        assertEquals(
            listOf(
                Routes.CUSTOMER_HOME,
                Routes.CUSTOMER_ORDERS,
                Routes.CHAT_LIST,
                Routes.CUSTOMER_PROFILE
            ),
            routes
        )
        assertFalse(Routes.CUSTOMER_NOTIFICATIONS in routes)
    }

    @Test
    fun providerBottomBar_containsExactlyFourStreamlinedTabs() {
        val routes = providerNavTabs().map { it.route }
        assertEquals(
            listOf(
                Routes.PROVIDER_HOME,
                Routes.PROVIDER_ORDERS,
                Routes.PROVIDER_DASHBOARD,
                Routes.PROVIDER_PROFILE
            ),
            routes
        )
        assertFalse(Routes.PROVIDER_CALENDAR in routes)
    }

    @Test
    fun bottomBarRoutes_matchesCombinedCustomerAndProviderTabs() {
        val routes = bottomBarRoutes()
        assertEquals(8, routes.size)
        assertTrue(Routes.CHAT_LIST in routes)
        assertTrue(Routes.PROVIDER_PROFILE in routes)
        assertFalse(Routes.CUSTOMER_NOTIFICATIONS in routes)
        assertFalse(Routes.SETTINGS in routes)
    }

    @Test
    fun activeBottomBarRoutesFor_separatesCustomerChatListFromProviderTabs() {
        val customerRoutes = activeBottomBarRoutesFor("customer")
        val providerRoutes = activeBottomBarRoutesFor("provider")

        assertTrue(Routes.CHAT_LIST in customerRoutes)
        assertFalse(Routes.CHAT_LIST in providerRoutes)
        assertEquals(4, customerRoutes.size)
        assertEquals(4, providerRoutes.size)
    }

    @Test
    fun homeRouteFor_respectsActiveRoleForDualRoleUsers() {
        val dualRoleUser = UserProfile(
            id = "u1",
            fullName = "Dewi",
            email = "dewi@bantuin.test",
            phone = "08123456789",
            roles = setOf("customer", "provider")
        )
        val providerOnlyUser = UserProfile(
            id = "p01",
            fullName = "Budi",
            email = "mitra01@bantuin.test",
            phone = "08123456789",
            roles = setOf("provider")
        )

        assertEquals(Routes.PROVIDER_HOME, homeRouteFor(dualRoleUser, activeRole = "provider"))
        assertEquals(Routes.CUSTOMER_HOME, homeRouteFor(dualRoleUser, activeRole = "customer"))
        assertEquals(Routes.PROVIDER_HOME, homeRouteFor(providerOnlyUser, activeRole = "customer"))
    }
}
