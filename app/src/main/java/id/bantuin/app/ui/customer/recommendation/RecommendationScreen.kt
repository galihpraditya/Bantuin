package id.bantuin.app.ui.customer.recommendation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.RadiusExpansionState
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
import kotlinx.coroutines.flow.drop

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

    var showFilterSheet by remember { mutableStateOf(false) }
    var showRankingInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(categoryId, orderFlow.customerLocation) {
        orderFlow.selectedCategoryId = categoryId
        viewModel.initialize(categoryId, orderFlow.customerLocation)
        repository.trustChanges.drop(1).collect {
            viewModel.loadCandidates(categoryId, orderFlow.customerLocation)
        }
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
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                    text = "Radius ${uiState.effectiveRadius.toInt()} km • ${filteredList.size} mitra siap",
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
                actions = {
                    IconButton(
                        onClick = { showRankingInfoDialog = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Info Pemeringkatan",
                            tint = colors.textSecondary
                        )
                    }
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter dan Urutkan Mitra",
                                tint = if (uiState.hasActiveFilterOrSort) colors.primary else colors.textPrimary
                            )
                        }
                        if (uiState.activeFilterCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.activeFilterCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = colors.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
                    // 1. Bilah Ringkas Urutan & Filter Cepat
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val chipColors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer
                            )

                            FilterChip(
                                selected = uiState.sortOption != ProviderSortOption.RECOMMENDED,
                                onClick = { showFilterSheet = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(uiState.sortOption.label) },
                                colors = chipColors,
                                modifier = Modifier.heightIn(min = 40.dp)
                            )

                            FilterChip(
                                selected = uiState.maxDistanceFilter == 5.0,
                                onClick = {
                                    viewModel.setMaxDistanceFilter(
                                        if (uiState.maxDistanceFilter == 5.0) BantuinConfig.MAX_RADIUS_KM else 5.0
                                    )
                                },
                                label = { Text("<= 5 km") },
                                colors = chipColors,
                                modifier = Modifier.heightIn(min = 40.dp)
                            )

                            FilterChip(
                                selected = uiState.minRatingFilter >= 4.8,
                                onClick = {
                                    viewModel.setMinRatingFilter(
                                        if (uiState.minRatingFilter >= 4.8) 0.0 else 4.8
                                    )
                                },
                                label = { Text("Rating 4.8+") },
                                colors = chipColors,
                                modifier = Modifier.heightIn(min = 40.dp)
                            )

                            FilterChip(
                                selected = uiState.onlyTrusted,
                                onClick = { viewModel.setOnlyTrusted(!uiState.onlyTrusted) },
                                label = { Text("Terpercaya") },
                                colors = chipColors,
                                modifier = Modifier.heightIn(min = 40.dp)
                            )

                            FilterChip(
                                selected = uiState.onlyBookmarked,
                                onClick = { viewModel.setOnlyBookmarked(!uiState.onlyBookmarked) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (uiState.onlyBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Favorit") },
                                colors = chipColors,
                                modifier = Modifier.heightIn(min = 40.dp)
                            )
                        }
                    }

                    // 1b. Banner Saran Perluasan Radius Otomatis
                    if (uiState.expansionState == RadiusExpansionState.SUGGESTING_EXPANSION) {
                        item {
                            RadiusExpansionBanner(
                                effectiveRadius = uiState.effectiveRadius,
                                potentialCount = uiState.potentialCountIfExpanded,
                                onExpand = { viewModel.acceptRadiusExpansion() },
                                onDismiss = { viewModel.dismissRadiusExpansion() }
                            )
                        }
                    }

                    // 2. Daftar Kartu Rekomendasi Mitra Terurut
                    if (filteredList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 240.dp)
                            ) {
                                EmptyView(
                                    title = "Tidak Ada Mitra yang Sesuai",
                                    message = "Tidak ada mitra kategori ini yang memenuhi kriteria filter Anda saat ini.",
                                    actionText = "Reset Filter",
                                    onAction = { viewModel.resetFilters() }
                                )
                            }
                        }
                    } else {
                        items(filteredList, key = { it.candidate.providerId }) { ranked ->
                            ProviderRecommendationCard(
                                ranked = ranked,
                                isBookmarked = uiState.bookmarkedProviderIds.contains(ranked.candidate.providerId),
                                onToggleBookmark = { viewModel.toggleBookmark(ranked.candidate) },
                                onSelect = {
                                    orderFlow.selectedProvider = ranked.candidate
                                    onSelectProvider(ranked.candidate)
                                }
                            )
                        }
                    }

                    // 3. Catatan Mitra di Luar Radius Maksimal
                    if (matchResult.outOfRange.isNotEmpty()) {
                        item {
                            OutOfRangeNoticeCard(
                                count = matchResult.outOfRange.size,
                                radiusKm = uiState.effectiveRadius.toInt()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterAndSortBottomSheet(
            currentMaxDistance = uiState.maxDistanceFilter,
            currentMinRating = uiState.minRatingFilter,
            currentOnlyTrusted = uiState.onlyTrusted,
            currentSortOption = uiState.sortOption,
            previewCountProvider = { dist, rating, trusted ->
                viewModel.previewMatchCount(dist, rating, trusted)
            },
            onDismiss = { showFilterSheet = false },
            onApply = { dist, rating, trusted, sort ->
                viewModel.applyFiltersAndSort(dist, rating, trusted, sort)
                showFilterSheet = false
            },
            onReset = {
                viewModel.resetFilters()
                showFilterSheet = false
            }
        )
    }

    if (showRankingInfoDialog) {
        AlertDialog(
            onDismissRequest = { showRankingInfoDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Cara Pemeringkatan Mitra",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Pada urutan Rekomendasi Terbaik, peringkat mitra dihitung secara otomatis berdasarkan 50% kedekatan jarak, 35% rating pelanggan, dan 15% rasio penyelesaian pesanan. Anda juga dapat mengurutkan berdasarkan jarak, rating, tarif, atau jumlah ulasan melalui tombol Filter di pojok kanan atas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showRankingInfoDialog = false }) {
                    Text("Mengerti", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterAndSortBottomSheet(
    currentMaxDistance: Double,
    currentMinRating: Double,
    currentOnlyTrusted: Boolean,
    currentSortOption: ProviderSortOption,
    previewCountProvider: (Double, Double, Boolean) -> Int,
    onDismiss: () -> Unit,
    onApply: (Double, Double, Boolean, ProviderSortOption) -> Unit,
    onReset: () -> Unit
) {
    val colors = BantuinTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draftDistance by remember(currentMaxDistance) { mutableDoubleStateOf(currentMaxDistance) }
    var draftRating by remember(currentMinRating) { mutableDoubleStateOf(currentMinRating) }
    var draftTrusted by remember(currentOnlyTrusted) { mutableStateOf(currentOnlyTrusted) }
    var draftSort by remember(currentSortOption) { mutableStateOf(currentSortOption) }

    val previewCount = previewCountProvider(draftDistance, draftRating, draftTrusted)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter & Urutkan Mitra",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onReset) {
                    Text("Reset", color = colors.primary, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(color = colors.borderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bagian 1: Urutkan Berdasarkan
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Urutkan Berdasarkan",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    ProviderSortOption.entries.forEach { option ->
                        val selected = draftSort == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) colors.primaryContainer.copy(alpha = 0.45f) else colors.surfaceSubtle)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) colors.primary else colors.borderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { draftSort = option }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { draftSort = option },
                                colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = option.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.borderSubtle)

                // Bagian 2: Batas Jarak
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Jarak Maksimal dari Lokasi Anda",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            3.0 to "Maks 3 km",
                            5.0 to "Maks 5 km",
                            BantuinConfig.MAX_RADIUS_KM to "Semua (<= ${BantuinConfig.MAX_RADIUS_KM.toInt()} km)"
                        ).forEach { (value, label) ->
                            val selected = draftDistance == value
                            SelectableOptionPill(
                                label = label,
                                selected = selected,
                                onClick = { draftDistance = value },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Bagian 3: Rating Minimum
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Rating Minimum",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0.0 to "Semua",
                            4.5 to "4.5+",
                            4.8 to "4.8+"
                        ).forEach { (value, label) ->
                            val selected = draftRating == value
                            SelectableOptionPill(
                                label = label,
                                selected = selected,
                                onClick = { draftRating = value },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Bagian 4: Hanya Mitra Terpercaya
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceSubtle)
                        .clickable { draftTrusted = !draftTrusted }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hanya Mitra Terpercaya",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tampilkan teknisi dengan performa dan keandalan terverifikasi",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = draftTrusted,
                        onCheckedChange = { draftTrusted = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.35f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Batal", color = colors.textPrimary)
                }
                Button(
                    onClick = { onApply(draftDistance, draftRating, draftTrusted, draftSort) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.65f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Terapkan ($previewCount Mitra)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableOptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.primaryContainer else colors.surfaceSubtle)
            .border(
                width = 1.dp,
                color = if (selected) colors.primary else colors.borderSubtle,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onPrimaryContainer else colors.textPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProviderRecommendationCard(
    ranked: RankedProvider,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onSelect: () -> Unit
) {
    val colors = BantuinTheme.colors
    val candidate = ranked.candidate

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(colors.borderSubtle)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val hasBadge = candidate.flagAnomaly || candidate.badge.name == "TRUSTED"
            if (hasBadge) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProviderBadgeChip(
                        badge = candidate.badge,
                        flagAnomaly = candidate.flagAnomaly
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = candidate.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = candidate.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    RatingDisplay(
                        rating = candidate.ratingAvg,
                        reviewCount = candidate.reviewCount
                    )
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Hapus dari Favorit" else "Simpan ke Favorit",
                        tint = if (isBookmarked) colors.primary else colors.textMuted
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
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
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
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
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
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
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
private fun OutOfRangeNoticeCard(count: Int, radiusKm: Int) {
    val colors = BantuinTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.infoContainer),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(colors.borderSubtle)
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
                text = "$count mitra lain berada di luar radius $radiusKm km sehingga tidak ditampilkan untuk menjaga ketepatan waktu kedatangan.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.info
            )
        }
    }
}

@Composable
private fun RadiusExpansionBanner(
    effectiveRadius: Double,
    potentialCount: Int,
    onExpand: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = BantuinTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.warningContainer),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(colors.warning)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Perluas Radius Pencarian?",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tidak ada mitra dalam radius ${effectiveRadius.toInt()} km. Tersedia $potentialCount mitra dalam radius ${BantuinConfig.MAX_RADIUS_KM_EXPANDED.toInt()} km.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExpand,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Perluas ke ${BantuinConfig.MAX_RADIUS_KM_EXPANDED.toInt()} km",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Abaikan",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}
