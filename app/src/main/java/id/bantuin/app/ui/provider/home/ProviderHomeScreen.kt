package id.bantuin.app.ui.provider.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.navigation.Routes
import id.bantuin.app.ui.theme.BantuinTheme

@Composable
fun ProviderHomeScreen(
    repository: BantuinRepository,
    onNavigateToRoute: (String) -> Unit,
    onSwitchToCustomer: () -> Unit,
    onSwitchToCustomerUnauthorized: () -> Unit,
    viewModel: ProviderHomeViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    if (uiState.isLoading && uiState.provider == null) {
        LoadingView(
            message = "Memuat beranda mitra...",
            modifier = modifier.background(colors.background)
        )
        return
    }

    if (uiState.errorMessage != null && uiState.provider == null && uiState.user == null) {
        ErrorView(
            message = uiState.errorMessage ?: "Gagal memuat data mitra.",
            onRetry = { viewModel.loadData() },
            modifier = modifier.background(colors.background)
        )
        return
    }

    val trust = uiState.trustResult
    val scoreText = trust?.trustScore?.toInt()?.toString() ?: "70"
    val trustStatusLabel = when {
        trust == null -> "Aktif"
        trust.flagAnomaly -> "Evaluasi"
        trust.txCount < BantuinConfig.MIN_TX_FOR_EVALUATION -> "Mitra Baru"
        trust.trustScore >= 85.0 -> "Sangat Baik"
        else -> "Baik"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Ringkasan Mitra
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.user?.fullName?.trim()?.firstOrNull()?.uppercase() ?: "M",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = uiState.user?.fullName?.let { "Halo, $it!" } ?: "Halo, Mitra!",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Mode Mitra Kerja • ${uiState.specialtyLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.switchToCustomer(
                                onAllowed = onSwitchToCustomer,
                                onUnauthorized = onSwitchToCustomerUnauthorized
                            )
                        },
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(colors.outline)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Pelanggan", style = MaterialTheme.typography.labelSmall, color = colors.textPrimary)
                    }
                }
            }
        }

        // 2. Tiga Kartu Ringkasan Operasional
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    title = "Menunggu",
                    count = uiState.pendingCount.toString(),
                    subtext = "Perlu Respon",
                    color = if (uiState.pendingCount > 0) colors.primary else colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToRoute(Routes.PROVIDER_ORDERS) }
                )
                SummaryCard(
                    title = "Hari Ini",
                    count = uiState.todayOrdersCount.toString(),
                    subtext = "Jadwal Aktif",
                    color = colors.info,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToRoute(Routes.PROVIDER_CALENDAR) }
                )
                SummaryCard(
                    title = "Trust Score",
                    count = scoreText,
                    subtext = trustStatusLabel,
                    color = if (trust?.flagAnomaly == true) colors.error else colors.success,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToRoute(Routes.PROVIDER_DASHBOARD) }
                )
            }
        }

        // 3. Pintasan Cepat
        item {
            Text(
                text = "Kelola Layanan & Jadwal",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        }

        item {
            ActionTile(
                title = "Edit Profil & Pangkalan",
                subtitle = "Ubah biodata, kontak, dan titik lokasi pangkalan mitra",
                icon = Icons.Default.Person,
                onClick = { onNavigateToRoute(Routes.PROVIDER_PROFILE_EDIT) }
            )
        }

        item {
            ActionTile(
                title = "Antrean Pesanan Masuk",
                subtitle = "Terima atau tolak pesanan baru dengan batas 15 menit",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                onClick = { onNavigateToRoute(Routes.PROVIDER_ORDERS) }
            )
        }

        item {
            ActionTile(
                title = "Pengaturan Kalender Ketersediaan",
                subtitle = "Atur hari operasional OFF, ON, atau ALWAYS_ON per hari",
                icon = Icons.Default.CalendarMonth,
                onClick = { onNavigateToRoute(Routes.PROVIDER_CALENDAR) }
            )
        }

        item {
            ActionTile(
                title = "Katalog Layanan & Tarif",
                subtitle = "Ubah tarif dasar, estimasi durasi, dan jenis layanan",
                icon = Icons.Default.Handyman,
                onClick = { onNavigateToRoute(Routes.PROVIDER_SERVICES) }
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    count: String,
    subtext: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
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
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
