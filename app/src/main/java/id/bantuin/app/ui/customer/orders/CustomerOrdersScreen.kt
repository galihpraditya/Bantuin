package id.bantuin.app.ui.customer.orders

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun CustomerOrdersScreen(
    repository: BantuinRepository,
    onOrderClick: (String) -> Unit,
    onOpenQrisPayment: (String) -> Unit = {},
    viewModel: CustomerOrdersViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors
    val selectedTab = uiState.selectedTab
    val activeOrders = uiState.activeOrders
    val historyOrders = uiState.historyOrders

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        BantuinTopAppBar(title = "Pesanan Saya")

        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.primary,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTab),
                    color = colors.primary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { viewModel.selectTab(0) },
                text = {
                    Text(
                        text = "Pesanan Aktif (${activeOrders.size})",
                        color = if (selectedTab == 0) colors.primary else colors.textSecondary
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { viewModel.selectTab(1) },
                text = {
                    Text(
                        text = "Riwayat Transaksi",
                        color = if (selectedTab == 1) colors.primary else colors.textSecondary
                    )
                }
            )
        }

        val displayList = if (selectedTab == 0) activeOrders else historyOrders

        when {
            uiState.isLoading && uiState.orders.isEmpty() -> {
                LoadingView(message = "Memuat daftar pesanan...")
            }
            uiState.errorMessage != null && uiState.orders.isEmpty() -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat daftar pesanan.",
                    onRetry = { viewModel.loadOrders() }
                )
            }
            displayList.isEmpty() -> {
                EmptyView(
                    title = if (selectedTab == 0) "Tidak Ada Pesanan Aktif" else "Belum Ada Riwayat",
                    message = if (selectedTab == 0) {
                        "Pesan layanan sekarang untuk merawat rumah atau kos Anda."
                    } else {
                        "Pesanan yang telah selesai atau dibatalkan akan tercatat di sini."
                    }
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayList, key = { it.id }) { booking ->
                        BookingCardItem(
                            booking = booking,
                            onExpired = { viewModel.loadOrders() },
                            onClick = { onOrderClick(booking.id) },
                            onOpenQrisPayment = { onOpenQrisPayment(booking.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCardItem(
    booking: BookingItem,
    onExpired: () -> Unit,
    onClick: () -> Unit,
    onOpenQrisPayment: () -> Unit
) {
    val colors = BantuinTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.providerName,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                val badgeBg = when (booking.status) {
                    BookingStatus.SELESAI -> colors.successContainer
                    BookingStatus.DITOLAK, BookingStatus.KEDALUWARSA,
                    BookingStatus.DIBATALKAN_PELANGGAN, BookingStatus.DIBATALKAN_MITRA -> colors.errorContainer
                    else -> colors.primaryContainer
                }
                val badgeFg = when (booking.status) {
                    BookingStatus.SELESAI -> colors.success
                    BookingStatus.DITOLAK, BookingStatus.KEDALUWARSA,
                    BookingStatus.DIBATALKAN_PELANGGAN, BookingStatus.DIBATALKAN_MITRA -> colors.error
                    else -> colors.onPrimaryContainer
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = booking.status.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = badgeFg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = booking.serviceName,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            val paymentChipLabel = when {
                booking.paymentMethod == PaymentMethod.QRIS && booking.paymentStatus == PaymentStatus.PAID ->
                    "QRIS • Lunas"
                booking.paymentMethod == PaymentMethod.QRIS && booking.paymentStatus == PaymentStatus.WAITING_PAYMENT ->
                    "QRIS • Menunggu Bayar"
                booking.paymentMethod == PaymentMethod.QRIS ->
                    "QRIS • ${booking.paymentStatus.label}"
                else -> "Tunai • Bayar di Lokasi"
            }
            val paymentChipFg = if (booking.paymentStatus == PaymentStatus.PAID) {
                colors.success
            } else if (booking.paymentMethod == PaymentMethod.QRIS) {
                colors.warning
            } else {
                colors.textSecondary
            }
            val paymentChipBg = if (booking.paymentStatus == PaymentStatus.PAID) {
                colors.successContainer
            } else if (booking.paymentMethod == PaymentMethod.QRIS) {
                colors.warningContainer
            } else {
                colors.surfaceSubtle
            }

            BadgeContainer(
                text = paymentChipLabel,
                color = paymentChipFg,
                backgroundColor = paymentChipBg
            )

            if (booking.status == BookingStatus.PENDING && booking.expiresAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                CountdownTimerView(
                    targetEpochMillis = booking.expiresAt.toEpochMilliseconds(),
                    onExpired = onExpired
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${booking.scheduledDate} • ${booking.scheduledTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
                Text(
                    text = CurrencyUtils.formatRupiah(booking.totalCost),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (booking.paymentMethod == PaymentMethod.QRIS &&
                booking.paymentStatus == PaymentStatus.WAITING_PAYMENT &&
                booking.status.isActive
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenQrisPayment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bayar QRIS", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
