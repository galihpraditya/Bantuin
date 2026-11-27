package id.bantuin.app.ui.customer.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.core.util.DistanceUtils
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.RankedProvider
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinServiceIcon
import id.bantuin.app.ui.common.BantuinUiIcons
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.common.LocationPickerDialog
import id.bantuin.app.ui.common.RatingDisplay
import id.bantuin.app.ui.common.WarrantyShieldIllustration
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
    onNotificationsClick: () -> Unit,
    onActiveOrderClick: (String) -> Unit,
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
                    hasUnreadNotifications = hasUnreadNotifications,
                    activeOrder = activeOrder,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onLocationClick = { viewModel.setShowLocationDialog(true) },
                    onNotificationsClick = onNotificationsClick,
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

            // 3. Mitra Terdekat Siap Booking
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

            // 4. Banner Jaminan Garansi Layanan 7 Hari
            item {
                WarrantyBanner(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // 5. Floating Bottom Live Order Tracker Pill
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
    hasUnreadNotifications: Boolean,
    activeOrder: BookingItem?,
    onSearchQueryChange: (String) -> Unit,
    onLocationClick: () -> Unit,
    onNotificationsClick: () -> Unit,
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
        // Latar Header Melengkung dengan aksen lengkung geometris tipis di kanan (tanpa ilustrasi orang)
        val arcTint = if (colors.isDark) {
            Color(0xFFFB8C00).copy(alpha = 0.08f)
        } else {
            Color.White.copy(alpha = 0.08f)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 26.dp)
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(headerBrush)
                .drawBehind {
                    val cx = size.width * 0.94f
                    val cy = size.height * 0.62f
                    drawCircle(
                        color = arcTint,
                        radius = size.height * 0.56f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = arcTint.copy(alpha = arcTint.alpha * 0.65f),
                        radius = size.height * 0.82f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 58.dp)
        ) {
            // Baris 1: Search Bar Pill + Notifikasi (Chat sudah tersedia di Bottom Navbar)
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
                    icon = BantuinUiIcons.NotificationBell,
                    contentDescription = "Notifikasi",
                    hasBadge = hasUnreadNotifications,
                    badgeCount = 0,
                    containerColor = actionCircleBg,
                    iconTint = actionIconTint,
                    onClick = onNotificationsClick
                )
            }

            // Baris 2: Hero Greeting berskala jelas & lega tanpa ilustrasi orang (otomatis terlipat saat mencari)
            AnimatedVisibility(
                visible = searchQuery.isBlank(),
                enter = if (reducedMotion) fadeIn() else expandVertically() + fadeIn(),
                exit = if (reducedMotion) fadeOut() else shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Halo, $firstName!",
                        style = MaterialTheme.typography.headlineLarge,
                        color = headerTextColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Cari teknisi listrik, AC, atau pipa terpercaya tanpa ribet.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = headerSubtextColor,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (colors.isDark) colors.surfaceSubtle else Color(0xFF8C2C00)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
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
                                style = MaterialTheme.typography.labelMedium,
                                color = headerTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Kartu Lokasi & Aksi Cepat Mengambang (menumpuk tepi bawah header melengkung, tetap di dalam batas Box)
        OverlappingLocationQuickCard(
            address = address,
            activeOrder = activeOrder,
            onLocationClick = onLocationClick,
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
                        imageVector = BantuinUiIcons.LocationPin,
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
                            imageVector = BantuinUiIcons.ChevronDown,
                            contentDescription = "Ubah lokasi",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (activeOrder != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .width(1.dp)
                        .height(32.dp)
                        .background(colors.borderSubtle)
                )

                // Pintasan kanan: lacak pesanan yang sedang aktif
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .bantuinPressScale(quickInteraction, targetScale = 0.94f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = quickInteraction,
                            indication = LocalIndication.current,
                            onClick = { onActiveOrderClick(activeOrder.id) }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(colors.infoContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        LivePulseDot(color = colors.info)
                    }
                    Column {
                        Text(
                            text = "Pantau",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Order Aktif",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = colors.textMuted
                        )
                    }
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
                imageVector = BantuinUiIcons.Search,
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
                    onClick = { onCategoryClick(0) },
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isPriorityTrade = category.id in 1..3

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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BantuinServiceIcon(
            categoryId = category.id,
            size = 64.dp,
            cornerRadius = 20.dp,
            isPressed = isPressed
        )

        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            fontWeight = if (isPriorityTrade) FontWeight.Bold else FontWeight.SemiBold,
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
    // Indikator status aktif statis bercincin tanpa animasi berulang terus-menerus (R-19).
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
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
                                    text = if (ranked.distanceKm < 0.1) {
                                        "< 0.1 km"
                                    } else {
                                        DistanceUtils.formatKm(ranked.distanceKm)
                                    },
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
                                    text = "Respon ${provider.responseTimeText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = provider.completionRate?.let {
                                        "${(it * 100).toInt()}% selesai"
                                    } ?: "Siap booking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.success,
                                    fontWeight = FontWeight.SemiBold
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
