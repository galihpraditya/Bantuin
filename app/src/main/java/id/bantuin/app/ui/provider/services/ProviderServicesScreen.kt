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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme

@Composable
fun ProviderServicesScreen(
    repository: BantuinRepository,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: ProviderServicesViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        BantuinTopAppBar(
            title = "Katalog Layanan & Tarif",
            canNavigateBack = onNavigateBack != null,
            onNavigateBack = { onNavigateBack?.invoke() }
        )

        when {
            uiState.isLoading && uiState.services.isEmpty() -> {
                LoadingView(message = "Memuat katalog layanan...")
            }
            uiState.errorMessage != null && uiState.services.isEmpty() -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat katalog layanan.",
                    onRetry = { viewModel.loadData() }
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Kelola tarif dasar dan durasi estimasi pengerjaan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.openAddDialog() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                ),
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

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        item {
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error
                            )
                        }
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
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Belum Ada Layanan",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ketuk tombol Tambah untuk membuat penawaran jasa pertama Anda.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.services, key = { it.id }) { service ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                        Text(
                                            text = service.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(
                                                checked = service.isActive,
                                                onCheckedChange = { active ->
                                                    viewModel.toggleServiceActive(service, active)
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = colors.onPrimary,
                                                    checkedTrackColor = colors.success
                                                )
                                            )
                                            IconButton(
                                                onClick = { viewModel.openEditDialog(service) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Ubah Layanan",
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.requestDelete(service) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus Layanan",
                                                    tint = colors.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (!service.description.isNullOrBlank()) {
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
                                        Text(
                                            text = "Estimasi: ${TimeUtils.formatDuration(service.durationMin)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textMuted
                                        )
                                        Text(
                                            text = CurrencyUtils.formatRupiah(service.baseFare),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = colors.primary,
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

    if (uiState.isUpsertDialogOpen) {
        UpsertServiceDialog(
            existing = uiState.editingService,
            categories = uiState.categories,
            isSaving = uiState.isSaving,
            errorMessage = uiState.formErrorMessage,
            onClearError = { viewModel.clearFormError() },
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { name, desc, catId, fareText, durationText ->
                viewModel.saveServiceFromForm(
                    existing = uiState.editingService,
                    name = name,
                    description = desc,
                    categoryId = catId,
                    baseFareText = fareText,
                    durationText = durationText
                )
            }
        )
    }

    val targetDelete = uiState.serviceToDelete
    if (targetDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.requestDelete(null) },
            title = { Text("Hapus Layanan?") },
            text = { Text("Layanan '${targetDelete.name}' akan dihapus dari katalog penawaran Anda.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteService(targetDelete.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.requestDelete(null) },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Batal", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun UpsertServiceDialog(
    existing: ServiceItem?,
    categories: List<ServiceCategory>,
    isSaving: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, categoryId: Int, fareText: String, durationText: String) -> Unit
) {
    val colors = BantuinTheme.colors
    val availableCats = categories.ifEmpty {
        listOf(
            ServiceCategory(1, "Kelistrikan", "bolt", 60, ""),
            ServiceCategory(2, "AC & Elektronik", "ac", 90, ""),
            ServiceCategory(3, "Pipa & Sanitasi", "plumbing", 75, ""),
            ServiceCategory(4, "Kebersihan Kos", "cleaning", 120, ""),
            ServiceCategory(5, "Angkut & Pindahan", "moving", 150, ""),
            ServiceCategory(6, "Les Privat", "tutor", 90, "")
        )
    }
    val initialCatId = existing?.categoryId ?: availableCats.first().id
    val initialDuration = existing?.durationMin
        ?: availableCats.firstOrNull { it.id == initialCatId }?.defaultDurationMin
        ?: 60

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var selectedCategory by remember { mutableIntStateOf(initialCatId) }
    var baseFareText by remember { mutableStateOf(existing?.baseFare?.toString() ?: "75000") }
    var durationText by remember { mutableStateOf(initialDuration.toString()) }
    var durationEditedManually by remember { mutableStateOf(existing != null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Layanan Baru" else "Ubah Layanan") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onClearError()
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
                    color = colors.textPrimary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableCats.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.id,
                            onClick = {
                                selectedCategory = cat.id
                                if (!durationEditedManually && cat.defaultDurationMin in 15..720) {
                                    durationText = cat.defaultDurationMin.toString()
                                }
                            },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = baseFareText,
                    onValueChange = {
                        baseFareText = it.filter { ch -> ch.isDigit() }
                        onClearError()
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
                        durationEditedManually = true
                        onClearError()
                    },
                    label = { Text("Estimasi Durasi (Menit, 15 - 720)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, description, selectedCategory, baseFareText, durationText)
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (isSaving) "Menyimpan..." else "Simpan")
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
