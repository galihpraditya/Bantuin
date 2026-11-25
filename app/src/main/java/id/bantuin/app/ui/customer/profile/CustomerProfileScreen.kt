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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LightMode
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemeMode
import id.bantuin.app.core.theme.ThemePreferenceStore
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.theme.BantuinTheme

@Composable
fun CustomerProfileScreen(
    repository: BantuinRepository,
    themeStore: ThemePreferenceStore = remember { InMemoryThemeStore() },
    onSwitchRole: () -> Unit,
    onSwitchRoleUnauthorized: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: CustomerProfileViewModel = viewModel(factory = BantuinViewModelFactory(repository, themeStore)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors
    val user = uiState.user
    val hasProviderRole = user?.roles?.contains("provider") == true
    var showUpgradeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        BantuinTopAppBar(title = "Profil Pengguna")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Kartu Profil Pengguna
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.fullName?.trim()?.firstOrNull()?.uppercase() ?: "B",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = user?.fullName ?: "Pengguna",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = user?.email ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Text(
                            text = if (uiState.activeRole == "provider") {
                                "Peran Aktif: Mitra Kerja"
                            } else {
                                "Peran Aktif: Pelanggan"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 2. Pengaturan Tampilan (Mode Terang / Gelap / Sistem)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                )
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

            if (!uiState.errorMessage.isNullOrBlank()) {
                Text(
                    text = uiState.errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error
                )
            }

            // 3. Tombol Alih atau Aktivasi Mode Mitra
            if (hasProviderRole) {
                Button(
                    onClick = {
                        viewModel.switchRole(
                            newRole = "provider",
                            onAllowed = onSwitchRole,
                            onUnauthorized = onSwitchRoleUnauthorized
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
                        viewModel.clearError()
                        showUpgradeDialog = true
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
                    Text("Aktifkan Akun Mitra Kerja")
                }
            }

            // 4. Tombol Keluar
            OutlinedButton(
                onClick = { viewModel.signOut(onSignOut) },
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.error)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun", color = colors.error)
            }
        }
    }

    if (showUpgradeDialog) {
        var addressInput by remember { mutableStateOf("Malang, Jawa Timur") }
        var bioInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = { Text("Aktifkan Profil Mitra Kerja") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Lengkapi alamat basis layanan dan ringkasan keahlian untuk mulai menerima pesanan sebagai mitra.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Alamat Basis Layanan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("Keahlian (contoh: Servis AC & Kelistrikan)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.activateProviderMode(
                            address = addressInput,
                            location = FakeData.MALANG_CENTER,
                            bio = bioInput,
                            onSuccess = {
                                showUpgradeDialog = false
                                onSwitchRole()
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
                    onClick = { showUpgradeDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Batal", color = colors.textSecondary)
                }
            }
        )
    }
}
