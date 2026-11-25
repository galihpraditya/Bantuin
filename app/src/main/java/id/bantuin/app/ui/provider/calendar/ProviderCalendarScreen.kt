package id.bantuin.app.ui.provider.calendar

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme
import java.util.Locale

@Composable
fun ProviderCalendarScreen(
    repository: BantuinRepository,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    viewModel: ProviderCalendarViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors
    val dayNames = listOf("", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        BantuinTopAppBar(
            title = "Jadwal & Ketersediaan",
            canNavigateBack = canNavigateBack,
            onNavigateBack = onNavigateBack
        )

        when {
            uiState.isLoading && uiState.availabilities.isEmpty() -> {
                LoadingView(message = "Memuat jadwal operasional...")
            }
            uiState.errorMessage != null && uiState.availabilities.isEmpty() -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat jadwal mitra.",
                    onRetry = { viewModel.loadAvailability() }
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (uiState.upcomingBookings.isNotEmpty()) {
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
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Jadwal Pesanan Aktif (${uiState.upcomingBookings.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    uiState.upcomingBookings.take(5).forEach { booking ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colors.surfaceSubtle)
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${booking.customerName} • ${booking.serviceName}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = "${booking.scheduledDate} • ${booking.scheduledTime}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.textSecondary
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(colors.primaryContainer)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = booking.status.displayName,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = colors.onPrimaryContainer,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Atur jam kerja dan ketersediaan operasional Anda per hari:",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }

                    items(uiState.availabilities, key = { it.dayOfWeek }) { day ->
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
                                        text = dayNames.getOrElse(day.dayOfWeek) { "Hari" },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary
                                    )

                                    val badgeBg = when (day.mode) {
                                        AvailabilityMode.ON -> colors.successContainer
                                        AvailabilityMode.ALWAYS_ON -> colors.primaryContainer
                                        AvailabilityMode.OFF -> colors.neutralBadgeContainer
                                    }
                                    val badgeFg = when (day.mode) {
                                        AvailabilityMode.ON -> colors.success
                                        AvailabilityMode.ALWAYS_ON -> colors.onPrimaryContainer
                                        AvailabilityMode.OFF -> colors.neutralBadge
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when (day.mode) {
                                                AvailabilityMode.ON -> "Aktif (${TimeUtils.formatMinuteOfDay(day.openMin ?: 480, false)} - ${TimeUtils.formatMinuteOfDay(day.closeMin ?: 1020)})"
                                                AvailabilityMode.ALWAYS_ON -> "Siaga 24 Jam"
                                                AvailabilityMode.OFF -> "Libur (OFF)"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = badgeFg,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    AvailabilityMode.entries.forEach { mode ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .clickable { viewModel.updateDayMode(day.dayOfWeek, mode) }
                                        ) {
                                            RadioButton(
                                                selected = day.mode == mode,
                                                onClick = { viewModel.updateDayMode(day.dayOfWeek, mode) },
                                                colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                                            )
                                            Text(
                                                text = when (mode) {
                                                    AvailabilityMode.OFF -> "Libur"
                                                    AvailabilityMode.ON -> "Jam Kerja"
                                                    AvailabilityMode.ALWAYS_ON -> "24 Jam"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textPrimary
                                            )
                                        }
                                    }
                                }

                                if (day.mode == AvailabilityMode.ON) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Pilih Rentang Jam Operasional:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val standardHours = listOf(
                                            "08.00 - 17.00" to (480 to 1020),
                                            "07.00 - 21.00" to (420 to 1260),
                                            "09.00 - 18.00" to (540 to 1080),
                                            "08.00 - 14.00" to (480 to 840)
                                        )
                                        standardHours.forEach { (label, range) ->
                                            val isSelected = (day.openMin == range.first && day.closeMin == range.second)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    viewModel.updateOperatingHours(
                                                        dayOfWeek = day.dayOfWeek,
                                                        openMin = range.first,
                                                        closeMin = range.second
                                                    )
                                                },
                                                label = { Text(label) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = colors.primaryContainer,
                                                    selectedLabelColor = colors.onPrimaryContainer
                                                ),
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        var openInput by remember(day.dayOfWeek, day.openMin) {
                                            val h = (day.openMin ?: 480) / 60
                                            val m = (day.openMin ?: 480) % 60
                                            mutableStateOf(String.format(Locale.US, "%02d:%02d", h, m))
                                        }
                                        var openFormatInvalid by remember(day.dayOfWeek, day.openMin) {
                                            mutableStateOf(false)
                                        }
                                        var closeInput by remember(day.dayOfWeek, day.closeMin) {
                                            val h = (day.closeMin ?: 1020) / 60
                                            val m = (day.closeMin ?: 1020) % 60
                                            mutableStateOf(String.format(Locale.US, "%02d:%02d", h, m))
                                        }
                                        var closeFormatInvalid by remember(day.dayOfWeek, day.closeMin) {
                                            mutableStateOf(false)
                                        }

                                        OutlinedTextField(
                                            value = openInput,
                                            onValueChange = { newVal ->
                                                openInput = newVal
                                                val total = parseTimeToMin(newVal)
                                                openFormatInvalid = newVal.isNotBlank() && total == null
                                                if (total != null) {
                                                    viewModel.updateHourFieldUnchecked(day.dayOfWeek, openMin = total, closeMin = null)
                                                }
                                            },
                                            label = { Text("Jam Buka (JJ:MM)") },
                                            isError = openFormatInvalid,
                                            supportingText = if (openFormatInvalid) {
                                                { Text("Format JJ:MM, mis. 08:00") }
                                            } else null,
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = closeInput,
                                            onValueChange = { newVal ->
                                                closeInput = newVal
                                                val total = parseTimeToMin(newVal)
                                                closeFormatInvalid = newVal.isNotBlank() && total == null
                                                if (total != null) {
                                                    viewModel.updateHourFieldUnchecked(day.dayOfWeek, openMin = null, closeMin = total)
                                                }
                                            },
                                            label = { Text("Jam Tutup (JJ:MM)") },
                                            isError = closeFormatInvalid,
                                            supportingText = if (closeFormatInvalid) {
                                                { Text("Format JJ:MM, mis. 17:00") }
                                            } else null,
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
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

                    item {
                        Button(
                            onClick = { viewModel.saveChanges() },
                            enabled = !uiState.isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.saveSuccess) colors.success else colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                when {
                                    uiState.isSaving -> "Menyimpan..."
                                    uiState.saveSuccess -> "Jadwal Tersimpan"
                                    else -> "Simpan Perubahan Jadwal"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseTimeToMin(raw: String): Int? {
    val parts = raw.trim().split(":", ".")
    if (parts.size != 2) return null
    val h = parts[0].trim().toIntOrNull() ?: return null
    val m = parts[1].trim().toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}
