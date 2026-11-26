package id.bantuin.app.ui.provider.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.common.OsmMapView
import id.bantuin.app.ui.common.ProviderBadgeChip
import id.bantuin.app.ui.common.RatingDisplay
import id.bantuin.app.ui.theme.BantuinTheme

@Composable
fun ProviderProfileScreen(
    repository: BantuinRepository,
    onOpenSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToServices: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToEvaluation: () -> Unit,
    viewModel: ProviderProfileViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            BantuinTopAppBar(
                title = "Profil Mitra Kerja",
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan",
                            tint = colors.textPrimary
                        )
                    }
                }
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.provider == null -> {
                LoadingView(
                    message = "Memuat profil mitra...",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            !uiState.errorMessage.isNullOrBlank() && uiState.provider == null -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat profil mitra.",
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                val provider = uiState.provider
                val displayName = provider?.name ?: uiState.user?.fullName ?: "Mitra Bantuin"
                val trust = uiState.trustResult
                val badge = trust?.badge ?: provider?.badge ?: ProviderBadge.NEW
                val isAnomaly = trust?.flagAnomaly ?: provider?.flagAnomaly ?: false

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Kartu Identitas Usaha / Teknisi
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayName.trim().firstOrNull()?.uppercase() ?: "M",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = colors.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (badge == ProviderBadge.TRUSTED && !isAnomaly) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Mitra Terpercaya",
                                                tint = colors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RatingDisplay(
                                            rating = provider?.ratingAvg ?: trust?.ratingAvg,
                                            reviewCount = provider?.reviewCount ?: 0
                                        )
                                        ProviderBadgeChip(
                                            badge = badge,
                                            flagAnomaly = isAnomaly
                                        )
                                    }
                                }
                            }

                            if (!provider?.bio.isNullOrBlank()) {
                                Text(
                                    text = provider?.bio.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = provider?.address ?: uiState.user?.defaultAddress ?: "Malang, Jawa Timur",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                BadgeContainer(
                                    text = if (uiState.isAcceptingOrders) "Buka Order" else "Tutup Sementara",
                                    color = if (uiState.isAcceptingOrders) colors.success else colors.neutralBadge,
                                    backgroundColor = if (uiState.isAcceptingOrders) {
                                        colors.successContainer
                                    } else {
                                        colors.neutralBadgeContainer
                                    }
                                )
                            }

                            if (provider != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    OsmMapView(
                                        centerPoint = provider.location,
                                        markerTitle = displayName,
                                        zoomLevel = 14.5,
                                        isInteractive = false
                                    )
                                }
                            }
                        }
                    }

                    // 2. Menu Operasional Mitra
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Pengelolaan Usaha & Layanan",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            ProviderActionRow(
                                icon = Icons.Default.Edit,
                                title = "Edit Biodata & Titik Pangkalan",
                                subtitle = "Perbarui nama usaha, deskripsi keahlian, nomor HP, dan peta basis",
                                onClick = onNavigateToEditProfile
                            )
                            ProviderActionRow(
                                icon = Icons.Default.Build,
                                title = "Katalog Layanan & Tarif",
                                subtitle = "Atur daftar jasa yang ditawarkan beserta tarif dan estimasi durasi",
                                onClick = onNavigateToServices
                            )
                            ProviderActionRow(
                                icon = Icons.Default.CalendarMonth,
                                title = "Kalender Operasional",
                                subtitle = "Atur jadwal buka harian, mode selalu siap, atau hari libur",
                                onClick = onNavigateToCalendar
                            )
                            ProviderActionRow(
                                icon = Icons.Default.Speed,
                                title = "Evaluasi Performa",
                                subtitle = "Pantau skor kepercayaan dan tingkat penyelesaian pesanan",
                                onClick = onNavigateToEvaluation
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = BantuinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
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
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMuted
        )
    }
}
