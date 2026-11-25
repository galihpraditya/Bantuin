package id.bantuin.app.ui.provider.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemeMode
import id.bantuin.app.core.theme.ThemePreferenceStore
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.CountdownTimerView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme
import java.util.Locale

@Composable
fun ProviderDashboardScreen(
    onNavigateToEvaluation: () -> Unit,
    onSwitchToCustomer: () -> Unit,
    onSwitchToCustomerUnauthorized: () -> Unit,
    onSignOut: () -> Unit,
    repository: BantuinRepository,
    themeStore: ThemePreferenceStore = remember { InMemoryThemeStore() },
    viewModel: ProviderDashboardViewModel = viewModel(factory = BantuinViewModelFactory(repository, themeStore)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    if (uiState.isLoading && uiState.trustResult == null) {
        LoadingView(
            message = "Memuat metrik performa mitra...",
            modifier = modifier.background(colors.background)
        )
        return
    }

    val activeResult = if (uiState.isSimulationActive) uiState.simResult else uiState.trustResult
    val trustScore = activeResult?.trustScore ?: 70.0
    val cancelRate = activeResult?.cancelRate ?: 0.0
    val onTimeRate = activeResult?.onTimeRate ?: 0.0
    val ratingText = activeResult?.ratingAvg?.let { String.format(Locale.US, "%.2f", it) } ?: "Baru"
    val isAnomaly = activeResult?.flagAnomaly == true ||
        (activeResult?.txCount ?: 0) >= BantuinConfig.MIN_TX_FOR_EVALUATION &&
        (trustScore < BantuinConfig.TRUST_MIN_THRESHOLD || cancelRate > BantuinConfig.CANCEL_RATE_MAX_PCT)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Ringkasan Akun & Pengalih Peran
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.user?.fullName?.trim()?.firstOrNull()?.uppercase() ?: "M",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.providerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                if (!isAnomaly) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.success,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${uiState.providerSpecialty} • ${uiState.providerAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = uiState.user?.email ?: "-",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.switchToCustomer(
                                    onAllowed = onSwitchToCustomer,
                                    onUnauthorized = onSwitchToCustomerUnauthorized
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Beralih ke Pelanggan", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = { viewModel.signOut(onSignOut) },
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(colors.error)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Keluar", color = colors.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 2. Provider Trust Score (F4 Index)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Provider Trust Score",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isAnomaly) colors.errorContainer else colors.successContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isAnomaly) "Prioritas Turun" else "Visibilitas Penuh",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAnomaly) colors.error else colors.success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val trackColor = colors.borderSubtle
                    val arcColor = if (isAnomaly) colors.error else colors.success

                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 12.dp.toPx()
                            val arcSize = size.width - strokeWidth
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                            drawArc(
                                color = trackColor,
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            val sweepAngle = ((trustScore.coerceIn(0.0, 100.0) / 100.0) * 270f).toFloat()
                            drawArc(
                                color = arcColor,
                                startAngle = 135f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${trustScore.toInt()}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "dari 100",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isAnomaly) colors.errorContainer else colors.successContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when {
                                        isAnomaly -> "Evaluasi"
                                        (activeResult?.txCount ?: 0) < BantuinConfig.MIN_TX_FOR_EVALUATION -> "Mitra Baru"
                                        trustScore >= 85.0 -> "Sangat Baik"
                                        else -> "Baik"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isAnomaly) colors.error else colors.success
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceSubtle)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Bobot Evaluasi (${activeResult?.txCount ?: 0} transaksi / 30 hr):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAnomaly) "Anomali" else "Aman",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAnomaly) colors.error else colors.success
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "45% Rating + 35% Ketepatan SLA + 20% Reliabilitas (Tanpa Pembatalan Mitra).",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3. Tiga Kartu Metrik Kinerja 30 Hari (Dinamis dari UseCase F4)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Metrik Kinerja 30 Hari",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (uiState.isSimulationActive) "Mode Simulasi" else "Data Aktual",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.isSimulationActive) colors.warning else colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        icon = Icons.Default.Star,
                        value = ratingText,
                        unit = "Skala 5.0",
                        subtext = "Skor ${activeResult?.ratingScore?.toInt() ?: 70}%",
                        color = colors.starTint,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        icon = Icons.Default.Schedule,
                        value = String.format(Locale.US, "%.1f%%", onTimeRate),
                        unit = "Ketepatan SLA",
                        subtext = "Toleransi ${BantuinConfig.LATE_TOLERANCE_MIN} mnt",
                        color = colors.success,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        icon = Icons.Default.Close,
                        value = String.format(Locale.US, "%.1f%%", cancelRate),
                        unit = "Rasio Batal",
                        subtext = if (cancelRate > BantuinConfig.CANCEL_RATE_MAX_PCT) "Melebihi 20%" else "Batas <= 20%",
                        color = if (cancelRate > BantuinConfig.CANCEL_RATE_MAX_PCT) colors.error else colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Tren Trust Score dari Snapshot Nyata
        item {
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
                        text = "Riwayat Tren Trust Score",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Rekaman evaluasi berkala dari database metrik",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val snapshots = uiState.snapshots
                    if (snapshots.isEmpty()) {
                        Text(
                            text = "Belum ada riwayat snapshot tersimpan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    } else {
                        val activePrimary = if (isAnomaly) colors.error else colors.primary
                        val mutedBar = colors.borderSubtle

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val count = snapshots.size.coerceAtLeast(1)
                                val barWidth = (28.dp.toPx()).coerceAtMost((size.width / (count * 2f)))
                                val spacing = (size.width - (barWidth * count)) / (count + 1)
                                snapshots.forEachIndexed { idx, snap ->
                                    val x = spacing + idx * (barWidth + spacing)
                                    val barHeight = ((snap.trustScore.toFloat() / 100f).coerceIn(0.05f, 1f)) * (size.height - 12.dp.toPx())
                                    val y = size.height - barHeight
                                    val isLast = idx == snapshots.lastIndex
                                    drawRoundRect(
                                        color = if (isLast) activePrimary else mutedBar,
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            snapshots.forEachIndexed { idx, snap ->
                                val isLast = idx == snapshots.lastIndex
                                Text(
                                    text = "${snap.label} (${snap.trustScore.toInt()})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isLast) colors.primary else colors.textMuted,
                                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Kartu Simulasi Dampak Performa (What-if)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAnomaly) colors.errorContainer else colors.warningContainer
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isAnomaly) colors.error else colors.borderSubtle
                    )
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAnomaly) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isAnomaly) colors.error else colors.warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAnomaly) "Peringatan Anomali Akun" else "Monitoring Kualitas & Simulasi",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isAnomaly) colors.error else colors.warning,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isAnomaly) {
                            "Trust Score di bawah 60 atau pembatalan mitra di atas 20% menurunkan visibilitas ke faktor pengali 0.7x."
                        } else {
                            "Uji simulasi untuk melihat bagaimana keterlambatan atau pembatalan memengaruhi status prioritas akun."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (uiState.isSimulationActive) {
                                viewModel.resetSimulation()
                            } else {
                                viewModel.updateSimulation(rating = 2.0, lateCount = 8, cancelCount = 12)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isSimulationActive) colors.success else colors.error,
                            contentColor = colors.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = if (uiState.isSimulationActive) "Kembali ke Data Aktual" else "Simulasikan Kondisi Anomali",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // 6. Kartu Pesanan Masuk Terbaru (Nyata dari BookingRepository)
        item {
            LatestPendingOrderCard(
                order = uiState.latestPendingOrder,
                isResponding = uiState.isRespondingOrder,
                onExpired = { viewModel.loadData() },
                onRespond = { id, accept -> viewModel.respondToPendingOrder(id, accept) }
            )
        }

        // 7. Pengaturan Tema Aplikasi (Light / Dark / System)
        item {
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
                    Spacer(modifier = Modifier.height(6.dp))
                    val themeOptions = listOf(
                        Triple(ThemeMode.SYSTEM, "Ikuti Sistem", Icons.Default.BrightnessAuto),
                        Triple(ThemeMode.LIGHT, "Mode Terang", Icons.Default.LightMode),
                        Triple(ThemeMode.DARK, "Mode Gelap", Icons.Default.DarkMode)
                    )
                    themeOptions.forEach { (mode, label, icon) ->
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
        }

        // 8. Tombol ke Evaluasi & Bobot Algoritma
        item {
            OutlinedButton(
                onClick = onNavigateToEvaluation,
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.outline)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = "Lihat Rincian Bobot Algoritma & Evaluasi",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LatestPendingOrderCard(
    order: BookingItem?,
    isResponding: Boolean,
    onExpired: () -> Unit,
    onRespond: (String, Boolean) -> Unit
) {
    val colors = BantuinTheme.colors
    if (order == null) {
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
                    text = "Antrean Pesanan Baru",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tidak ada pesanan baru yang menunggu konfirmasi saat ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.primary)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PESANAN MENUNGGU KONFIRMASI",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
                if (order.expiresAt != null) {
                    CountdownTimerView(
                        targetEpochMillis = order.expiresAt.toEpochMilliseconds(),
                        onExpired = onExpired
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = order.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = CurrencyUtils.formatRupiah(order.totalCost),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${order.serviceName} • ${order.scheduledDate} (${order.scheduledTime})",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onRespond(order.id, false) },
                    enabled = !isResponding,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.error)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Tolak", color = colors.error)
                }

                Button(
                    onClick = { onRespond(order.id, true) },
                    enabled = !isResponding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Terima Pesanan")
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    value: String,
    unit: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = colors.textSecondary
            )
        }
    }
}
