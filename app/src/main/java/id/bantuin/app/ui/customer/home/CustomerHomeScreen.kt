package id.bantuin.app.ui.customer.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.location.LocationHelper
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.RankedProvider
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.common.OsmMapView
import id.bantuin.app.ui.common.RatingDisplay
import id.bantuin.app.ui.customer.OrderFlowManager
import id.bantuin.app.ui.theme.BantuinTheme
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

@Composable
fun CustomerHomeScreen(
    orderFlow: OrderFlowManager,
    onCategoryClick: (Int) -> Unit,
    onProviderClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onSwitchToProvider: () -> Unit,
    onSwitchToProviderUnauthorized: () -> Unit,
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
        // 1. Header & Sapaan
        item {
            CustomerHeaderSection(
                customerName = uiState.user?.fullName ?: "Pengguna",
                hasUnreadNotifications = hasUnreadNotifications,
                onNotificationsClick = onNotificationsClick,
                onSwitchRole = {
                    viewModel.switchToProvider(
                        onAllowed = onSwitchToProvider,
                        onUnauthorized = onSwitchToProviderUnauthorized
                    )
                }
            )
        }

        // 2. Kartu Lokasi Pelanggan
        item {
            LocationCard(
                address = uiState.customerAddress,
                locationSourceLabel = uiState.locationSourceLabel,
                isFromGps = uiState.isLocationFromGps,
                onEditLocation = { viewModel.setShowLocationDialog(true) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 3. Bilah Pencarian Jasa Aktif
        item {
            SearchBarInput(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 4. Kartu Darurat 24 Jam (sembunyikan saat pengguna mengetik pencarian)
        if (uiState.searchQuery.isBlank()) {
            item {
                EmergencyHeroCard(
                    readyProviderCount = uiState.readyProviderCount,
                    onCallNow = onEmergencyClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // 5. Grid Kategori Layanan
        item {
            CategoryGridSection(
                categories = uiState.filteredCategories,
                allCategories = uiState.categories,
                searchQuery = uiState.searchQuery,
                onCategoryClick = onCategoryClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 6. Mitra Siap Tanggap Terdekat
        item {
            NearbyProvidersSection(
                nearbyProviders = uiState.nearbyProviders,
                onProviderClick = { ranked ->
                    orderFlow.selectedProvider = ranked.candidate
                    orderFlow.selectedCategoryId =
                        uiState.providerCategoryMap[ranked.candidate.providerId] ?: orderFlow.selectedCategoryId
                    onProviderClick(ranked.candidate.providerId)
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // 7. Banner Jaminan Garansi Layanan 7 Hari
        item {
            WarrantyBanner(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
private fun CustomerHeaderSection(
    customerName: String,
    hasUnreadNotifications: Boolean,
    onNotificationsClick: () -> Unit,
    onSwitchRole: () -> Unit
) {
    val colors = BantuinTheme.colors
    val initial = customerName.trim().firstOrNull()?.uppercase() ?: "B"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Handyman,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bantuin",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNotificationsClick) {
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
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column {
            Text(
                text = "Halo, $customerName!",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary
            )
            Text(
                text = "Butuh bantuan teknisi atau jasa apa hari ini?",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Switch Mode: Pelanggan vs Mitra
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surfaceSubtle)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(24.dp))
                .padding(3.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.primary)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pelanggan",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSwitchRole() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mitra Kerja",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationCard(
    address: String,
    locationSourceLabel: String,
    isFromGps: Boolean,
    onEditLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LOKASI LAYANAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isFromGps) colors.successContainer else colors.infoContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = locationSourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFromGps) colors.success else colors.info
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onEditLocation,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.outline)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Ubah",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary
                    )
                }
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
                text = "Cari jasa: AC bocor, listrik, pipa...",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted
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
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Hapus pencarian",
                        tint = colors.textMuted
                    )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun EmergencyHeroCard(
    readyProviderCount: Int,
    onCallNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A2500))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFC62828))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "DARURAT 24 JAM",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Respon Cepat",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFCC80)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Korsleting Listrik atau AC Rusak Malam Ini?",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Teknisi siaga terdekat siap meluncur ke kos atau rumah Anda.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFE0B2)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCallNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF4A2500)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Panggil Sekarang",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4A2500),
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (readyProviderCount > 0) "$readyProviderCount Mitra Terjangkau" else "Cari Mitra Siaga",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFCC80)
                )
            }
        }
    }
}

@Composable
private fun CategoryGridSection(
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
                text = "Kategori Layanan",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
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

        Spacer(modifier = Modifier.height(6.dp))

        if (categories.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                )
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

        val chunked = categories.chunked(2)
        chunked.forEach { rowCategories ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowCategories.forEach { category ->
                    val (icon, tag) = iconMap[category.id] ?: (Icons.Default.Handyman to "")
                    CategoryCardItem(
                        category = category,
                        icon = icon,
                        tag = tag,
                        onClick = { onCategoryClick(category.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCardItem(
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
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                color = colors.textPrimary
            )
            Text(
                text = "Diurutkan berdasarkan jarak dan reputasi dari lokasi Anda",
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
                            .width(232.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onProviderClick(ranked) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
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
                                Column {
                                    Text(
                                        text = provider.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    RatingDisplay(
                                        rating = provider.ratingAvg,
                                        reviewCount = provider.reviewCount
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

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
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.successContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Skor ${ranked.matchScore}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = colors.success,
                                        fontWeight = FontWeight.Bold
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

@Composable
private fun WarrantyBanner(modifier: Modifier = Modifier) {
    val colors = BantuinTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceSubtle),
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

@Composable
private fun LocationPickerDialog(
    currentAddress: String,
    location: GeoPoint,
    isDetectingGps: Boolean,
    gpsError: String?,
    onGpsDetectStart: () -> Unit,
    onGpsError: (String?) -> Unit,
    onDismiss: () -> Unit,
    onLocationConfirmed: (String, GeoPoint, Boolean) -> Unit
) {
    val colors = BantuinTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val presets = remember {
        listOf(
            Triple("Alun-alun Malang", GeoPoint(-7.9797, 112.6304), "Jl. Merdeka Barat, Alun-alun Malang"),
            Triple("UB / Suhat", GeoPoint(-7.9467, 112.6156), "Jl. Soekarno Hatta No. 9, Lowokwaru, Malang"),
            Triple("Sawojajar", GeoPoint(-7.9772, 112.6562), "Jl. Danau Toba, Sawojajar, Malang"),
            Triple("Dinoyo", GeoPoint(-7.9405, 112.6074), "Jl. MT Haryono No. 193, Dinoyo, Malang")
        )
    }

    var textInput by rememberSaveable { mutableStateOf(currentAddress) }
    var selectedLat by rememberSaveable { mutableDoubleStateOf(location.lat) }
    var selectedLng by rememberSaveable { mutableDoubleStateOf(location.lng) }
    val selectedLocation = GeoPoint(selectedLat, selectedLng)
    var fromGps by rememberSaveable { mutableStateOf(false) }

    fun fetchHardwareGps() {
        onGpsDetectStart()
        scope.launch {
            val detected = LocationHelper.getCurrentLocation(context)
            if (detected != null) {
                selectedLat = detected.point.lat
                selectedLng = detected.point.lng
                textInput = detected.address ?: LocationHelper.formatCoordinateFallback(detected.point)
                fromGps = true
                onGpsError(null)
            } else {
                onGpsError("Lokasi GPS tidak tersedia. Ketuk peta atau pilih area cepat di bawah.")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchHardwareGps()
        } else {
            onGpsError("Izin lokasi ditolak. Silakan ketuk peta atau pilih area secara manual.")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Titik Lokasi Layanan") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedButton(
                    onClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            fetchHardwareGps()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !isDetectingGps,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.primary)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    if (isDetectingGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mendeteksi GPS...", color = colors.primary)
                    } else {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gunakan Lokasi GPS Saya", color = colors.primary)
                    }
                }

                if (!gpsError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = gpsError,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Atau pilih area cepat / ketuk titik pada peta:",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (name, point, addr) ->
                        val isSelected = abs(selectedLocation.lat - point.lat) < 0.001 &&
                            abs(selectedLocation.lng - point.lng) < 0.001
                        Button(
                            onClick = {
                                selectedLat = point.lat
                                selectedLng = point.lng
                                textInput = addr
                                fromGps = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) colors.primary else colors.surface,
                                contentColor = if (isSelected) colors.onPrimary else colors.textPrimary
                            ),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isSelected) colors.primary else colors.outline
                                )
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) colors.onPrimary else colors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    OsmMapView(
                        centerPoint = selectedLocation,
                        markerTitle = "Lokasi Layanan",
                        zoomLevel = 14.5,
                        isInteractive = true,
                        onLocationSelected = { tappedPoint ->
                            selectedLat = tappedPoint.lat
                            selectedLng = tappedPoint.lng
                            fromGps = false
                            scope.launch {
                                textInput = LocationHelper.reverseGeocode(context, tappedPoint.lat, tappedPoint.lng)
                                    ?: LocationHelper.formatCoordinateFallback(tappedPoint)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Alamat Lengkap") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onLocationConfirmed(textInput.ifBlank { LocationHelper.formatCoordinateFallback(selectedLocation) }, selectedLocation, fromGps) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Simpan Lokasi")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Batal", color = colors.textSecondary)
            }
        }
    )
}
