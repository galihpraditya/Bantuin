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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.ui.navigation.Routes
import id.bantuin.app.ui.theme.BorderSubtle
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.NeutralBackground
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangeContainer
import id.bantuin.app.ui.theme.OrangePrimary
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.theme.TextMuted
import id.bantuin.app.ui.theme.TextPrimary
import id.bantuin.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ProviderHomeScreen(
    repository: FakeBantuinRepository,
    onNavigateToRoute: (String) -> Unit,
    onSwitchToCustomer: () -> Unit,
    viewModel: ProviderHomeViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NeutralBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Ringkasan Mitra
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(OrangeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "B",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = OrangePrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Halo, Pak Budi!",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Mode Mitra Kerja • Teknisi AC",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OrangePrimary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.switchToCustomer(onSwitchToCustomer)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0EAE1)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Pelanggan", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // 2. Tiga Kartu Ringkasan Operasional Hari Ini
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    title = "Menunggu",
                    count = "1",
                    subtext = "Perlu Respon",
                    color = OrangePrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToRoute(Routes.PROVIDER_ORDERS) }
                )
                SummaryCard(
                    title = "Jadwal Hari Ini",
                    count = "2",
                    subtext = "Servis AC",
                    color = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToRoute(Routes.PROVIDER_CALENDAR) }
                )
                SummaryCard(
                    title = "Trust Score",
                    count = "92",
                    subtext = "Sangat Baik",
                    color = GreenSuccess,
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
                color = TextPrimary
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
                icon = Icons.Default.ReceiptLong,
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
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextMuted
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
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OrangeContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
