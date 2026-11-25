package id.bantuin.app.navigation

import id.bantuin.app.ui.common.bottomBarRoutes
import id.bantuin.app.ui.common.customerNavTabs
import id.bantuin.app.ui.common.providerNavTabs
import id.bantuin.app.ui.navigation.Routes
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
}
