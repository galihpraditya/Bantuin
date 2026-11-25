package id.bantuin.app.ui.customer.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.EmptyView
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme

@Composable
fun CustomerNotificationsScreen(
    repository: BantuinRepository,
    onOrderClick: (String) -> Unit,
    viewModel: CustomerNotificationsViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        BantuinTopAppBar(title = "Notifikasi")

        when {
            uiState.isLoading && uiState.notifications.isEmpty() -> {
                LoadingView(message = "Memuat notifikasi...")
            }
            uiState.errorMessage != null && uiState.notifications.isEmpty() -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat notifikasi.",
                    onRetry = { viewModel.loadNotifications() }
                )
            }
            uiState.notifications.isEmpty() -> {
                EmptyView(
                    title = "Belum Ada Notifikasi",
                    message = "Pembaruan status pesanan dan informasi layanan penting Anda akan tampil di sini."
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.notifications, key = { it.id }) { item ->
                        NotificationCardItem(
                            item = item,
                            onClick = {
                                if (item.bookingId != null) {
                                    onOrderClick(item.bookingId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    item: CustomerNotificationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    val iconConfig = rememberNotificationIconConfig(item.type)
    val isClickable = item.bookingId != null

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isClickable, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconConfig.containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconConfig.icon,
                    contentDescription = null,
                    tint = iconConfig.iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )

                    if (isClickable) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lihat Detail",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NotificationIconConfig(
    val icon: ImageVector,
    val iconColor: Color,
    val containerColor: Color
)

@Composable
private fun rememberNotificationIconConfig(type: NotificationType): NotificationIconConfig {
    val colors = BantuinTheme.colors
    return when (type) {
        NotificationType.ORDER_PENDING -> NotificationIconConfig(
            icon = Icons.Default.HourglassTop,
            iconColor = colors.warning,
            containerColor = colors.warningContainer
        )
        NotificationType.ORDER_CONFIRMED -> NotificationIconConfig(
            icon = Icons.Default.CheckCircle,
            iconColor = colors.success,
            containerColor = colors.successContainer
        )
        NotificationType.ORDER_IN_PROGRESS -> NotificationIconConfig(
            icon = Icons.Default.Build,
            iconColor = colors.info,
            containerColor = colors.infoContainer
        )
        NotificationType.ORDER_COMPLETED -> NotificationIconConfig(
            icon = Icons.Default.TaskAlt,
            iconColor = colors.success,
            containerColor = colors.successContainer
        )
        NotificationType.ORDER_CANCELLED -> NotificationIconConfig(
            icon = Icons.Default.Cancel,
            iconColor = colors.error,
            containerColor = colors.errorContainer
        )
        NotificationType.SYSTEM_INFO -> NotificationIconConfig(
            icon = Icons.Default.Info,
            iconColor = colors.onPrimaryContainer,
            containerColor = colors.primaryContainer
        )
    }
}
