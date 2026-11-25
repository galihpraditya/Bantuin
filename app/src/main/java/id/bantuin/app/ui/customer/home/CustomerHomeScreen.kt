package id.bantuin.app.ui.customer.home

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.RankedProvider
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.common.LocationPickerDialog
import id.bantuin.app.ui.common.RatingDisplay
import id.bantuin.app.ui.customer.OrderFlowManager
import id.bantuin.app.ui.theme.BantuinTheme
import java.util.Locale

@Composable
fun CustomerHomeScreen(
    orderFlow: OrderFlowManager,
    onCategoryClick: (Int) -> Unit,
    onProviderClick: (String) -> Unit,
    onChatClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onActiveOrderClick: (String) -> Unit,
    onEmergencyClick: () -> Unit,
    hasUnreadNotifications: Boolean = false,
    repository: BantuinRepository,
    viewModel: CustomerHomeViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    LaunchedEffect(uiState.customerAddress, uiState.customerLocation) {
        orderFlow.customerAddress = uiState.customerAddress
        orderFlow.customerLocation = uiState.customerLocation
    }

    if (uiState.isLoading && uiState.categories.isEmpty()) {
        LoadingView(message = "Memuat beranda layanan...", modifier = modifier.background(colors.background))
        return
    }

    if (uiState.errorMessage != null && uiState.categories.isEmpty()) {
        ErrorView(
            message = uiState.errorMessage ?: "Gagal memuat kategori layanan.",
            onRetry = { viewModel.loadCategories() },
            modifier = modifier.background(colors.background)
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Top Bar Lokasi + Aksi Cepat (Chat & Notifikasi)
        item {
            CompactLocationTopBar(
                address = uiState.customerAddress,
                unreadChatCount = uiState.unreadChatCount,
                hasUnreadNotifications = hasUnreadNotifications,
                onLocationClick = { viewModel.setShowLocationDialog(true) },
                onChatClick = onChatClick,
                onNotificationsClick = onNotificationsClick
            )
        }

        // 2. Bilah Pencarian Jasa Aktif
        item {
            SearchBarInput(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 3. Grid Ikon Layanan 3 Kolom (ala Gojek/Grab)
        item {
            ServiceCategoryGrid3Col(
                categories = uiState.filteredCategories,
                allCategories = uiState.categories,
                searchQuery = uiState.searchQuery,
                onCategoryClick = onCategoryClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        // 4. Kartu Order Berjalan (bila ada) & Banner Darurat 24 Jam
        if (uiState.searchQuery.isBlank()) {
            val activeOrder = uiState.activeOrderSummary
            if (activeOrder != null) {
                item {
                    ActiveOrderSummaryCard(
                        booking = activeOrder,
                        onClick = { onActiveOrderClick(activeOrder.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            item {
                EmergencyBannerCard(
                    readyProviderCount = uiState.readyProviderCount,
                    onCallNow = onEmergencyClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // 5. Mitra Terdekat Siap Booking
        item {
            NearbyProvidersSection(
                nearbyProviders = uiState.nearbyProviders,
                onProviderClick = { ranked ->
                    orderFlow.selectedProvider = ranked.candidate
                    orderFlow.selectedCategoryId =
                        uiState.providerCategoryMap[ranked.candidate.providerId] ?: orderFlow.selectedCategoryId
                    onProviderClick(ranked.candidate.providerId)
                },
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }

        // 6. Banner Jaminan Garansi Layanan 7 Hari
        item {
            WarrantyBanner(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }

    if (uiState.showLocationDialog) {
        LocationPickerDialog(
            currentAddress = uiState.customerAddress,
            location = uiState.customerLocation,
            isDetectingGps = uiState.isDetectingGps,
            gpsError = uiState.gpsErrorMessage,
            onGpsDetectStart = { viewModel.setDetectingGps(true) },
            onGpsError = { msg -> viewModel.setGpsError(msg) },
            onDismiss = { viewModel.setShowLocationDialog(false) },
            onLocationConfirmed = { newAddr, newLoc, fromGps ->
                viewModel.updateLocation(newAddr, newLoc, fromGps = fromGps)
                orderFlow.customerAddress = newAddr
                orderFlow.customerLocation = newLoc
            }
        )
    }
}

@Composable
private fun CompactLocationTopBar(
    address: String,
    unreadChatCount: Int,
    hasUnreadNotifications: Boolean,
    onLocationClick: () -> Unit,
    onChatClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    val colors = BantuinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(24.dp))
                .clickable(onClick = onLocationClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Lokasi Layanan",
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lokasi Layanan",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = colors.textMuted
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Ubah lokasi",
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onChatClick,
            modifier = Modifier.size(48.dp)
        ) {
            if (unreadChatCount > 0) {
                BadgedBox(
                    badge = { Badge(containerColor = colors.primary) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Pesan Chat",
                        tint = colors.textPrimary
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Pesan Chat",
                    tint = colors.textPrimary
                )
            }
        }

        IconButton(
            onClick = onNotificationsClick,
            modifier = Modifier.size(48.dp)
        ) {
            if (hasUnreadNotifications) {
                BadgedBox(
                    badge = { Badge(containerColor = colors.primary) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifikasi",
                        tint = colors.textPrimary
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifikasi",
                    tint = colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun SearchBarInput(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Cari jasa AC, listrik, pipa, atau teknisi...",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cari layanan",
                tint = colors.textMuted
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Hapus pencarian",
                        tint = colors.textMuted
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.borderSubtle
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun ServiceCategoryGrid3Col(
    categories: List<ServiceCategory>,
    allCategories: List<ServiceCategory>,
    searchQuery: String,
    onCategoryClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Layanan Populer",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            if (allCategories.isNotEmpty()) {
                TextButton(
                    onClick = { onCategoryClick(allCategories.first().id) },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Lihat Semua",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
            }
        }

        if (categories.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        "Tidak ada kategori yang cocok dengan \"$searchQuery\"."
                    } else {
                        "Belum ada kategori layanan tersedia."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return
        }

        val iconMap = mapOf(
            1 to (Icons.Default.ElectricBolt to "Populer"),
            2 to (Icons.Default.Build to "Cepat"),
            3 to (Icons.Default.Handyman to "Teruji"),
            4 to (Icons.Default.CleaningServices to "Higienis"),
            5 to (Icons.Default.LocalShipping to "Pindahan"),
            6 to (Icons.Default.School to "Privat")
        )

        val rows = categories.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { category ->
                        val (icon, tag) = iconMap[category.id] ?: (Icons.Default.Handyman to "")
                        CompactCategoryTile(
                            category = category,
                            icon = icon,
                            tag = tag,
                            onClick = { onCategoryClick(category.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCategoryTile(
    category: ServiceCategory,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.name,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            if (tag.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.warningContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.warning,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveOrderSummaryCard(
    booking: BookingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.primary.copy(alpha = 0.4f)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BadgeContainer(
                        text = booking.status.displayName,
                        color = colors.info,
                        backgroundColor = colors.infoContainer
                    )
                    Text(
                        text = booking.scheduledTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${booking.serviceName} (${booking.providerName})",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Ketuk untuk pantau progres atau chat teknisi",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Buka detail pesanan",
                tint = colors.primary
            )
        }
    }
}

@Composable
private fun EmergencyBannerCard(
    readyProviderCount: Int,
    onCallNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A2500))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFC62828))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "DARURAT 24 JAM",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Korsleting atau AC Rusak Mendadak?",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (readyProviderCount > 0) {
                        "$readyProviderCount teknisi siaga siap meluncur cepat."
                    } else {
                        "Teknisi siaga terdekat siap membantu kapan saja."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFCC80)
                )
            }

            Button(
                onClick = onCallNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF4A2500)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = "Panggil",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF4A2500),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NearbyProvidersSection(
    nearbyProviders: List<RankedProvider>,
    onProviderClick: (RankedProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Mitra Terdekat Siap Booking",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Diurutkan dari jarak terdekat dan rating terpercaya",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (nearbyProviders.isEmpty()) {
            Text(
                text = "Belum ada mitra tersedia dalam radius 10 km.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                nearbyProviders.forEach { ranked ->
                    val provider = ranked.candidate
                    Card(
                        modifier = Modifier
                            .width(236.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onProviderClick(ranked) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = provider.name.take(1),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = provider.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (provider.badge == ProviderBadge.TRUSTED && !provider.flagAnomaly) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Terpercaya",
                                                tint = colors.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    RatingDisplay(
                                        rating = provider.ratingAvg,
                                        reviewCount = provider.reviewCount
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f km", ranked.distanceKm),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = provider.minBaseFare?.let {
                                        "Mulai ${CurrencyUtils.formatRupiahShort(it)}"
                                    } ?: "Tarif transparan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = provider.responseTimeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                                BadgeContainer(
                                    text = "Skor ${ranked.matchScore}%",
                                    color = colors.success,
                                    backgroundColor = colors.successContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarrantyBanner(modifier: Modifier = Modifier) {
    val colors = BantuinTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceSubtle),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.successContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Garansi Layanan 7 Hari",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary
                )
                Text(
                    text = "Pekerjaan tidak rapi atau kendala berulang ditangani kembali tanpa biaya tambahan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
