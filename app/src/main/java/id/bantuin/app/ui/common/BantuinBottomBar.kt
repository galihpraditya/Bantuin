package id.bantuin.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.bantuin.app.ui.navigation.Routes
import id.bantuin.app.ui.theme.BantuinMotion
import id.bantuin.app.ui.theme.BantuinTheme
import id.bantuin.app.ui.theme.rememberReducedMotion

data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hasBadge: Boolean = false,
    val badgeCount: Int = 0
)

fun customerNavTabs(unreadChatCount: Int = 0): List<NavTabItem> = listOf(
    NavTabItem(
        route = Routes.CUSTOMER_HOME,
        label = "Beranda",
        selectedIcon = BantuinUiIcons.HomeFilled,
        unselectedIcon = BantuinUiIcons.HomeOutline
    ),
    NavTabItem(
        route = Routes.CUSTOMER_ORDERS,
        label = "Pesanan",
        selectedIcon = BantuinUiIcons.OrdersFilled,
        unselectedIcon = BantuinUiIcons.OrdersOutline
    ),
    NavTabItem(
        route = Routes.CHAT_LIST,
        label = "Chat",
        selectedIcon = BantuinUiIcons.ChatFilled,
        unselectedIcon = BantuinUiIcons.ChatOutline,
        hasBadge = unreadChatCount > 0,
        badgeCount = unreadChatCount
    ),
    NavTabItem(
        route = Routes.CUSTOMER_PROFILE,
        label = "Profil",
        selectedIcon = BantuinUiIcons.ProfileFilled,
        unselectedIcon = BantuinUiIcons.ProfileOutline
    )
)

fun providerNavTabs(): List<NavTabItem> = listOf(
    NavTabItem(
        route = Routes.PROVIDER_HOME,
        label = "Beranda",
        selectedIcon = BantuinUiIcons.HomeFilled,
        unselectedIcon = BantuinUiIcons.HomeOutline
    ),
    NavTabItem(
        route = Routes.PROVIDER_ORDERS,
        label = "Pesanan",
        selectedIcon = BantuinUiIcons.OrdersFilled,
        unselectedIcon = BantuinUiIcons.OrdersOutline
    ),
    NavTabItem(
        route = Routes.PROVIDER_DASHBOARD,
        label = "Dashboard",
        selectedIcon = BantuinUiIcons.DashboardFilled,
        unselectedIcon = BantuinUiIcons.DashboardOutline
    ),
    NavTabItem(
        route = Routes.PROVIDER_PROFILE,
        label = "Profil",
        selectedIcon = BantuinUiIcons.ProfileFilled,
        unselectedIcon = BantuinUiIcons.ProfileOutline
    )
)

fun bottomBarRoutes(): Set<String> =
    (customerNavTabs() + providerNavTabs()).map { it.route }.toSet()

@Composable
fun BantuinBottomBar(
    currentRoute: String?,
    activeRole: String, // "customer" or "provider"
    onNavigateToRoute: (String) -> Unit,
    unreadChatCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    val reducedMotion = rememberReducedMotion()
    val items = if (activeRole == "provider") {
        providerNavTabs()
    } else {
        customerNavTabs(unreadChatCount = unreadChatCount)
    }

    Column(modifier = modifier) {
        HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
        NavigationBar(
            containerColor = colors.surface,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1.0f,
                    animationSpec = if (reducedMotion) snap() else BantuinMotion.bouncySpring(),
                    label = "bottomBarIconScale"
                )

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != item.route) {
                            onNavigateToRoute(item.route)
                        }
                    },
                    icon = {
                        val iconModifier = Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                        val iconVector = if (isSelected) item.selectedIcon else item.unselectedIcon
                        val iconTint = if (isSelected) colors.onPrimaryContainer else colors.textMuted

                        BadgedBox(
                            badge = {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = item.hasBadge,
                                    enter = if (reducedMotion) fadeIn() else scaleIn(BantuinMotion.bouncySpring()) + fadeIn(),
                                    exit = if (reducedMotion) fadeOut() else scaleOut() + fadeOut()
                                ) {
                                    if (item.badgeCount > 0) {
                                        Badge(
                                            containerColor = colors.primary,
                                            contentColor = colors.onPrimary
                                        ) {
                                            Text(
                                                text = if (item.badgeCount > 9) "9+" else "${item.badgeCount}",
                                                fontSize = 10.sp
                                            )
                                        }
                                    } else {
                                        Badge(containerColor = colors.primary)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = item.label,
                                tint = iconTint,
                                modifier = iconModifier
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.textPrimary else colors.textMuted
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colors.primaryContainer,
                        selectedIconColor = colors.onPrimaryContainer,
                        unselectedIconColor = colors.textMuted,
                        selectedTextColor = colors.textPrimary,
                        unselectedTextColor = colors.textMuted
                    )
                )
            }
        }
    }
}
