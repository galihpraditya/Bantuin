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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.DayAvailability
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

@Composable
fun ProviderCalendarScreen(
    repository: BantuinRepository,
    viewModel: ProviderCalendarViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dayNames = listOf("", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    var days by remember { mutableStateOf<List<DayAvailability>>(emptyList()) }
    var isSaved by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.availabilities) {
        if (uiState.availabilities.isNotEmpty()) {
            days = uiState.availabilities
        }
    }
    LaunchedEffect(uiState.saveSuccess) {
        isSaved = uiState.saveSuccess
    }
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            validationError = uiState.errorMessage
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeutralBackground)
    ) {
        BantuinTopAppBar(title = "Jadwal & Ketersediaan")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Atur jam kerja dan ketersediaan operasional Anda per hari:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            items(days, key = { it.dayOfWeek }) { day ->
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
                                text = dayNames.getOrElse(day.dayOfWeek) { "Hari" },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (day.mode) {
                                            AvailabilityMode.ON -> Color(0xFFE8F5E9)
                                            AvailabilityMode.ALWAYS_ON -> Color(0xFFFFF3E0)
                                            AvailabilityMode.OFF -> Color(0xFFEEEEEE)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (day.mode) {
                                        AvailabilityMode.ON -> "Aktif (${TimeUtils.formatMinuteOfDay(day.openMin ?: 480)} - ${TimeUtils.formatMinuteOfDay(day.closeMin ?: 1020)})"
                                        AvailabilityMode.ALWAYS_ON -> "Siaga 24 Jam"
                                        AvailabilityMode.OFF -> "Libur (OFF)"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = when (day.mode) {
                                        AvailabilityMode.ON -> GreenSuccess
                                        AvailabilityMode.ALWAYS_ON -> OrangePrimary
                                        AvailabilityMode.OFF -> TextMuted
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Opsi Mode: OFF / ON / ALWAYS_ON (min 48dp target)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AvailabilityMode.entries.forEach { mode ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .clickable {
                                            val updated = days.map {
                                                if (it.dayOfWeek == day.dayOfWeek) {
                                                    it.copy(
                                                        mode = mode,
                                                        openMin = if (mode == AvailabilityMode.ON) (it.openMin ?: 480) else null,
                                                        closeMin = if (mode == AvailabilityMode.ON) (it.closeMin ?: 1020) else null
                                                    )
                                                } else it
                                            }
                                            days = updated
                                            isSaved = false
                                            validationError = null
                                        }
                                ) {
                                    RadioButton(
                                        selected = day.mode == mode,
                                        onClick = {
                                            val updated = days.map {
                                                if (it.dayOfWeek == day.dayOfWeek) {
                                                    it.copy(
                                                        mode = mode,
                                                        openMin = if (mode == AvailabilityMode.ON) (it.openMin ?: 480) else null,
                                                        closeMin = if (mode == AvailabilityMode.ON) (it.closeMin ?: 1020) else null
                                                    )
                                                } else it
                                            }
                                            days = updated
                                            isSaved = false
                                            validationError = null
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                                    )
                                    Text(
                                        text = when (mode) {
                                            AvailabilityMode.OFF -> "Libur"
                                            AvailabilityMode.ON -> "Jam Kerja"
                                            AvailabilityMode.ALWAYS_ON -> "24 Jam"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        // Sesuai PRD 11.2: Mode ON menampilkan input jam, mode ALWAYS_ON & OFF menyembunyikannya
                        if (day.mode == AvailabilityMode.ON) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Atur Rentang Jam Operasional:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
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
                                            val updated = days.map {
                                                if (it.dayOfWeek == day.dayOfWeek) {
                                                    it.copy(openMin = range.first, closeMin = range.second)
                                                } else it
                                            }
                                            days = updated
                                            isSaved = false
                                            validationError = null
                                        },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = OrangePrimary,
                                            selectedLabelColor = Color.White
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
                                var openInput by remember(day.openMin) {
                                    val h = (day.openMin ?: 480) / 60
                                    val m = (day.openMin ?: 480) % 60
                                    mutableStateOf(String.format(java.util.Locale.US, "%02d:%02d", h, m))
                                }
                                var closeInput by remember(day.closeMin) {
                                    val h = (day.closeMin ?: 1020) / 60
                                    val m = (day.closeMin ?: 1020) % 60
                                    mutableStateOf(String.format(java.util.Locale.US, "%02d:%02d", h, m))
                                }

                                OutlinedTextField(
                                    value = openInput,
                                    onValueChange = { newVal ->
                                        openInput = newVal
                                        isSaved = false
                                        val parts = newVal.split(":", ".")
                                        val h = parts.getOrNull(0)?.trim()?.toIntOrNull()
                                        val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                                        if (h != null && h in 0..23 && m in 0..59) {
                                            val total = h * 60 + m
                                            days = days.map { if (it.dayOfWeek == day.dayOfWeek) it.copy(openMin = total) else it }
                                        }
                                    },
                                    label = { Text("Jam Buka (JJ:MM)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = closeInput,
                                    onValueChange = { newVal ->
                                        closeInput = newVal
                                        isSaved = false
                                        val parts = newVal.split(":", ".")
                                        val h = parts.getOrNull(0)?.trim()?.toIntOrNull()
                                        val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                                        if (h != null && h in 0..23 && m in 0..59) {
                                            val total = h * 60 + m
                                            days = days.map { if (it.dayOfWeek == day.dayOfWeek) it.copy(closeMin = total) else it }
                                        }
                                    },
                                    label = { Text("Jam Tutup (JJ:MM)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (validationError != null) {
                item {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = RedError
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        // Validasi PRD 11.2: mode ON wajib openMin < closeMin
                        val hasInvalidHours = days.any {
                            it.mode == AvailabilityMode.ON && ((it.openMin ?: 0) >= (it.closeMin ?: 0))
                        }
                        if (hasInvalidHours) {
                            validationError = "Jam buka harus lebih awal dari jam tutup untuk mode Jam Kerja"
                            return@Button
                        }

                        viewModel.saveDays(days)
                        validationError = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) GreenSuccess else OrangePrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(if (isSaved) "Jadwal Tersimpan" else "Simpan Perubahan Jadwal")
                }
            }
        }
    }
}
