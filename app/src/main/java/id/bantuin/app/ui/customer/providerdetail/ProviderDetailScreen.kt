package id.bantuin.app.ui.customer.providerdetail

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.ServiceItem
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailScreen(
    providerId: String,
    orderFlow: OrderFlowManager,
    onNavigateBack: () -> Unit,
    onServiceSelected: (ServiceItem) -> Unit,
    repository: BantuinRepository,
    viewModel: ProviderDetailViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    LaunchedEffect(providerId) {
        val initial = orderFlow.selectedProvider?.takeIf { it.providerId == providerId }
        viewModel.loadProviderDetail(providerId = providerId, initialProvider = initial)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detail Mitra & Layanan",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
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
                    if (uiState.provider != null) {
                        IconButton(
                            onClick = { viewModel.toggleBookmark(categoryId = orderFlow.selectedCategoryId) }
                        ) {
                            Icon(
                                imageVector = if (uiState.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (uiState.isBookmarked) "Hapus dari Favorit" else "Simpan ke Favorit",
                                tint = if (uiState.isBookmarked) colors.primary else colors.textSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        val provider = uiState.provider

        when {
            uiState.isLoading && provider == null -> {
                LoadingView(
                    message = "Memuat profil mitra...",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            uiState.errorMessage != null && provider == null -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Profil mitra tidak tersedia.",
                    onRetry = {
                        viewModel.loadProviderDetail(providerId, orderFlow.selectedProvider)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            provider == null -> {
                EmptyView(
                    title = "Mitra Tidak Ditemukan",
                    message = "Data mitra belum tersedia. Silakan kembali ke daftar rekomendasi.",
                    actionText = "Kembali",
                    onAction = onNavigateBack,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Profil Ringkas Mitra
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(colors.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = provider.name.take(1),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = colors.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = provider.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = provider.bio.ifBlank { "Mitra teknisi layanan Bantuin" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RatingDisplay(
                                                rating = provider.ratingAvg,
                                                reviewCount = provider.reviewCount
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            ProviderBadgeChip(
                                                badge = provider.badge,
                                                flagAnomaly = provider.flagAnomaly
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = provider.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // 2. Daftar Layanan yang Tersedia
                    item {
                        Text(
                            text = "Pilih Jenis Layanan",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }

                    if (uiState.services.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                                )
                            ) {
                                Text(
                                    text = "Mitra belum mengaktifkan layanan untuk kategori ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(uiState.services, key = { it.id }) { service ->
                            val serviceInteraction = remember { MutableInteractionSource() }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bantuinPressScale(serviceInteraction, targetScale = 0.97f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(
                                        interactionSource = serviceInteraction,
                                        indication = LocalIndication.current
                                    ) {
                                        orderFlow.selectedProvider = provider
                                        orderFlow.selectedService = service
                                        orderFlow.selectedDurationMin = service.durationMin
                                        onServiceSelected(service)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BantuinServiceIcon(
                                            categoryId = service.categoryId,
                                            size = 38.dp,
                                            cornerRadius = 12.dp
                                        )
                                        Text(
                                            text = service.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = CurrencyUtils.formatRupiah(service.baseFare),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (!service.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = service.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = colors.textMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = TimeUtils.formatDuration(service.durationMin),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textMuted
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                orderFlow.selectedProvider = provider
                                                orderFlow.selectedService = service
                                                orderFlow.selectedDurationMin = service.durationMin
                                                onServiceSelected(service)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.primary,
                                                contentColor = colors.onPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Pilih Layanan")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Ulasan Pelanggan Sebelumnya
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ulasan Pelanggan Terverifikasi",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }

                    if (uiState.reviews.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                                )
                            ) {
                                Text(
                                    text = "Belum ada ulasan dari pelanggan untuk mitra ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else {
                        items(uiState.reviews, key = { it.id }) { review ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
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
                                            text = review.customerName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = review.createdAtText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        repeat(review.rating) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = colors.starTint,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    if (!review.comment.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = review.comment,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
