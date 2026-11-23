package id.bantuin.app.ui.provider.services

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.theme.BorderSubtle
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.NeutralBackground
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.TextMuted
import id.bantuin.app.ui.theme.TextPrimary
import id.bantuin.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ProviderServicesScreen(
    repository: BantuinRepository,
    viewModel: ProviderServicesViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var serviceToDelete by remember { mutableStateOf<ServiceItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeutralBackground)
    ) {
        BantuinTopAppBar(title = "Katalog Layanan & Tarif")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Banner Error bila ada
            if (uiState.errorMessage != null && !uiState.isUpsertDialogOpen) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Tutup")
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daftar Layanan Ditawarkan",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Kelola tarif dasar dan durasi estimasi pengerjaan",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.openAddDialog()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah")
                    }
                }
            }

            if (uiState.isLoading && uiState.services.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Memuat Katalog Layanan...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else if (uiState.services.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Belum Ada Layanan",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Klik tombol Tambah untuk membuat penawaran jasa pertama Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(uiState.services, key = { it.id }) { service ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = service.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = service.isActive,
                                        onCheckedChange = { _ ->
                                            viewModel.toggleServiceActive(service)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = GreenSuccess
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            viewModel.openEditDialog(service)
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Ubah Layanan",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            serviceToDelete = service
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus Layanan",
                                            tint = RedError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (!service.description.isNullOrBlank()) {
                                Text(
                                    text = service.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estimasi: ${TimeUtils.formatDuration(service.durationMin)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = CurrencyUtils.formatRupiah(service.baseFare),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Upsert Service Dialog
    if (uiState.isUpsertDialogOpen) {
        UpsertServiceDialog(
            existing = uiState.editingService,
            providerId = uiState.providerId,
            categories = uiState.categories,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { savedService ->
                viewModel.saveService(savedService)
            }
        )
    }

    // Modal Konfirmasi Hapus
    if (serviceToDelete != null) {
        val target = serviceToDelete!!
        AlertDialog(
            onDismissRequest = { serviceToDelete = null },
            title = { Text("Hapus Layanan?") },
            text = { Text("Layanan '${target.name}' akan dihapus dari katalog penawaran Anda.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteService(target.id)
                        serviceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { serviceToDelete = null },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun UpsertServiceDialog(
    existing: ServiceItem?,
    providerId: String,
    categories: List<id.bantuin.app.domain.model.ServiceCategory> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (ServiceItem) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var selectedCategory by remember { mutableIntStateOf(existing?.categoryId ?: 2) }
    var baseFareText by remember { mutableStateOf(existing?.baseFare?.toString() ?: "75000") }
    var durationText by remember { mutableStateOf(existing?.durationMin?.toString() ?: "90") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val resolvedCategories = if (categories.isNotEmpty()) categories else FakeData.categories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Tambah Layanan Baru" else "Ubah Layanan")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Nama Layanan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Keterangan Singkat (opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Kategori Layanan:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    resolvedCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.id,
                            onClick = { selectedCategory = cat.id },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangePrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = baseFareText,
                    onValueChange = {
                        baseFareText = it.filter { ch -> ch.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Tarif Dasar (Rp, kelipatan 500)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = {
                        durationText = it.filter { ch -> ch.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Estimasi Durasi (Menit, 15 - 720)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = RedError
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Nama layanan wajib diisi"
                        return@Button
                    }
                    val fare = baseFareText.toIntOrNull() ?: -1
                    if (fare <= 0) {
                        errorMessage = "Tarif dasar harus lebih besar dari Rp 0"
                        return@Button
                    }
                    if (fare % 500 != 0) {
                        errorMessage = "Tarif dasar harus kelipatan Rp 500 (contoh: Rp 75.000, Rp 75.500)"
                        return@Button
                    }
                    val duration = durationText.toIntOrNull() ?: 60
                    if (duration !in 15..720) {
                        errorMessage = "Durasi harus antara 15 hingga 720 menit"
                        return@Button
                    }

                    val finalFare = fare

                    val item = ServiceItem(
                        id = existing?.id ?: ("s_" + UUID.randomUUID().toString().take(6)),
                        providerId = existing?.providerId ?: providerId,
                        categoryId = selectedCategory,
                        name = name.trim(),
                        description = description.trim().ifBlank { null },
                        baseFare = finalFare,
                        durationMin = duration,
                        isActive = existing?.isActive ?: true
                    )
                    onConfirm(item)
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Batal")
            }
        }
    )
}
