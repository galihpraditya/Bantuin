package id.bantuin.app.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.settings.InMemorySettingsStore
import id.bantuin.app.core.settings.SettingsPreferenceStore
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemeMode
import id.bantuin.app.core.theme.ThemePreferenceStore
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.LocationPickerDialog
import id.bantuin.app.ui.theme.BantuinTheme

@Composable
fun SettingsScreen(
    repository: BantuinRepository,
    themeStore: ThemePreferenceStore = remember { InMemoryThemeStore() },
    settingsStore: SettingsPreferenceStore = remember { InMemorySettingsStore() },
    onNavigateBack: () -> Unit,
    onSwitchedToProvider: () -> Unit,
    onSwitchedToCustomer: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = BantuinViewModelFactory(repository, themeStore, settingsStore)
    ),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    val user = uiState.user
    val hasProviderRole = user?.roles?.contains("provider") == true
    val isProviderActive = uiState.activeRole == "provider"

    var showProviderRegistrationDialog by rememberSaveable { mutableStateOf(false) }
    var showLocationPicker by rememberSaveable { mutableStateOf(false) }
    var providerAddress by rememberSaveable {
        mutableStateOf(user?.defaultAddress ?: "Malang, Jawa Timur")
    }
    var providerLat by rememberSaveable {
        mutableDoubleStateOf(user?.defaultLocation?.lat ?: BantuinConfig.MALANG_CENTER.lat)
    }
    var providerLng by rememberSaveable {
        mutableDoubleStateOf(user?.defaultLocation?.lng ?: BantuinConfig.MALANG_CENTER.lng)
    }
    var providerBio by rememberSaveable { mutableStateOf("") }
    var isDetectingGps by rememberSaveable { mutableStateOf(false) }
    var gpsError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.successMessage) {
        val msg = uiState.successMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            BantuinTopAppBar(
                title = "Pengaturan",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
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
            // 1. Tampilan Aplikasi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tampilan Aplikasi",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val options = listOf(
                        Triple(ThemeMode.SYSTEM, "Ikuti Sistem", Icons.Default.BrightnessAuto),
                        Triple(ThemeMode.LIGHT, "Mode Terang", Icons.Default.LightMode),
                        Triple(ThemeMode.DARK, "Mode Gelap", Icons.Default.DarkMode)
                    )
                    options.forEach { (mode, label, icon) ->
                        val isSelected = uiState.themeMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.setThemeMode(mode) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) colors.primary else colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setThemeMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                            )
                        }
                    }
                }
            }

            // 2. Preferensi Notifikasi (Spesifik Peran Aktif)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isProviderActive) {
                            "Notifikasi Mitra Kerja"
                        } else {
                            "Notifikasi Pelanggan"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    val notif = uiState.notificationSettings
                    NotificationToggleRow(
                        title = "Pembaruan Status Pesanan",
                        subtitle = "Info konfirmasi, perjalanan, dan penyelesaian layanan",
                        checked = notif.orderUpdatesEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationSettings(notif.copy(orderUpdatesEnabled = it))
                        }
                    )
                    NotificationToggleRow(
                        title = "Pengingat Jadwal Layanan",
                        subtitle = "Pengingat sebelum jam pengerjaan dimulai",
                        checked = notif.scheduleReminderEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationSettings(notif.copy(scheduleReminderEnabled = it))
                        }
                    )
                    if (isProviderActive) {
                        NotificationToggleRow(
                            title = "Peringatan Antrean 15 Menit",
                            subtitle = "Batas waktu terima pesanan sebelum kedaluwarsa",
                            checked = notif.providerQueueAlertEnabled,
                            onCheckedChange = {
                                viewModel.updateNotificationSettings(
                                    notif.copy(providerQueueAlertEnabled = it)
                                )
                            }
                        )
                        NotificationToggleRow(
                            title = "Notifikasi Pencairan Saldo",
                            subtitle = "Status penarikan dana QRIS ke bank atau e-wallet",
                            checked = notif.payoutAlertEnabled,
                            onCheckedChange = {
                                viewModel.updateNotificationSettings(notif.copy(payoutAlertEnabled = it))
                            }
                        )
                    }
                }
            }

            // 3. Peran Akun & Mode Aplikasi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Peran Akun & Mode Aplikasi",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isProviderActive) {
                            "Mode aktif saat ini: Mitra Kerja. Anda dapat beralih ke mode Pelanggan kapan saja tanpa keluar akun."
                        } else if (hasProviderRole) {
                            "Mode aktif saat ini: Pelanggan. Anda juga terdaftar sebagai Mitra Kerja Bantuin."
                        } else {
                            "Mode aktif saat ini: Pelanggan. Daftar sebagai Mitra Kerja untuk mulai menerima pesanan."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    if (isProviderActive) {
                        Button(
                            onClick = {
                                viewModel.switchRoleWithoutSignOut(
                                    targetRole = "customer",
                                    onSwitched = onSwitchedToCustomer,
                                    onNeedsProviderRegistration = {}
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Beralih ke Mode Pelanggan")
                        }
                    } else if (hasProviderRole) {
                        Button(
                            onClick = {
                                viewModel.switchRoleWithoutSignOut(
                                    targetRole = "provider",
                                    onSwitched = onSwitchedToProvider,
                                    onNeedsProviderRegistration = {
                                        viewModel.clearMessages()
                                        showProviderRegistrationDialog = true
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Beralih ke Mode Mitra Kerja")
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.clearMessages()
                                showProviderRegistrationDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daftar Jadi Mitra Kerja Bantuin")
                        }
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Text(
                    text = uiState.errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error
                )
            }

            // 4. Keluar dari Akun
            OutlinedButton(
                onClick = { viewModel.signOut(onSignOut) },
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = SolidColor(colors.error)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun", color = colors.error)
            }
        }
    }

    if (showProviderRegistrationDialog) {
        AlertDialog(
            onDismissRequest = { showProviderRegistrationDialog = false },
            title = { Text("Aktivasi Akun Mitra Kerja") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.errorContainer)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error
                            )
                        }
                    }
                    Text(
                        text = "Akun Anda tetap masuk. Tentukan titik pangkalan pada peta dan ringkasan keahlian untuk membuka mode Mitra.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = providerAddress,
                        onValueChange = { providerAddress = it },
                        label = { Text("Alamat Basis Layanan") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    OutlinedButton(
                        onClick = { showLocationPicker = true },
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = SolidColor(colors.primary)
                        ),
                        shape = RoundedCornerShape(10.dp),
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
                        Text("Pilih Titik Pangkalan di Peta", color = colors.primary)
                    }
                    OutlinedTextField(
                        value = providerBio,
                        onValueChange = { providerBio = it },
                        label = { Text("Keahlian (contoh: Servis AC & Kelistrikan)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.activateProviderRole(
                            address = providerAddress,
                            location = GeoPoint(providerLat, providerLng),
                            bio = providerBio,
                            onSuccess = {
                                showProviderRegistrationDialog = false
                                onSwitchedToProvider()
                            }
                        )
                    },
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (uiState.isLoading) "Mengaktifkan..." else "Aktifkan Mitra")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showProviderRegistrationDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Batal", color = colors.textSecondary)
                }
            }
        )
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            title = "Titik Pangkalan Mitra",
            currentAddress = providerAddress,
            location = GeoPoint(providerLat, providerLng),
            isDetectingGps = isDetectingGps,
            gpsError = gpsError,
            onGpsDetectStart = { isDetectingGps = true; gpsError = null },
            onGpsError = { err -> isDetectingGps = false; gpsError = err },
            onDismiss = { showLocationPicker = false },
            onLocationConfirmed = { addr, point, _ ->
                providerAddress = addr
                providerLat = point.lat
                providerLng = point.lng
                isDetectingGps = false
                showLocationPicker = false
            }
        )
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = BantuinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary
            )
        )
    }
}
