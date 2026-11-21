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
import androidx.compose.material3.CircularProgressIndicator
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
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.EmptyView
import id.bantuin.app.ui.theme.BorderSubtle
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.GreenSuccessContainer
import id.bantuin.app.ui.theme.NeutralBackground
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangeContainer
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.RedErrorContainer
import id.bantuin.app.ui.theme.TextMuted
import id.bantuin.app.ui.theme.TextPrimary
import id.bantuin.app.ui.theme.TextSecondary

@Composable
fun CustomerNotificationsScreen(
    repository: FakeBantuinRepository,
    onOrderClick: (String) -> Unit,
    viewModel: CustomerNotificationsViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeutralBackground)
    ) {
        BantuinTopAppBar(title = "Notifikasi")

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (uiState.notifications.isEmpty()) {
            EmptyView(
                title = "Belum Ada Notifikasi",
                message = "Pembaruan status pesanan dan informasi layanan penting Anda akan tampil di sini."
            )
        } else {
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

@Composable
private fun NotificationCardItem(
    item: CustomerNotificationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconConfig = getNotificationIconConfig(item.type)
    val isClickable = item.bookingId != null

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
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
                        color = TextMuted
                    )

                    if (isClickable) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lihat Detail",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = OrangePrimary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = OrangePrimary,
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

private fun getNotificationIconConfig(type: NotificationType): NotificationIconConfig {
    return when (type) {
        NotificationType.ORDER_PENDING -> NotificationIconConfig(
            icon = Icons.Default.HourglassTop,
            iconColor = OrangePrimary,
            containerColor = OrangeContainer
        )
        NotificationType.ORDER_CONFIRMED -> NotificationIconConfig(
            icon = Icons.Default.CheckCircle,
            iconColor = GreenSuccess,
            containerColor = GreenSuccessContainer
        )
        NotificationType.ORDER_IN_PROGRESS -> NotificationIconConfig(
            icon = Icons.Default.Build,
            iconColor = Color(0xFF1D4ED8),
            containerColor = Color(0xFFDBEAFE)
        )
        NotificationType.ORDER_COMPLETED -> NotificationIconConfig(
            icon = Icons.Default.TaskAlt,
            iconColor = GreenSuccess,
            containerColor = GreenSuccessContainer
        )
        NotificationType.ORDER_CANCELLED -> NotificationIconConfig(
            icon = Icons.Default.Cancel,
            iconColor = RedError,
            containerColor = RedErrorContainer
        )
        NotificationType.SYSTEM_INFO -> NotificationIconConfig(
            icon = Icons.Default.Info,
            iconColor = OrangePrimary,
            containerColor = Color(0xFFFFF3E0)
        )
    }
}
