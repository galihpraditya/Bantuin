package id.bantuin.app.ui.customer.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import id.bantuin.app.ui.common.BantuinServiceIcon
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.EmergencyBeaconIllustration
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.HeroTechnicianIllustration
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.common.LocationPickerDialog
import id.bantuin.app.ui.common.RatingDisplay
import id.bantuin.app.ui.common.WarrantyShieldIllustration
import id.bantuin.app.ui.common.serviceTagColors
import id.bantuin.app.ui.common.serviceTagForCategory
import id.bantuin.app.ui.customer.OrderFlowManager
import id.bantuin.app.ui.theme.BantuinMotion
import id.bantuin.app.ui.theme.BantuinTheme
import id.bantuin.app.ui.theme.bantuinPressScale
import id.bantuin.app.ui.theme.rememberReducedMotion
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
    val reducedMotion = rememberReducedMotion()
    val activeOrder = uiState.activeOrderSummary
    val showFloatingTracker = activeOrder != null && uiState.searchQuery.isBlank()

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (showFloatingTracker) 104.dp else 24.dp)
        ) {
            // 1. Hero Header Oranye Melengkung + Bilah Pencarian + Kartu Lokasi Mengambang
            item {
                CurvedHeroAndQuickStrip(
                    userName = uiState.user?.fullName,
                    address = uiState.customerAddress,
                    searchQuery = uiState.searchQuery,
                    readyProviderCount = uiState.readyProviderCount,
                    unreadChatCount = uiState.unreadChatCount,
                    hasUnreadNotifications = hasUnreadNotifications,
                    activeOrder = activeOrder,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onLocationClick = { viewModel.setShowLocationDialog(true) },
                    onChatClick = onChatClick,
                    onNotificationsClick = onNotificationsClick,
                    onEmergencyClick = onEmergencyClick,
                    onActiveOrderClick = onActiveOrderClick
                )
            }

            // 2. Grid Ikon Layanan 3 Kolom Bebas Kotak Kaku + Floating Pill Tags
            item {
                ServiceCategoryGrid3Col(
                    categories = uiState.filteredCategories,
                    allCategories = uiState.categories,
                    searchQuery = uiState.searchQuery,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            // 3. Banner Darurat 24 Jam
            if (uiState.searchQuery.isBlank()) {
                item {
                    EmergencyBannerCard(
                        readyProviderCount = uiState.readyProviderCount,
                        onCallNow = onEmergencyClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // 4. Mitra Terdekat Siap Booking
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

            // 5. Banner Jaminan Garansi Layanan 7 Hari
            item {
                WarrantyBanner(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // 6. Floating Bottom Live Order Tracker Pill
        AnimatedVisibility(
            visible = showFloatingTracker,
            enter = if (reducedMotion) fadeIn() else slideInVertically(BantuinMotion.snappySpring()) { it } + fadeIn(),
            exit = if (reducedMotion) fadeOut() else slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (activeOrder != null) {
                FloatingActiveOrderBar(
                    booking = activeOrder,
                    onClick = { onActiveOrderClick(activeOrder.id) }
                )
            }
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
private fun CurvedHeroAndQuickStrip(
    userName: String?,
    address: String,
    searchQuery: String,
    readyProviderCount: Int,
    unreadChatCount: Int,
    hasUnreadNotifications: Boolean,
    activeOrder: BookingItem?,
    onSearchQueryChange: (String) -> Unit,
    onLocationClick: () -> Unit,
    onChatClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onActiveOrderClick: (String) -> Unit
) {
    val colors = BantuinTheme.colors
    val reducedMotion = rememberReducedMotion()
    val firstName = userName
        ?.trim()
        ?.split(" ")
        ?.firstOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: "Sobat Bantuin"

    val headerBrush = if (colors.isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF2E1E12), Color(0xFF211710))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFB33800), Color(0xFFC43E00))
        )
    }
    val headerTextColor = if (colors.isDark) colors.textPrimary else Color.White
    val headerSubtextColor = if (colors.isDark) colors.textSecondary else Color(0xFFFFF7ED)
    val actionCircleBg = if (colors.isDark) colors.surfaceSubtle else Color(0xFF8C2C00)
    val actionIconTint = if (colors.isDark) colors.textPrimary else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        // Latar Header Melengkung (padding bawah 26.dp memberi ruang bagi kartu lokasi mengambang di dalam batas Box)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 26.dp)
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(headerBrush)
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 46.dp)
        ) {
            // Baris 1: Search Bar Pill + Chat + Notifikasi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBarPill(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f)
                )

                HeaderCircleActionButton(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Pesan Chat",
                    hasBadge = unreadChatCount > 0,
                    badgeCount = unreadChatCount,
                    containerColor = actionCircleBg,
                    iconTint = actionIconTint,
                    onClick = onChatClick
                )

                HeaderCircleActionButton(
                    icon = Icons.Default.Notifications,
                    contentDescription = "Notifikasi",
                    hasBadge = hasUnreadNotifications,
                    badgeCount = 0,
                    containerColor = actionCircleBg,
                    iconTint = actionIconTint,
                    onClick = onNotificationsClick
                )
            }

            // Baris 2: Hero Greeting + Ilustrasi Teknisi (otomatis terlipat saat mencari)
            AnimatedVisibility(
                visible = searchQuery.isBlank(),
                enter = if (reducedMotion) fadeIn() else expandVertically() + fadeIn(),
                exit = if (reducedMotion) fadeOut() else shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Halo, $firstName!",
                            style = MaterialTheme.typography.titleMedium,
                            color = headerTextColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Cari teknisi listrik, AC, atau pipa tanpa ribet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = headerSubtextColor,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (colors.isDark) colors.surfaceSubtle else Color(0xFF8C2C00)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LivePulseDot(
                                    color = if (colors.isDark) colors.success else Color(0xFF86EFAC)
                                )
                                Text(
                                    text = if (readyProviderCount > 0) {
                                        "$readyProviderCount Mitra Siaga Sekitar"
                                    } else {
                                        "Teknisi Terverifikasi Malang"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = headerTextColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    HeroTechnicianIllustration()
                }
            }
        }

        // Kartu Lokasi & Aksi Cepat Mengambang (menumpuk tepi bawah header melengkung, tetap di dalam batas Box)
        OverlappingLocationQuickCard(
            address = address,
            activeOrder = activeOrder,
            onLocationClick = onLocationClick,
            onEmergencyClick = onEmergencyClick,
            onActiveOrderClick = onActiveOrderClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun HeaderCircleActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    hasBadge: Boolean,
    badgeCount: Int,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(48.dp)
            .bantuinPressScale(interactionSource, targetScale = 0.90f)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(
                badge = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = hasBadge,
                        enter = if (reducedMotion) fadeIn() else scaleIn(BantuinMotion.bouncySpring()) + fadeIn(),
                        exit = if (reducedMotion) fadeOut() else scaleOut() + fadeOut()
                    ) {
                        if (badgeCount > 0) {
                            Badge(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = if (badgeCount > 9) "9+" else "$badgeCount",
                                    fontSize = 10.sp
                                )
                            }
                        } else {
                            Badge(containerColor = Color(0xFFDC2626))
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun OverlappingLocationQuickCard(
    address: String,
    activeOrder: BookingItem?,
    onLocationClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onActiveOrderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    val locInteraction = remember { MutableInteractionSource() }
    val quickInteraction = remember { MutableInteractionSource() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .bantuinPressScale(locInteraction, targetScale = 0.97f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = locInteraction,
                        indication = LocalIndication.current,
                        onClick = onLocationClick
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Lokasi Layanan",
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lokasi Layanan",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.textMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = address,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Ubah lokasi",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(1.dp)
                    .height(32.dp)
                    .background(colors.borderSubtle)
            )

            // Pintasan kanan: Lacak Pesanan aktif bila ada, atau Siaga Darurat
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .bantuinPressScale(quickInteraction, targetScale = 0.94f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = quickInteraction,
                        indication = LocalIndication.current,
                        onClick = {
                            if (activeOrder != null) {
                                onActiveOrderClick(activeOrder.id)
                            } else {
                                onEmergencyClick()
                            }
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            if (activeOrder != null) colors.infoContainer else colors.errorContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeOrder != null) {
                        LivePulseDot(color = colors.info)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = if (activeOrder != null) "Pantau" else "Darurat",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (activeOrder != null) "Order Aktif" else "Siaga 24J",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBarPill(
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
                text = "Cari AC, listrik, pipa...",
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
                tint = colors.primary
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
            focusedBorderColor = colors.primaryLight,
            unfocusedBorderColor = Color.Transparent
        ),
        shape = RoundedCornerShape(26.dp),
        singleLine = true,
        modifier = modifier
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
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (categories.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
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

        val rows = categories.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { category ->
                        AiryCategoryTile(
                            category = category,
                            tag = serviceTagForCategory(category.id),
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
private fun AiryCategoryTile(
    category: ServiceCategory,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val (tagBg, tagFg) = serviceTagColors(category.id, colors.isDark)

    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .bantuinPressScale(interactionSource, targetScale = 0.93f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.padding(top = 6.dp, end = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            BantuinServiceIcon(
                categoryId = category.id,
                size = 62.dp,
                cornerRadius = 20.dp,
                isPressed = isPressed
            )

            if (tag.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-5).dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tagBg)
                        .border(1.5.dp, colors.background, RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = tagFg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
    }
}

@Composable
private fun FloatingActiveOrderBar(
    booking: BookingItem,
    onClick: () -> Unit
) {
    val colors = BantuinTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bantuinPressScale(interactionSource, targetScale = 0.97f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.infoContainer),
                contentAlignment = Alignment.Center
            ) {
                LivePulseDot(color = colors.info)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${booking.status.displayName} • ${booking.scheduledTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${booking.serviceName} (${booking.providerName})",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lacak",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LivePulseDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()
    val scale = if (reducedMotion) {
        1f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "liveDot")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotScale"
        )
        animated
    }

    Box(
        modifier = modifier
            .size(10.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun EmergencyBannerCard(
    readyProviderCount: Int,
    onCallNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctaInteraction = remember { MutableInteractionSource() }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A2500))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmergencyBeaconIllustration()

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
                interactionSource = ctaInteraction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF4A2500)
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .bantuinPressScale(ctaInteraction, targetScale = 0.94f)
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
                    val cardInteraction = remember { MutableInteractionSource() }
                    Card(
                        modifier = Modifier
                            .width(240.dp)
                            .bantuinPressScale(cardInteraction, targetScale = 0.96f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = cardInteraction,
                                indication = LocalIndication.current
                            ) { onProviderClick(ranked) },
                        shape = RoundedCornerShape(16.dp),
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
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = provider.name.take(1),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceSubtle),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WarrantyShieldIllustration()
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Garansi Layanan 7 Hari",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Dana ditahan aman di Bantuin hingga pekerjaan selesai sesuai kesepakatan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
