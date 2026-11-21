package id.bantuin.app.ui.customer.schedule

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.usecase.matchmaking.HaversineCalculator
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.ConflictReason
import kotlinx.coroutines.launch
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.model.TimeBlock
import id.bantuin.app.domain.usecase.scheduling.SchedulingConflictDetector
import id.bantuin.app.ui.common.RatingDisplay
import id.bantuin.app.ui.customer.OrderFlowManager
import id.bantuin.app.ui.theme.BorderSubtle
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.GreenSuccessContainer
import id.bantuin.app.ui.theme.NeutralBackground
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangeContainer
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.RedErrorContainer
import id.bantuin.app.ui.theme.TextMuted
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.theme.TextPrimary
import id.bantuin.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    orderFlow: OrderFlowManager,
    repository: id.bantuin.app.data.repository.FakeBantuinRepository,
    onNavigateBack: () -> Unit,
    onContinueToEstimate: () -> Unit,
    viewModel: ScheduleViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val detector = remember { SchedulingConflictDetector() }
    val provider = orderFlow.selectedProvider ?: FakeData.providers.first()
    val service = orderFlow.selectedService ?: FakeData.services.first()

    val distanceKm = remember(orderFlow.customerLocation, provider.location) {
        val d = HaversineCalculator.distanceKm(orderFlow.customerLocation, provider.location)
        Math.round(d * 10.0) / 10.0
    }

    androidx.compose.runtime.LaunchedEffect(provider.providerId, service.id) {
        viewModel.initialize(provider, service, distanceKm, orderFlow.selectedStartMin)
    }

    // 7 hari kalender dinamis mulai dari hari ini (WIB)
    val today = remember { java.time.LocalDate.now() }
    val dynamicDates = remember(today) {
        (0..6).map { i ->
            val d = today.plusDays(i.toLong())
            val dayName = when (d.dayOfWeek.value) {
                1 -> "Sen"
                2 -> "Sel"
                3 -> "Rab"
                4 -> "Kam"
                5 -> "Jum"
                6 -> "Sab"
                else -> "Min"
            }
            Triple(dayName, "${d.dayOfMonth}", d)
        }
    }
    var selectedDateIdx by remember { mutableIntStateOf(0) }
    val selectedDate = dynamicDates[selectedDateIdx].third
    val selectedDateIso = remember(selectedDate) { selectedDate.toString() }
    val dayOfWeekIso = remember(selectedDate) { selectedDate.dayOfWeek.value }

    // Memuat ketersediaan & blok sibuk dari repositori secara dinamis
    var providerAvailability by remember { mutableStateOf<List<DayAvailability>>(emptyList()) }
    var existingBlocks by remember { mutableStateOf<List<TimeBlock>>(emptyList()) }

    androidx.compose.runtime.LaunchedEffect(provider.providerId, selectedDateIso) {
        val availRes = repository.availability(provider.providerId)
        if (availRes is id.bantuin.app.core.result.AppResult.Success) {
            providerAvailability = availRes.data
        }
        val busyRes = repository.busyBlocks(provider.providerId, selectedDateIso)
        if (busyRes is id.bantuin.app.core.result.AppResult.Success) {
            existingBlocks = busyRes.data
        }
    }

    val currentDayAvailability = remember(providerAvailability, dayOfWeekIso) {
        providerAvailability.find { it.dayOfWeek == dayOfWeekIso } ?: DayAvailability(
            dayOfWeek = dayOfWeekIso,
            mode = AvailabilityMode.ON,
            openMin = 480,
            closeMin = 1020
        )
    }

    // Pembangkitan slot jam berdasarkan jam kerja mitra
    val availableSlots = remember(currentDayAvailability, service.durationMin) {
        when (currentDayAvailability.mode) {
            AvailabilityMode.OFF -> emptyList()
            AvailabilityMode.ALWAYS_ON -> {
                (480..1260 step 60).map { min -> min to TimeUtils.formatMinuteOfDay(min) }
            }
            AvailabilityMode.ON -> {
                val start = currentDayAvailability.openMin ?: 480
                val end = (currentDayAvailability.closeMin ?: 1020) - service.durationMin
                if (start <= end) {
                    (start..end step 30).map { min -> min to TimeUtils.formatMinuteOfDay(min) }
                } else {
                    listOf(480 to "08:00", 600 to "10:00", 780 to "13:00", 870 to "14:30")
                }
            }
        }
    }

    var selectedStartMin by remember { mutableIntStateOf(orderFlow.selectedStartMin) }

    androidx.compose.runtime.LaunchedEffect(availableSlots) {
        if (availableSlots.isNotEmpty() && availableSlots.none { it.first == selectedStartMin }) {
            selectedStartMin = availableSlots.first().first
        }
    }

    // Evaluasi F3 Conflict Detector secara dinamis
    val isSelectedDateToday = selectedDateIdx == 0
    val nowMin = remember(isSelectedDateToday) {
        if (isSelectedDateToday) {
            val now = java.time.LocalTime.now()
            now.hour * 60 + now.minute
        } else null
    }

    val slotCheckResult = remember(selectedStartMin, service.durationMin, currentDayAvailability, existingBlocks, nowMin) {
        detector.check(
            requestedStartMin = selectedStartMin,
            durationMin = service.durationMin,
            day = currentDayAvailability,
            existing = existingBlocks,
            nowMin = nowMin
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pilih Jadwal Servis",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeutralBackground)
            )
        },
        bottomBar = {
            // Bilah Bawah Konfirmasi Jadwal
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralSurface),
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
                                color = TextMuted
                            )
                            val endMin = selectedStartMin + service.durationMin
                            val timeStr = "${TimeUtils.formatMinuteOfDay(selectedStartMin, false)} - ${TimeUtils.formatMinuteOfDay(endMin)}"
                            Text(
                                text = if (slotCheckResult.valid) timeStr else "$timeStr (Konflik)",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (slotCheckResult.valid) TextPrimary else RedError,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Durasi Layanan:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Text(
                                text = "${service.durationMin} Mnt",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.acquireLockAndProceed { lockInfo ->
                                orderFlow.currentLock = lockInfo
                                orderFlow.selectedStartMin = selectedStartMin
                                orderFlow.selectedDurationMin = service.durationMin
                                orderFlow.selectedDateIso = selectedDateIso
                                onContinueToEstimate()
                            }
                        },
                        enabled = slotCheckResult.valid && !uiState.isLocking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary,
                            disabledContainerColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (slotCheckResult.valid) "Lanjut ke Estimasi Biaya" else "Pilih Jam Bebas Konflik",
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
        containerColor = NeutralBackground,
        modifier = modifier
    ) { innerPadding ->
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
                    colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(OrangeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = provider.name.take(1),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = OrangePrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = provider.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", distanceKm)} km dari lokasimu • Rating ${provider.ratingAvg ?: 4.9}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateBack,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Ganti", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 2. Pemilih Tanggal Horizontal
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
                            color = TextPrimary
                        )
                        Text(
                            text = "Oktober 2024",
                            style = MaterialTheme.typography.labelMedium,
                            color = OrangePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dynamicDates.forEachIndexed { index, (dayName, dateNum, d) ->
                            val isSelected = selectedDateIdx == index
                            val dayAvail = providerAvailability.find { it.dayOfWeek == d.dayOfWeek.value }
                            val isOff = dayAvail?.mode == AvailabilityMode.OFF
                            val note = if (index == 0) "Hari ini" else if (isOff) "Libur" else "Ada slot"
                            Card(
                                modifier = Modifier
                                    .width(68.dp)
                                    .heightIn(min = 68.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedDateIdx = index },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) OrangePrimary else NeutralSurface
                                ),
                                border = if (isSelected) null else CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
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
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dateNum,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isSelected) Color.White else if (isOff) RedError else GreenSuccess
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0EAE1))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Estimasi Durasi Servis",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = TimeUtils.formatDuration(service.durationMin),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(OrangeContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ 30m Buffer SLA",
                                style = MaterialTheme.typography.labelSmall,
                                color = OrangePrimary,
                                fontWeight = FontWeight.Bold
                            )
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
                    color = TextPrimary
                )
            }

            item {
                if (availableSlots.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralSurface)
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
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mitra tidak membuka jadwal pada hari ini. Silakan pilih tanggal lain.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    val chunkedSlots = availableSlots.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        chunkedSlots.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { (min, label) ->
                                    val isSelected = selectedStartMin == min
                                    val check = detector.check(
                                        requestedStartMin = min,
                                        durationMin = service.durationMin,
                                        day = currentDayAvailability,
                                        existing = existingBlocks,
                                        nowMin = nowMin
                                    )
                                    val isConflict = !check.valid

                                    TimeSlotItem(
                                        timeLabel = label,
                                        finishLabel = "Selesai ${TimeUtils.formatMinuteOfDay(min + service.durationMin)}",
                                        isSelected = isSelected,
                                        isFilled = false,
                                        isConflict = isConflict,
                                        onClick = {
                                            selectedStartMin = min
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Kartu Peringatan Konflik Jadwal (F3 Conflict Detector)
            if (!slotCheckResult.valid) {
                item {
                    ConflictAlertCard(
                        slotCheckResult = slotCheckResult,
                        onSelectAlternative = { altMin ->
                            selectedStartMin = altMin
                        }
                    )
                }
            }

            // 6. Notifikasi Kunci Slot 10 Menit
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF7B1FA2),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Slot jadwal dikunci otomatis selama 10 menit setelah waktu valid disetujui.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4A148C)
                        )
                    }
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
    isFilled: Boolean,
    isConflict: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isSelected -> OrangePrimary
        isConflict -> RedError
        else -> BorderSubtle
    }

    val backgroundColor = when {
        isFilled -> Color(0xFFEEEEEE)
        isSelected -> OrangeContainer.copy(alpha = 0.3f)
        else -> NeutralSurface
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isFilled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor))
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
                    color = if (isFilled) TextMuted else TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isFilled -> Color(0xFFE0E0E0)
                                isConflict -> RedErrorContainer
                                else -> GreenSuccessContainer
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            isFilled -> "Terisi"
                            isConflict -> "Konflik"
                            else -> "Tersedia"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = when {
                            isFilled -> TextMuted
                            isConflict -> RedError
                            else -> GreenSuccess
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isFilled) "Pesanan Lain" else finishLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (isConflict) RedError else TextMuted
            )
        }
    }
}

@Composable
private fun ConflictAlertCard(
    slotCheckResult: id.bantuin.app.domain.model.SlotCheckResult,
    onSelectAlternative: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RedErrorContainer),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RedError.copy(alpha = 0.3f)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(RedError),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Konflik Jadwal Terdeteksi!",
                    style = MaterialTheme.typography.titleSmall,
                    color = RedError,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Slot jam ini beririsan dengan pesanan aktif mitra (estimasi selesai 13:45 + 30 mnt buffer perjalanan). Mohon pilih waktu alternatif bebas tumpang tindih.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7F0000)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bar visual overlapping
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFCDD2))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(8.dp)
                        .background(RedError)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Rekomendasi Jam Bebas Konflik:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rekomendasi slot alternatif (maks 3 slot)
            slotCheckResult.alternativeStartsMin.forEachIndexed { idx, altMin ->
                val timeLabel = TimeUtils.formatMinuteOfDay(altMin)
                Button(
                    onClick = { onSelectAlternative(altMin) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(GreenSuccess)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
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
                                tint = GreenSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pilih $timeLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }

                        if (idx == 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GreenSuccessContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Paling Cepat",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = GreenSuccess,
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
