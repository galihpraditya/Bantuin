package id.bantuin.app.ui.customer.recommendation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.RankedProvider
import id.bantuin.app.domain.usecase.matchmaking.MatchmakingUseCase
import id.bantuin.app.ui.common.ProviderBadgeChip
import id.bantuin.app.ui.common.RatingDisplay
import id.bantuin.app.ui.customer.OrderFlowManager
import id.bantuin.app.ui.theme.BorderSubtle
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.GreenSuccessContainer
import id.bantuin.app.ui.theme.NeutralBackground
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangeContainer
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.TextMuted
import id.bantuin.app.ui.theme.TextPrimary
import id.bantuin.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    categoryId: Int,
    orderFlow: OrderFlowManager,
    repository: id.bantuin.app.data.repository.FakeBantuinRepository,
    onNavigateBack: () -> Unit,
    onSelectProvider: (ProviderCandidate) -> Unit,
    modifier: Modifier = Modifier
) {
    val matchmakingUseCase = remember { MatchmakingUseCase() }

    val category = remember(categoryId) {
        FakeData.categories.find { it.id == categoryId } ?: FakeData.categories[1]
    }

    // Filter states
    var maxDistanceFilter by remember { mutableDoubleStateOf(10.0) }
    var minRatingFilter by remember { mutableDoubleStateOf(0.0) }
    var onlyTrusted by remember { mutableStateOf(false) }

    var candidateList by remember { mutableStateOf<List<ProviderCandidate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    androidx.compose.runtime.LaunchedEffect(categoryId, orderFlow.customerLocation) {
        isLoading = true
        val res = repository.candidates(categoryId)
        if (res is id.bantuin.app.core.result.AppResult.Success) {
            candidateList = res.data
        }
        isLoading = false
    }

    // Evaluasi F1 Matchmaking secara dinamis berdasarkan data repositori terbaru
    val matchResult by remember(candidateList, orderFlow.customerLocation) {
        derivedStateOf {
            matchmakingUseCase(orderFlow.customerLocation, candidateList)
        }
    }

    val filteredList by remember(matchResult, maxDistanceFilter, minRatingFilter, onlyTrusted) {
        derivedStateOf {
            matchResult.ranked.filter { ranked ->
                val distPass = ranked.distanceKm <= maxDistanceFilter
                val ratingPass = (ranked.candidate.ratingAvg ?: 0.0) >= minRatingFilter
                val trustedPass = !onlyTrusted || ranked.candidate.badge == id.bantuin.app.domain.model.ProviderBadge.TRUSTED
                distPass && ratingPass && trustedPass
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Radius 10 km • ${filteredList.size} Siap Booking",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeutralBackground)
            )
        },
        containerColor = NeutralBackground,
        modifier = modifier
    ) { innerPadding ->
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
                    FilterChip(
                        selected = maxDistanceFilter == 10.0 && minRatingFilter == 0.0 && !onlyTrusted,
                        onClick = {
                            maxDistanceFilter = 10.0
                            minRatingFilter = 0.0
                            onlyTrusted = false
                        },
                        label = { Text("Semua Mitra") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeContainer,
                            selectedLabelColor = OrangePrimary
                        )
                    )

                    FilterChip(
                        selected = maxDistanceFilter == 5.0,
                        onClick = {
                            maxDistanceFilter = if (maxDistanceFilter == 5.0) 10.0 else 5.0
                        },
                        label = { Text("Jarak ≤ 5km") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeContainer,
                            selectedLabelColor = OrangePrimary
                        )
                    )

                    FilterChip(
                        selected = minRatingFilter == 4.8,
                        onClick = {
                            minRatingFilter = if (minRatingFilter == 4.8) 0.0 else 4.8
                        },
                        label = { Text("★ Rating 4.8+") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeContainer,
                            selectedLabelColor = OrangePrimary
                        )
                    )

                    FilterChip(
                        selected = onlyTrusted,
                        onClick = {
                            onlyTrusted = !onlyTrusted
                        },
                        label = { Text("Mitra Terpercaya") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeContainer,
                            selectedLabelColor = OrangePrimary
                        )
                    )
                }
            }

            // 2. Banner Transparansi Algoritma Smart Matchmaking (F1)
            item {
                SmartMatchmakingExplainerBanner()
            }

            // 3. Daftar Kartu Rekomendasi Mitra Terurut
            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada mitra yang memenuhi kriteria filter saat ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
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

@Composable
private fun SmartMatchmakingExplainerBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFE0B2)))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(OrangePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Smart Matchmaking Aktif",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFBF360C)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFBF360C))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "AI CORE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Algoritme mengombinasikan 50% Jarak, 35% Rating, dan 15% Rasio Penyelesaian tugas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD84315)
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
    val candidate = ranked.candidate

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Baris Atas: Badge Kepercayaan & Match Score
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
                        .background(if (candidate.flagAnomaly) Color(0xFFFFEBEE) else Color(0xFFFFF3E0))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Match Score ${ranked.matchScore}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (candidate.flagAnomaly) RedError else OrangePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info Mitra
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(OrangeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = candidate.name.take(1),
                        style = MaterialTheme.typography.titleLarge,
                        color = OrangePrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = candidate.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = candidate.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
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

            // 3 Metrik Cepat (Jarak, Rasio Selesai, Respon)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF7F6F4))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Jarak",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f km", ranked.distanceKm),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Rasio Selesai",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                    Text(
                        text = candidate.completionRate?.let { "${(it * 100).toInt()}%" } ?: "100%",
                        style = MaterialTheme.typography.labelMedium,
                        color = GreenSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Respon",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                    Text(
                        text = candidate.responseTimeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarif & Tombol Pilih
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tarif Dasar Mulai",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                    Text(
                        text = CurrencyUtils.formatRupiah(candidate.minBaseFare ?: 50000),
                        style = MaterialTheme.typography.titleMedium,
                        color = OrangePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF7B1FA2),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "$count mitra lain berada di luar radius 10 km (tidak ditampilkan untuk menjamin kecepatan layanan & garansi tiba tepat waktu).",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4A148C)
            )
        }
    }
}
