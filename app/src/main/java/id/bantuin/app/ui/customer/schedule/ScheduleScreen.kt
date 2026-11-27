package id.bantuin.app.ui.customer.schedule

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.DistanceUtils
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.ConflictReason
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.model.SlotCheckResult
import id.bantuin.app.domain.model.TimeBlock
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.domain.usecase.matchmaking.HaversineCalculator
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.customer.OrderFlowManager
import id.bantuin.app.ui.theme.BantuinTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    orderFlow: OrderFlowManager,
    repository: BantuinRepository,
    onNavigateBack: () -> Unit,
    onContinueToEstimate: () -> Unit,
    viewModel: ScheduleViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    val provider = orderFlow.selectedProvider
    val service = orderFlow.selectedService

    if (provider == null || service == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pilih Jadwal Servis", color = colors.textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
                )
            },
            containerColor = colors.background
        ) { innerPadding ->
            ErrorView(
                message = "Data mitra atau layanan belum terpilih. Silakan pilih layanan terlebih dahulu.",
                onRetry = onNavigateBack,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    val distanceKm = remember(orderFlow.customerLocation, provider.location) {
        val d = HaversineCalculator.distanceKm(orderFlow.customerLocation, provider.location)
        Math.round(d * 10.0) / 10.0
    }

    LaunchedEffect(provider.providerId, service.id) {
        viewModel.initialize(provider, service, distanceKm, orderFlow.selectedStartMin)
    }

    val selectedStartMin = uiState.selectedStartMin
    val slotCheckResult = uiState.slotCheckResult
    val isSlotValid = slotCheckResult?.valid ?: true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pilih Jadwal Servis",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        bottomBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Waktu Terpilih:",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                            val endMin = selectedStartMin + service.durationMin
                            val timeStr = "${TimeUtils.formatMinuteOfDay(selectedStartMin, false)} - ${TimeUtils.formatMinuteOfDay(endMin)}"
                            Text(
                                text = if (isSlotValid) timeStr else "$timeStr (Tidak Tersedia)",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSlotValid) colors.textPrimary else colors.error,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Durasi Layanan:",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                            Text(
                                text = "${service.durationMin} Mnt",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.validateAndProceed {
                                orderFlow.selectedStartMin = selectedStartMin
                                orderFlow.selectedDurationMin = service.durationMin
                                orderFlow.selectedDateIso = uiState.selectedDate.toString()
                                onContinueToEstimate()
                            }
                        },
                        enabled = isSlotValid && !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                            disabledContainerColor = colors.surfaceSubtle,
                            disabledContentColor = colors.textMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = if (isSlotValid) "Lanjut ke Estimasi Biaya" else "Pilih Jam Bebas Konflik",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading && uiState.dynamicDates.isEmpty()) {
            LoadingView(
                message = "Memuat jadwal ketersediaan mitra...",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        val currentDayAvailability = uiState.providerAvailability
            .find { it.dayOfWeek == uiState.selectedDate.dayOfWeek.value }
            ?: DayAvailability(
                dayOfWeek = uiState.selectedDate.dayOfWeek.value,
                mode = AvailabilityMode.ON,
                openMin = 480,
                closeMin = 1020
            )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Ringkasan Mitra Terpilih
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = provider.name.take(1),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = provider.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${DistanceUtils.formatKm(distanceKm)} dari lokasimu • ${service.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onNavigateBack,
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(colors.outline)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Ganti", style = MaterialTheme.typography.labelSmall, color = colors.textPrimary)
                        }
                    }
                }
            }

            // 2. Pemilih Tanggal Horizontal (7 Hari ke Depan)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pilih Tanggal Servis",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = uiState.monthYearLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.dynamicDates.forEachIndexed { index, (dayName, dateNum, date) ->
                            val isSelected = uiState.selectedDateIdx == index
                            val dayAvail = uiState.providerAvailability.find { it.dayOfWeek == date.dayOfWeek.value }
                            val isOff = dayAvail?.mode == AvailabilityMode.OFF
                            val note = if (index == 0) "Hari ini" else if (isOff) "Libur" else "Tersedia"

                            Card(
                                modifier = Modifier
                                    .width(72.dp)
                                    .heightIn(min = 72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.selectDate(index) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) colors.primary else colors.surface
                                ),
                                border = if (isSelected) {
                                    null
                                } else {
                                    CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                                    )
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) colors.onPrimary else colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dateNum,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (isSelected) colors.onPrimary else colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = when {
                                            isSelected -> colors.onPrimary
                                            isOff -> colors.error
                                            else -> colors.success
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Info Buffer SLA F3
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceSubtle),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Estimasi Durasi Servis",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = TimeUtils.formatDuration(service.durationMin),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                            }
                        }

                    }
                }
            }

            // 4. Grid Slot Waktu Mulai
            item {
                val operText = when (currentDayAvailability.mode) {
                    AvailabilityMode.OFF -> "Mitra Libur"
                    AvailabilityMode.ALWAYS_ON -> "Operasional 24 Jam"
                    AvailabilityMode.ON -> "Operasional ${TimeUtils.formatMinuteOfDay(currentDayAvailability.openMin ?: 480)} - ${TimeUtils.formatMinuteOfDay(currentDayAvailability.closeMin ?: 1020)}"
                }
                Text(
                    text = "Pilih Jam Mulai ($operText)",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
            }

            item {
                val slots = uiState.availableSlots
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        LoadingView(message = "Memeriksa ketersediaan slot...")
                    }
                } else if (slots.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                                text = "Tidak Ada Slot Tersedia",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mitra tidak membuka jadwal pada tanggal ini. Silakan pilih tanggal lain.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                } else {
                    val morningSlots = slots.filter { it.first < 720 }
                    val afternoonSlots = slots.filter { it.first in 720..990 }
                    val eveningSlots = slots.filter { it.first > 990 }
                    val groups = listOf(
                        "Pagi" to morningSlots,
                        "Siang" to afternoonSlots,
                        "Sore & Malam" to eveningSlots
                    ).filter { it.second.isNotEmpty() }

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        groups.forEach { (groupTitle, groupSlots) ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "$groupTitle (${groupSlots.size} slot)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val chunkedSlots = groupSlots.chunked(2)
                                chunkedSlots.forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        pair.forEach { (min, label) ->
                                            val isSelected = selectedStartMin == min
                                            val isValid = uiState.slotValidMap[min] ?: true

                                            TimeSlotItem(
                                                timeLabel = label,
                                                finishLabel = "Selesai ${TimeUtils.formatMinuteOfDay(min + service.durationMin)}",
                                                isSelected = isSelected,
                                                isConflict = !isValid,
                                                onClick = { viewModel.selectSlot(min) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Kartu Peringatan Konflik Jadwal (F3 Conflict Detector)
            if (slotCheckResult != null && !isSlotValid) {
                item {
                    ConflictAlertCard(
                        slotCheckResult = slotCheckResult,
                        selectedStartMin = uiState.selectedStartMin,
                        durationMin = uiState.service?.durationMin ?: 60,
                        existingBlocks = uiState.existingBlocks,
                        onSelectAlternative = { altMin -> viewModel.selectSlot(altMin) }
                    )
                }
            }

        }
    }
}

@Composable
private fun TimeSlotItem(
    timeLabel: String,
    finishLabel: String,
    isSelected: Boolean,
    isConflict: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    val borderColor = when {
        isSelected -> colors.primary
        isConflict -> colors.error
        else -> colors.borderSubtle
    }

    val backgroundColor = when {
        isSelected -> colors.primaryContainer.copy(alpha = 0.45f)
        else -> colors.surface
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isConflict) colors.errorContainer else colors.successContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isConflict) "Bentrok" else "Tersedia",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isConflict) colors.error else colors.success,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = finishLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (isConflict) colors.error else colors.textMuted
            )
        }
    }
}

@Composable
private fun ConflictAlertCard(
    slotCheckResult: SlotCheckResult,
    selectedStartMin: Int = 0,
    durationMin: Int = 60,
    existingBlocks: List<TimeBlock>,
    onSelectAlternative: (Int) -> Unit
) {
    val colors = BantuinTheme.colors
    val reasonDetail = when (slotCheckResult.reason) {
        ConflictReason.OVERLAP -> {
            val requestedEnd = selectedStartMin + durationMin + id.bantuin.app.core.config.BantuinConfig.TRAVEL_BUFFER_MIN
            val block = existingBlocks.firstOrNull { it.startMin < requestedEnd && selectedStartMin < it.endMin }
                ?: existingBlocks.firstOrNull()
            if (block != null) {
                "Slot beririsan dengan jadwal sibuk (${TimeUtils.formatMinuteOfDay(block.startMin, false)} - ${TimeUtils.formatMinuteOfDay(block.endMin)} termasuk buffer perjalanan)."
            } else {
                slotCheckResult.reason.message
            }
        }
        else -> slotCheckResult.reason?.message ?: "Slot ini tidak tersedia untuk dipilih."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.error.copy(alpha = 0.4f))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.error),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Slot Tidak Tersedia",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.error,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reasonDetail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error
            )

            if (slotCheckResult.alternativeStartsMin.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rekomendasi Jam Bebas Konflik:",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                slotCheckResult.alternativeStartsMin.forEachIndexed { idx, altMin ->
                    val timeLabel = TimeUtils.formatMinuteOfDay(altMin)
                    OutlinedButton(
                        onClick = { onSelectAlternative(altMin) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surface,
                            contentColor = colors.textPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(colors.success)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.success,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pilih $timeLabel",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary
                                )
                            }

                            if (idx == 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.successContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Paling Cepat",
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
