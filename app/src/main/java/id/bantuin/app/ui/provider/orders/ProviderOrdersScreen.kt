package id.bantuin.app.ui.provider.orders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.CountdownTimerView
import id.bantuin.app.ui.common.EmptyView
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderOrdersScreen(
    repository: BantuinRepository,
    onOpenChat: (String) -> Unit = {},
    onOpenCompleteOrder: (String) -> Unit = {},
    viewModel: ProviderOrdersViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors
    val tabs = listOf("Menunggu", "Terkonfirmasi", "Berjalan", "Riwayat")
    val selectedTab = uiState.selectedTab.coerceIn(0, tabs.lastIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        BantuinTopAppBar(title = "Antrean Pesanan Masuk")

        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.primary,
            edgePadding = 16.dp,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTab),
                    color = colors.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, tabName ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { viewModel.selectTab(index) },
                    text = {
                        Text(
                            text = tabName,
                            color = if (selectedTab == index) colors.primary else colors.textSecondary
                        )
                    }
                )
            }
        }

        if (!uiState.errorMessage.isNullOrBlank() && uiState.orders.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = colors.errorContainer)
            ) {
                Text(
                    text = uiState.errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        val filteredOrders = when (selectedTab) {
            0 -> uiState.pendingOrders
            1 -> uiState.confirmedOrders
            2 -> uiState.inProgressOrders
            else -> uiState.historyOrders
        }

        when {
            uiState.isLoading && uiState.orders.isEmpty() -> {
                LoadingView(message = "Memuat antrean pesanan...")
            }
            uiState.errorMessage != null && uiState.orders.isEmpty() -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat daftar pesanan.",
                    onRetry = { viewModel.loadOrders() }
                )
            }
            filteredOrders.isEmpty() -> {
                EmptyView(
                    title = "Antrean Kosong",
                    message = "Tidak ada pesanan pada status ${tabs[selectedTab].lowercase()}."
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { booking ->
                        ProviderOrderActionCard(
                            booking = booking,
                            isUpdating = uiState.isUpdatingStatus,
                            onExpired = { viewModel.loadOrders() },
                            onUpdateStatus = { newStatus ->
                                viewModel.updateStatus(booking.id, newStatus)
                            },
                            onOpenChat = { onOpenChat(booking.id) },
                            onOpenCompleteOrder = { onOpenCompleteOrder(booking.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderOrderActionCard(
    booking: BookingItem,
    isUpdating: Boolean,
    onExpired: () -> Unit,
    onUpdateStatus: (BookingStatus) -> Unit,
    onOpenChat: () -> Unit,
    onOpenCompleteOrder: () -> Unit
) {
    val colors = BantuinTheme.colors
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = CurrencyUtils.formatRupiah(booking.totalCost),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Layanan: ${booking.serviceName}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )

            Text(
                text = "Lokasi: ${booking.address}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            val paymentLabel = when {
                booking.paymentMethod == PaymentMethod.QRIS && booking.paymentStatus == PaymentStatus.PAID ->
                    "QRIS • Lunas"
                booking.paymentMethod == PaymentMethod.QRIS ->
                    "QRIS • Menunggu Bayar"
                else -> "Tunai • Bayar di Lokasi"
            }
            val paymentFg = if (booking.paymentStatus == PaymentStatus.PAID) {
                colors.success
            } else if (booking.paymentMethod == PaymentMethod.QRIS) {
                colors.warning
            } else {
                colors.textSecondary
            }
            val paymentBg = if (booking.paymentStatus == PaymentStatus.PAID) {
                colors.successContainer
            } else if (booking.paymentMethod == PaymentMethod.QRIS) {
                colors.warningContainer
            } else {
                colors.surfaceSubtle
            }
            BadgeContainer(
                text = paymentLabel,
                color = paymentFg,
                backgroundColor = paymentBg
            )

            if (!booking.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Catatan: ${booking.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${booking.scheduledDate} • ${booking.scheduledTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )

                if (booking.status == BookingStatus.PENDING && booking.expiresAt != null) {
                    CountdownTimerView(
                        targetEpochMillis = booking.expiresAt.toEpochMilliseconds(),
                        onExpired = onExpired
                    )
                }
            }

            if (booking.status.isActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenChat,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat Pelanggan", fontWeight = FontWeight.SemiBold)
                    }

                    if (!booking.customerPhone.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${booking.customerPhone}"))
                                context.startActivity(intent)
                            },
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(colors.outline)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Telepon", color = colors.textPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (booking.status) {
                BookingStatus.PENDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(BookingStatus.DITOLAK) },
                            enabled = !isUpdating,
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(colors.error)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Tolak", color = colors.error)
                        }

                        Button(
                            onClick = { onUpdateStatus(BookingStatus.DIKONFIRMASI) },
                            enabled = !isUpdating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Terima Pesanan")
                        }
                    }
                }
                BookingStatus.DIKONFIRMASI -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(BookingStatus.DIBATALKAN_MITRA) },
                            enabled = !isUpdating,
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(colors.error)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Batalkan", color = colors.error)
                        }

                        Button(
                            onClick = { onUpdateStatus(BookingStatus.DALAM_PENGERJAAN) },
                            enabled = !isUpdating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Mulai Pengerjaan")
                        }
                    }
                }
                BookingStatus.DALAM_PENGERJAAN -> {
                    Button(
                        onClick = onOpenCompleteOrder,
                        enabled = !isUpdating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.success,
                            contentColor = if (colors.isDark) colors.background else androidx.compose.ui.graphics.Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Proses Selesai & Tagihan", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Text(
                        text = "Status: ${booking.status.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}
