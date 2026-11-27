package id.bantuin.app.ui.provider.calendar

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme

private val DAY_FULL_NAMES = listOf("", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
private val DAY_SHORT_NAMES = listOf("", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

@OptIn(ExperimentalMaterial3Api::class)
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

    var editingTimeTarget by remember { mutableStateOf<Pair<Int, Boolean>?>(null) } // (dayOfWeek, isOpenTime)

    Scaffold(
        topBar = {
            BantuinTopAppBar(
                title = "Jadwal & Ketersediaan",
                canNavigateBack = canNavigateBack,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            if (uiState.availabilities.isNotEmpty()) {
                Surface(
                    color = colors.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!uiState.errorMessage.isNullOrBlank()) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error
                            )
                        }
                        Button(
                            onClick = { viewModel.saveChanges() },
                            enabled = !uiState.isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.saveSuccess && !uiState.hasUnsavedChanges) colors.success else colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            if (uiState.saveSuccess && !uiState.hasUnsavedChanges) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = when {
                                    uiState.isSaving -> "Menyimpan Jadwal..."
                                    uiState.saveSuccess && !uiState.hasUnsavedChanges -> "Jadwal Tersimpan"
                                    uiState.hasUnsavedChanges -> "Simpan Perubahan Jadwal"
                                    else -> "Simpan Jadwal"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.availabilities.isEmpty() -> {
                LoadingView(
                    message = "Memuat jadwal operasional...",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            uiState.errorMessage != null && uiState.availabilities.isEmpty() -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat jadwal mitra.",
                    onRetry = { viewModel.loadAvailability() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                val selectedDay = uiState.availabilities.find { it.dayOfWeek == uiState.selectedDayOfWeek }
                    ?: uiState.availabilities.firstOrNull()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Jadwal Pesanan Aktif
                    if (uiState.upcomingBookings.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Pesanan Aktif Terjadwal",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        BadgeContainer(
                                            text = "${uiState.upcomingBookings.size} Pesanan",
                                            color = colors.onPrimaryContainer,
                                            backgroundColor = colors.primaryContainer
                                        )
                                    }
                                    uiState.upcomingBookings.take(4).forEach { booking ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.surfaceSubtle)
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${booking.customerName} • ${booking.serviceName}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${booking.scheduledDate} • ${booking.scheduledTime}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.textSecondary
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            BadgeContainer(
                                                text = booking.status.displayName,
                                                color = colors.onPrimaryContainer,
                                                backgroundColor = colors.primaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Template Cepat Mingguan (1-ketuk)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Template Cepat Mingguan",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WeeklyScheduleTemplate.entries.forEach { template ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { viewModel.applyWeeklyTemplate(template) },
                                        label = { Text(template.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = colors.surface,
                                            labelColor = colors.textPrimary
                                        ),
                                        modifier = Modifier.heightIn(min = 42.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Navigasi Strip 7 Hari (Sen - Min)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Pilih Hari untuk Diatur",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                uiState.availabilities.forEach { day ->
                                    val isSelected = day.dayOfWeek == uiState.selectedDayOfWeek
                                    val statusColor = when (day.mode) {
                                        AvailabilityMode.ON -> colors.success
                                        AvailabilityMode.ALWAYS_ON -> colors.primary
                                        AvailabilityMode.OFF -> colors.textMuted
                                    }
                                    val modeShort = when (day.mode) {
                                        AvailabilityMode.ON -> "Jam"
                                        AvailabilityMode.ALWAYS_ON -> "24J"
                                        AvailabilityMode.OFF -> "Off"
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) colors.primaryContainer else colors.surface)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) colors.primary else colors.borderSubtle,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.selectDay(day.dayOfWeek) }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = DAY_SHORT_NAMES.getOrElse(day.dayOfWeek) { "-" },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) colors.onPrimaryContainer else colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
                                        )
                                        Text(
                                            text = modeShort,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = statusColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Panel Editor Terfokus untuk Hari Terpilih
                    if (selectedDay != null) {
                        item {
                            SelectedDayEditorCard(
                                day = selectedDay,
                                onModeChange = { mode -> viewModel.updateDayMode(selectedDay.dayOfWeek, mode) },
                                onSelectPreset = { openMin, closeMin ->
                                    viewModel.updateOperatingHours(selectedDay.dayOfWeek, openMin, closeMin)
                                },
                                onPickOpenTime = { editingTimeTarget = selectedDay.dayOfWeek to true },
                                onPickCloseTime = { editingTimeTarget = selectedDay.dayOfWeek to false },
                                onCopyToWeekdays = { viewModel.copyDayToWeekdays(selectedDay.dayOfWeek) },
                                onCopyToAll = { viewModel.copyDayToAllDays(selectedDay.dayOfWeek) }
                            )
                        }
                    }

                    // 5. Ringkasan Jadwal 7 Hari
                    item {
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
                                    text = "Ringkasan Jadwal Mingguan",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                uiState.availabilities.forEachIndexed { index, day ->
                                    val isSelected = day.dayOfWeek == uiState.selectedDayOfWeek
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) colors.surfaceSubtle else colors.surface)
                                            .clickable { viewModel.selectDay(day.dayOfWeek) }
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = DAY_FULL_NAMES.getOrElse(day.dayOfWeek) { "Hari" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        val (statusText, fg, bg) = when (day.mode) {
                                            AvailabilityMode.ON -> Triple(
                                                "${TimeUtils.formatMinuteOfDay(day.openMin ?: 480, false)} - ${TimeUtils.formatMinuteOfDay(day.closeMin ?: 1020)}",
                                                colors.success,
                                                colors.successContainer
                                            )
                                            AvailabilityMode.ALWAYS_ON -> Triple(
                                                "Siaga 24 Jam",
                                                colors.onPrimaryContainer,
                                                colors.primaryContainer
                                            )
                                            AvailabilityMode.OFF -> Triple(
                                                "Libur",
                                                colors.neutralBadge,
                                                colors.neutralBadgeContainer
                                            )
                                        }
                                        BadgeContainer(
                                            text = statusText,
                                            color = fg,
                                            backgroundColor = bg
                                        )
                                    }
                                    if (index < uiState.availabilities.lastIndex) {
                                        HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Material 3 TimePicker Dialog
    val target = editingTimeTarget
    if (target != null) {
        val (dayOfWeek, isOpenTime) = target
        val day = uiState.availabilities.find { it.dayOfWeek == dayOfWeek }
        val currentMin = if (isOpenTime) (day?.openMin ?: 480) else (day?.closeMin ?: 1020)
        val timePickerState = rememberTimePickerState(
            initialHour = (currentMin / 60).coerceIn(0, 23),
            initialMinute = (currentMin % 60).coerceIn(0, 59),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { editingTimeTarget = null },
            containerColor = colors.surface,
            title = {
                Text(
                    text = if (isOpenTime) {
                        "Pilih Jam Buka (${DAY_FULL_NAMES.getOrElse(dayOfWeek) { "" }})"
                    } else {
                        "Pilih Jam Tutup (${DAY_FULL_NAMES.getOrElse(dayOfWeek) { "" }})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pickedMin = timePickerState.hour * 60 + timePickerState.minute
                        val open = if (isOpenTime) pickedMin else (day?.openMin ?: 480)
                        val close = if (isOpenTime) (day?.closeMin ?: 1020) else pickedMin
                        viewModel.updateOperatingHours(dayOfWeek, open, close)
                        editingTimeTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Text("Pilih Jam")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTimeTarget = null }) {
                    Text("Batal", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SelectedDayEditorCard(
    day: DayAvailability,
    onModeChange: (AvailabilityMode) -> Unit,
    onSelectPreset: (Int, Int) -> Unit,
    onPickOpenTime: () -> Unit,
    onPickCloseTime: () -> Unit,
    onCopyToWeekdays: () -> Unit,
    onCopyToAll: () -> Unit
) {
    val colors = BantuinTheme.colors
    val dayName = DAY_FULL_NAMES.getOrElse(day.dayOfWeek) { "Hari" }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pengaturan Hari $dayName",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                val (badgeText, badgeFg, badgeBg) = when (day.mode) {
                    AvailabilityMode.ON -> Triple("Jam Kerja", colors.success, colors.successContainer)
                    AvailabilityMode.ALWAYS_ON -> Triple("24 Jam", colors.onPrimaryContainer, colors.primaryContainer)
                    AvailabilityMode.OFF -> Triple("Libur", colors.neutralBadge, colors.neutralBadgeContainer)
                }
                BadgeContainer(text = badgeText, color = badgeFg, backgroundColor = badgeBg)
            }

            // Segmented mode pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeOptionTile(
                    icon = Icons.Default.AccessTime,
                    label = "Jam Kerja",
                    selected = day.mode == AvailabilityMode.ON,
                    onClick = { onModeChange(AvailabilityMode.ON) },
                    modifier = Modifier.weight(1f)
                )
                ModeOptionTile(
                    icon = Icons.Default.AllInclusive,
                    label = "24 Jam",
                    selected = day.mode == AvailabilityMode.ALWAYS_ON,
                    onClick = { onModeChange(AvailabilityMode.ALWAYS_ON) },
                    modifier = Modifier.weight(1f)
                )
                ModeOptionTile(
                    icon = Icons.Default.EventBusy,
                    label = "Libur",
                    selected = day.mode == AvailabilityMode.OFF,
                    onClick = { onModeChange(AvailabilityMode.OFF) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (day.mode == AvailabilityMode.ON) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Jam Buka & Tutup",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TimeSelectorCard(
                            label = "Jam Buka",
                            timeText = TimeUtils.formatMinuteOfDay(day.openMin ?: 480, false),
                            onClick = onPickOpenTime,
                            modifier = Modifier.weight(1f)
                        )
                        TimeSelectorCard(
                            label = "Jam Tutup",
                            timeText = TimeUtils.formatMinuteOfDay(day.closeMin ?: 1020, false),
                            onClick = onPickCloseTime,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Pintasan Jam Populer:",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val standardHours = listOf(
                            "08:00 - 17:00" to (480 to 1020),
                            "07:00 - 21:00" to (420 to 1260),
                            "09:00 - 18:00" to (540 to 1080),
                            "08:00 - 14:00" to (480 to 840)
                        )
                        standardHours.forEach { (label, range) ->
                            val isSelected = day.openMin == range.first && day.closeMin == range.second
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectPreset(range.first, range.second) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primaryContainer,
                                    selectedLabelColor = colors.onPrimaryContainer
                                ),
                                modifier = Modifier.heightIn(min = 42.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.borderSubtle)

            // Aksi salin cepat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyToWeekdays,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Salin ke Sen–Jum",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onCopyToAll,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Salin ke Semua",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeOptionTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.primaryContainer else colors.surfaceSubtle)
            .border(
                width = 1.dp,
                color = if (selected) colors.primary else colors.borderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) colors.onPrimaryContainer else colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.onPrimaryContainer else colors.textPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun TimeSelectorCard(
    label: String,
    timeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = label,
            tint = colors.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}
