package id.bantuin.app.ui.customer.recommendation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.RankedProvider
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinServiceIcon
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.EmptyView
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.common.ProviderBadgeChip
import id.bantuin.app.ui.common.RatingDisplay
import id.bantuin.app.ui.customer.OrderFlowManager
import id.bantuin.app.ui.theme.BantuinTheme
import id.bantuin.app.ui.theme.bantuinPressScale
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    categoryId: Int,
    orderFlow: OrderFlowManager,
    repository: BantuinRepository,
    onNavigateBack: () -> Unit,
    onSelectProvider: (ProviderCandidate) -> Unit,
    viewModel: RecommendationViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    LaunchedEffect(categoryId, orderFlow.customerLocation) {
        orderFlow.selectedCategoryId = categoryId
        viewModel.initialize(categoryId, orderFlow.customerLocation)
    }

    val category = uiState.category
    val filteredList = uiState.filteredList
    val matchResult = uiState.matchResult

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BantuinServiceIcon(
                            categoryId = categoryId,
                            size = 38.dp,
                            cornerRadius = 12.dp
                        )
                        Column {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.textPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Radius ${uiState.maxDistanceFilter.toInt()} km • ${filteredList.size} Siap Booking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.rawCandidates.isEmpty() -> {
                LoadingView(
                    message = "Mencari mitra terbaik...",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            uiState.errorMessage != null && uiState.rawCandidates.isEmpty() -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat rekomendasi mitra.",
                    onRetry = { viewModel.loadCandidates() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Baris Filter Chips
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val chipColors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer
                            )
                            FilterChip(
                                selected = uiState.maxDistanceFilter == 10.0 && uiState.minRatingFilter == 0.0 && !uiState.onlyTrusted,
                                onClick = {
                                    viewModel.setMaxDistanceFilter(10.0)
                                    viewModel.setMinRatingFilter(0.0)
                                    viewModel.setOnlyTrusted(false)
                                },
                                label = { Text("Semua Mitra") },
                                colors = chipColors
                            )

                            FilterChip(
                                selected = uiState.maxDistanceFilter == 5.0,
                                onClick = {
                                    viewModel.setMaxDistanceFilter(if (uiState.maxDistanceFilter == 5.0) 10.0 else 5.0)
                                },
                                label = { Text("Jarak <= 5km") },
                                colors = chipColors
                            )

                            FilterChip(
                                selected = uiState.minRatingFilter == 4.8,
                                onClick = {
                                    viewModel.setMinRatingFilter(if (uiState.minRatingFilter == 4.8) 0.0 else 4.8)
                                },
                                label = { Text("Rating 4.8+") },
                                colors = chipColors
                            )

                            FilterChip(
                                selected = uiState.onlyTrusted,
                                onClick = { viewModel.setOnlyTrusted(!uiState.onlyTrusted) },
                                label = { Text("Mitra Terpercaya") },
                                colors = chipColors
                            )
                        }
                    }

                    // 2. Banner Bobot Pemeringkatan Transparan (F1)
                    item {
                        SmartMatchmakingExplainerBanner()
                    }

                    // 3. Daftar Kartu Rekomendasi Mitra Terurut
                    if (filteredList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp)
                            ) {
                                EmptyView(
                                    title = "Tidak Ada Mitra yang Sesuai",
                                    message = "Tidak ada mitra kategori ini yang memenuhi filter atau berada dalam radius 10 km.",
                                    actionText = "Reset Filter",
                                    onAction = {
                                        viewModel.setMaxDistanceFilter(10.0)
                                        viewModel.setMinRatingFilter(0.0)
                                        viewModel.setOnlyTrusted(false)
                                    }
                                )
                            }
                        }
                    } else {
                        items(filteredList, key = { it.candidate.providerId }) { ranked ->
                            ProviderRecommendationCard(
                                ranked = ranked,
                                onSelect = {
                                    orderFlow.selectedProvider = ranked.candidate
                                    onSelectProvider(ranked.candidate)
                                }
                            )
                        }
                    }

                    // 4. Catatan Mitra di Luar Radius 10 km
                    if (matchResult.outOfRange.isNotEmpty()) {
                        item {
                            OutOfRangeNoticeCard(count = matchResult.outOfRange.size)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartMatchmakingExplainerBanner() {
    val colors = BantuinTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.warningContainer),
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Pemeringkatan Mitra Otomatis",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.warning
                )
                Text(
                    text = "Skor dihitung dari 50% Jarak, 35% Rating, dan 15% Rasio Penyelesaian.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ProviderRecommendationCard(
    ranked: RankedProvider,
    onSelect: () -> Unit
) {
    val colors = BantuinTheme.colors
    val candidate = ranked.candidate

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                ProviderBadgeChip(
                    badge = candidate.badge,
                    flagAnomaly = candidate.flagAnomaly
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (candidate.flagAnomaly) colors.errorContainer else colors.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Skor ${ranked.matchScore}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (candidate.flagAnomaly) colors.error else colors.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = candidate.name.take(1),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = candidate.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = candidate.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    RatingDisplay(
                        rating = candidate.ratingAvg,
                        reviewCount = candidate.reviewCount
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceSubtle)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Jarak",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textMuted
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f km", ranked.distanceKm),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Rasio Selesai",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textMuted
                    )
                    Text(
                        text = candidate.completionRate?.let { "${(it * 100).toInt()}%" } ?: "Baru",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.success,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Respon",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textMuted
                    )
                    Text(
                        text = candidate.responseTimeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tarif Dasar Mulai",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textMuted
                    )
                    Text(
                        text = CurrencyUtils.formatRupiah(candidate.minBaseFare ?: 50000),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                val selectInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = onSelect,
                    interactionSource = selectInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .bantuinPressScale(selectInteraction, targetScale = 0.95f)
                ) {
                    Text(
                        text = "Pilih Mitra Ini",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OutOfRangeNoticeCard(count: Int) {
    val colors = BantuinTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.infoContainer),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = colors.info,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "$count mitra lain berada di luar radius 10 km sehingga tidak ditampilkan untuk menjaga ketepatan waktu kedatangan.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.info
            )
        }
    }
}
