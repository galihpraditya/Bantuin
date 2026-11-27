package id.bantuin.app.ui.customer.profile

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.LocationPickerDialog
import id.bantuin.app.ui.common.OsmMapView
import id.bantuin.app.ui.theme.BantuinTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    repository: BantuinRepository,
    onOpenSettings: () -> Unit,
    onOpenOrders: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    viewModel: CustomerProfileViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    val user = uiState.user

    var showWarrantyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Profil pelanggan berhasil disimpan")
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            BantuinTopAppBar(
                title = "Profil Pelanggan",
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Kartu Identitas Pelanggan + Ringkasan Aktivitas
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (user?.fullName ?: uiState.nameInput)
                                    .trim()
                                    .firstOrNull()
                                    ?.uppercase() ?: "B",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.fullName ?: "Pelanggan Bantuin",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = user?.email?.takeIf { it.isNotBlank() } ?: "-",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = user?.phone?.takeIf { it.isNotBlank() } ?: "Belum ada nomor HP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.openEditSheet() },
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = SolidColor(colors.borderSubtle)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary
                            )
                        }
                    }

                    HorizontalDivider(color = colors.borderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomerStatPill(
                            label = "Aktif",
                            value = uiState.activeCount.toString(),
                            highlightColor = colors.primary,
                            onClick = onOpenOrders,
                            modifier = Modifier.weight(1f)
                        )
                        CustomerStatPill(
                            label = "Perlu Aksi",
                            value = uiState.pendingActionCount.toString(),
                            highlightColor = if (uiState.pendingActionCount > 0) colors.warning else colors.textPrimary,
                            onClick = onOpenOrders,
                            modifier = Modifier.weight(1f)
                        )
                        CustomerStatPill(
                            label = "Selesai",
                            value = uiState.completedCount.toString(),
                            highlightColor = colors.success,
                            onClick = onOpenOrders,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (uiState.totalSpend > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceSubtle)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total transaksi selesai",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = CurrencyUtils.formatRupiah(uiState.totalSpend),
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Kartu Alamat Layanan Utama + Pratinjau Peta
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alamat Layanan Utama",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { viewModel.setLocationPickerVisible(true) }) {
                            Text(
                                text = "Ubah Alamat",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.addressInput.ifBlank { "Belum diatur. Pilih titik lokasi rumah Anda." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        OsmMapView(
                            centerPoint = uiState.locationInput,
                            markerTitle = user?.fullName ?: "Lokasi Anda",
                            zoomLevel = 14.5,
                            isInteractive = false
                        )
                    }
                }
            }

            // 3. Menu Pintasan & Layanan Pelanggan
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Aktivitas & Pengaturan Akun",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    CustomerActionRow(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "Riwayat Pesanan Saya",
                        subtitle = if (uiState.pendingActionCount > 0) {
                            "${uiState.pendingActionCount} pesanan menunggu pembayaran atau ulasan"
                        } else {
                            "Pantau status teknisi, pembayaran, dan riwayat servis"
                        },
                        badgeText = if (uiState.activeCount > 0) "${uiState.activeCount} Aktif" else null,
                        onClick = onOpenOrders
                    )
                    CustomerActionRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifikasi & Pembaruan",
                        subtitle = "Lihat kabar konfirmasi jadwal dan progres pengerjaan",
                        onClick = onOpenNotifications
                    )
                    CustomerActionRow(
                        icon = Icons.Default.VerifiedUser,
                        title = "Jaminan Garansi Layanan 7 Hari",
                        subtitle = "Perlindungan dana dan garansi pengerjaan ulang",
                        onClick = { showWarrantyDialog = true }
                    )
                    CustomerActionRow(
                        icon = Icons.Default.Engineering,
                        title = "Mode Mitra & Pengaturan Akun",
                        subtitle = "Beralih peran ke Mitra Kerja, tema tampilan, atau keluar akun",
                        onClick = onOpenSettings
                    )
                }
            }
        }
    }

    // Bottom Sheet Edit Data Diri
    if (uiState.isEditSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeEditSheet() },
            sheetState = sheetState,
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit Profil Pelanggan",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.errorContainer)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.nameInput,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.phoneInput,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text("Nomor HP / WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.addressInput,
                    onValueChange = viewModel::onAddressChange,
                    label = { Text("Alamat Layanan Utama") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { viewModel.setLocationPickerVisible(true) },
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(colors.primary)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Titik di Peta / GPS", color = colors.primary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.closeEditSheet() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.35f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Batal", color = colors.textPrimary)
                    }
                    Button(
                        onClick = viewModel::saveCustomerProfile,
                        enabled = !uiState.isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.65f)
                            .heightIn(min = 48.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = colors.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyimpan...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan Profil", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (uiState.isLocationPickerVisible) {
        LocationPickerDialog(
            title = "Alamat Layanan Utama",
            currentAddress = uiState.addressInput,
            location = uiState.locationInput,
            isDetectingGps = uiState.isDetectingGps,
            gpsError = uiState.gpsError,
            onGpsDetectStart = { viewModel.setDetectingGps(true) },
            onGpsError = viewModel::setGpsError,
            onDismiss = { viewModel.setLocationPickerVisible(false) },
            onLocationConfirmed = { addr, point, _ ->
                if (uiState.isEditSheetOpen) {
                    viewModel.onLocationPicked(addr, point)
                } else {
                    viewModel.savePickedLocationImmediately(addr, point)
                }
            }
        )
    }

    if (showWarrantyDialog) {
        AlertDialog(
            onDismissRequest = { showWarrantyDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Jaminan Garansi Layanan 7 Hari",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Setiap pesanan yang diselesaikan melalui Bantuin dilindungi garansi perbaikan ulang selama 7 hari jika kendala yang sama muncul kembali. Pembayaran QRIS juga ditahan aman hingga pekerjaan dinyatakan selesai.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showWarrantyDialog = false }) {
                    Text("Tutup", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun CustomerStatPill(
    label: String,
    value: String,
    highlightColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = highlightColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CustomerActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (!badgeText.isNullOrBlank()) {
                    BadgeContainer(
                        text = badgeText,
                        color = colors.onPrimaryContainer,
                        backgroundColor = colors.primaryContainer
                    )
                }
            }
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
