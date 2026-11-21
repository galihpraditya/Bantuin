package id.bantuin.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import id.bantuin.app.ui.navigation.Routes
import id.bantuin.app.ui.theme.BorderSubtle
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangeContainer
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.TextMuted
import id.bantuin.app.ui.theme.TextPrimary

data class NavTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val hasBadge: Boolean = false
)

@Composable
fun BantuinBottomBar(
    currentRoute: String?,
    activeRole: String, // "customer" or "provider"
    onNavigateToRoute: (String) -> Unit,
    hasUnreadNotifications: Boolean = false,
    modifier: Modifier = Modifier
) {
    val items = if (activeRole == "provider") {
        listOf(
            NavTabItem(Routes.PROVIDER_HOME, "Beranda", Icons.Default.Home),
            NavTabItem(Routes.PROVIDER_ORDERS, "Pesanan", Icons.Default.ReceiptLong),
            NavTabItem(Routes.PROVIDER_CALENDAR, "Jadwal", Icons.Default.CalendarMonth),
            NavTabItem(Routes.PROVIDER_DASHBOARD, "Dashboard", Icons.Default.Dashboard)
        )
    } else {
        listOf(
            NavTabItem(Routes.CUSTOMER_HOME, "Beranda", Icons.Default.Home),
            NavTabItem(Routes.CUSTOMER_ORDERS, "Pesanan", Icons.Default.ReceiptLong),
            NavTabItem(Routes.CUSTOMER_NOTIFICATIONS, "Notifikasi", Icons.Default.Notifications, hasBadge = hasUnreadNotifications),
            NavTabItem(Routes.CUSTOMER_PROFILE, "Profil", Icons.Default.Person)
        )
    }

    NavigationBar(
        containerColor = NeutralSurface,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        onNavigateToRoute(item.route)
                    }
                },
                icon = {
                    if (item.hasBadge) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = OrangePrimary
                                )
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) OrangePrimary else TextMuted
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) OrangePrimary else TextMuted
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) TextPrimary else TextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = OrangeContainer
                )
            )
        }
    }
}
