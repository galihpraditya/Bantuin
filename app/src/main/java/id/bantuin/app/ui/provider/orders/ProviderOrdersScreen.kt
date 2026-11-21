package id.bantuin.app.ui.provider.orders

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.EmptyView
import id.bantuin.app.ui.theme.BorderSubtle
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.NeutralBackground
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangeContainer
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.RedErrorContainer
import id.bantuin.app.ui.theme.TextMuted
import id.bantuin.app.ui.theme.TextPrimary
import id.bantuin.app.ui.theme.TextSecondary
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.ui.common.BantuinViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun ProviderOrdersScreen(
    repository: FakeBantuinRepository,
    viewModel: ProviderOrdersViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab = uiState.selectedTab
    val tabs = listOf("Menunggu", "Terkonfirmasi", "Berjalan", "Riwayat")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeutralBackground)
    ) {
        BantuinTopAppBar(title = "Antrean Pesanan Masuk")

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = NeutralSurface,
            contentColor = OrangePrimary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = OrangePrimary
                )
            }
        ) {
            tabs.forEachIndexed { index, tabName ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { viewModel.selectTab(index) },
                    text = { Text(tabName) }
                )
            }
        }

        val filteredOrders = when (selectedTab) {
            0 -> uiState.pendingOrders
            1 -> uiState.confirmedOrders
            2 -> uiState.inProgressOrders
            else -> uiState.historyOrders
        }

        if (filteredOrders.isEmpty()) {
            EmptyView(
                title = "Antrean Kosong",
                message = "Tidak ada pesanan pada status ${tabs[selectedTab].lowercase()}."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredOrders, key = { it.id }) { booking ->
                    ProviderOrderActionCard(
                        booking = booking,
                        onUpdateStatus = { newStatus ->
                            viewModel.updateStatus(booking.id, newStatus)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderOrderActionCard(
    booking: BookingItem,
    onUpdateStatus: (BookingStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
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
                    color = TextPrimary
                )
                Text(
                    text = CurrencyUtils.formatRupiah(booking.totalCost),
                    style = MaterialTheme.typography.titleMedium,
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Layanan: ${booking.serviceName}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Text(
                text = "Lokasi: ${booking.address}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${booking.scheduledDate} • ${booking.scheduledTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                if (booking.status == BookingStatus.PENDING) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RedErrorContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Batas 15 mnt",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = RedError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons sesuai Siklus Status Pesanan (PRD 6.1)
            when (booking.status) {
                BookingStatus.PENDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(BookingStatus.DITOLAK) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tolak", color = RedError)
                        }

                        Button(
                            onClick = { onUpdateStatus(BookingStatus.DIKONFIRMASI) },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batalkan Mitra", color = RedError)
                        }

                        Button(
                            onClick = { onUpdateStatus(BookingStatus.DALAM_PENGERJAAN) },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mulai Pengerjaan")
                        }
                    }
                }
                BookingStatus.DALAM_PENGERJAAN -> {
                    Button(
                        onClick = { onUpdateStatus(BookingStatus.SELESAI) },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Selesaikan Pengerjaan & Konfirmasi Bayar")
                    }
                }
                else -> {
                    // Selesai atau dibatalkan
                    Text(
                        text = "Status: ${booking.status.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
