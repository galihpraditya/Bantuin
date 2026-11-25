package id.bantuin.app.ui.customer.estimate

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.domain.usecase.matchmaking.HaversineCalculator
import id.bantuin.app.domain.usecase.pricing.CostEstimatorUseCase
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.CountdownTimerView
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.customer.OrderFlowManager
import id.bantuin.app.ui.theme.BantuinTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimateScreen(
    orderFlow: OrderFlowManager,
    repository: BantuinRepository,
    onNavigateBack: () -> Unit,
    onOrderConfirmed: (String, PaymentMethod) -> Unit,
    viewModel: EstimateViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    val provider = orderFlow.selectedProvider
    val service = orderFlow.selectedService

    var isNavigatingBack by remember { mutableStateOf(false) }

    fun handleBack() {
        if (isNavigatingBack) return
        isNavigatingBack = true
        viewModel.releaseLock {
            orderFlow.currentLock = null
            onNavigateBack()
        }
    }

    BackHandler(onBack = ::handleBack)

    if (provider == null || service == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Estimasi Tarif & Order", color = colors.textPrimary) },
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
                message = "Data layanan atau mitra tidak lengkap. Silakan kembali memilih layanan.",
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

    LaunchedEffect(
        provider.providerId,
        service.id,
        orderFlow.selectedDateIso,
        orderFlow.selectedStartMin,
        orderFlow.currentLock?.lockId
    ) {
        viewModel.initialize(
            categoryId = orderFlow.selectedCategoryId,
            provider = provider,
            service = service,
            customerLocation = orderFlow.customerLocation,
            customerAddress = orderFlow.customerAddress,
            scheduledDateIso = orderFlow.selectedDateIso,
            scheduledStartMin = orderFlow.selectedStartMin,
            scheduledDurationMin = orderFlow.selectedDurationMin,
            lockInfo = orderFlow.currentLock,
            distanceKm = distanceKm,
            initialComplexity = orderFlow.selectedComplexity,
            initialUrgent = orderFlow.isUrgent,
            initialNotes = orderFlow.orderNotes,
            initialPaymentMethod = orderFlow.selectedPaymentMethod
        )
    }

    val estimate = uiState.costEstimate ?: CostEstimatorUseCase()(
        baseFare = service.baseFare,
        distanceKm = distanceKm,
        complexity = uiState.complexity,
        startMinOfDay = uiState.scheduledStartMin,
        isUrgent = uiState.isUrgent
    )

    LaunchedEffect(estimate) {
        orderFlow.currentEstimate = estimate
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Estimasi Tarif & Order",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Pembayaran",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                        Text(
                            text = CurrencyUtils.formatRupiah(estimate.total),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (uiState.isLockExpired) {
                                handleBack()
                            } else {
                                val chosenMethod = uiState.paymentMethod
                                viewModel.confirmOrder { bookingId ->
                                    orderFlow.reset()
                                    onOrderConfirmed(bookingId, chosenMethod)
                                }
                            }
                        },
                        enabled = !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isLockExpired) colors.error else colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = when {
                                uiState.isLockExpired -> "Pilih Jadwal Ulang"
                                uiState.isSubmitting -> "Memproses..."
                                else -> "Konfirmasi Order"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Hitung Mundur Kunci Slot 10 Menit
            val activeLock = uiState.lockInfo
            if (activeLock != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isLockExpired) colors.errorContainer else colors.infoContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (uiState.isLockExpired) colors.error else colors.info,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isLockExpired) {
                                        "Kunci slot 10 menit telah habis"
                                    } else {
                                        "Slot jadwal dikunci sementara"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isLockExpired) colors.error else colors.info,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            CountdownTimerView(
                                targetEpochMillis = activeLock.expiresAtMillis,
                                onExpired = { viewModel.onLockExpired() }
                            )
                        }
                    }
                }
            }

            // Pesan galat RPC / validasi
            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // 1. Ringkasan Pesanan & Jadwal
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    text = service.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = colors.borderSubtle)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${uiState.scheduledDateIso} • ${TimeUtils.formatMinuteOfDay(uiState.scheduledStartMin)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            // 2. Pemilih Tingkat Kompleksitas (F2)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pilih Tingkat Kompleksitas",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${uiState.complexity.weight}x Tarif",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Complexity.entries.forEach { comp ->
                        val isSelected = uiState.complexity == comp
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    orderFlow.selectedComplexity = comp
                                    viewModel.setComplexity(comp)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) colors.primaryContainer.copy(alpha = 0.35f) else colors.surface
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isSelected) colors.primary else colors.borderSubtle
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        orderFlow.selectedComplexity = comp
                                        viewModel.setComplexity(comp)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = comp.displayName + if (comp == Complexity.SEDANG) " (Rekomendasi)" else "",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "${comp.weight}x",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = comp.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Sakelar Layanan Cepat Tanggap / Darurat
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = colors.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Layanan Cepat Tanggap",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Penanganan prioritas ke lokasi Anda",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Switch(
                            checked = uiState.isUrgent,
                            onCheckedChange = {
                                orderFlow.isUrgent = it
                                viewModel.setUrgent(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onPrimary,
                                checkedTrackColor = colors.primary
                            )
                        )
                    }
                }
            }

            // 4. Kartu Informasi Surge (+20%) bila jam sibuk, malam, atau darurat
            if (estimate.surgeApplied) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.warningContainer),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = colors.warning,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = estimate.surgeReason.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.warning,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Penambahan tunggal +20% berlaku sesuai jam sibuk, malam, atau prioritas darurat.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 5. Rincian Biaya Transparan (F2)
            item {
                val preComplexity = estimate.baseFare + estimate.distanceCost
                val complexityAdjustment = estimate.baseCost - preComplexity
                val complexityDiffLabel = if (complexityAdjustment >= 0) {
                    "+ ${CurrencyUtils.formatRupiah(complexityAdjustment)}"
                } else {
                    "- ${CurrencyUtils.formatRupiah(-complexityAdjustment)}"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Rincian Biaya Transparan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.successContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Garansi 7 Hari",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = colors.success,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        CostRow(label = "Tarif Dasar Jasa", amount = CurrencyUtils.formatRupiah(estimate.baseFare))
                        CostRow(
                            label = "Biaya Jarak (${estimate.distanceKm} km x Rp2.000)",
                            amount = CurrencyUtils.formatRupiah(estimate.distanceCost)
                        )
                        CostRow(
                            label = "Kompleksitas (${uiState.complexity.displayName} ${uiState.complexity.weight}x)",
                            amount = complexityDiffLabel
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = colors.borderSubtle)
                        Spacer(modifier = Modifier.height(6.dp))

                        CostRow(
                            label = "Subtotal Layanan",
                            amount = CurrencyUtils.formatRupiah(estimate.baseCost)
                        )

                        if (estimate.surgeApplied) {
                            CostRow(
                                label = "Surge 1.20x (${estimate.surgeReason.label})",
                                amount = "+ ${CurrencyUtils.formatRupiah(estimate.surgeAmount)}",
                                isHighlight = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = colors.borderSubtle)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Pembayaran",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Termasuk garansi servis 7 hari",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                            Text(
                                text = CurrencyUtils.formatRupiah(estimate.total),
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 6. Catatan untuk Teknisi
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Catatan untuk Teknisi (Opsional)",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.notes,
                            onValueChange = {
                                orderFlow.orderNotes = it.take(500)
                                viewModel.setNotes(it)
                            },
                            placeholder = { Text("Contoh: Lokasi di lantai 2, patokan gerbang hijau...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                }
            }

            // 7. Pilih Metode Pembayaran (QRIS atau Tunai)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih Metode Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )

                    val paymentOptions = listOf(
                        Triple(
                            PaymentMethod.QRIS,
                            "QRIS",
                            "Non-tunai instan, dana ditahan aman sistem hingga pekerjaan selesai"
                        ),
                        Triple(
                            PaymentMethod.CASH,
                            "Tunai di Tempat (Cash)",
                            "Bayar langsung ke teknisi setelah pekerjaan diperiksa"
                        )
                    )

                    paymentOptions.forEach { (method, title, subtitle) ->
                        val isSelected = uiState.paymentMethod == method
                        val icon = if (method == PaymentMethod.QRIS) Icons.Default.QrCode2 else Icons.Default.Payments
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    orderFlow.selectedPaymentMethod = method
                                    viewModel.setPaymentMethod(method)
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) colors.primaryContainer.copy(alpha = 0.35f) else colors.surface
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isSelected) colors.primary else colors.borderSubtle
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        orderFlow.selectedPaymentMethod = method
                                        viewModel.setPaymentMethod(method)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) colors.primaryContainer else colors.surfaceSubtle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.onPrimaryContainer else colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
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

@Composable
private fun CostRow(
    label: String,
    amount: String,
    isHighlight: Boolean = false
) {
    val colors = BantuinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isHighlight) colors.primary else colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHighlight) colors.primary else colors.textPrimary,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}
