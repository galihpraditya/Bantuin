package id.bantuin.app.ui.provider.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme
import java.util.Locale
import kotlinx.coroutines.flow.drop

@Composable
fun ProviderDashboardScreen(
    repository: BantuinRepository,
    onNavigateToEvaluation: () -> Unit,
    onOpenWithdraw: () -> Unit,
    viewModel: ProviderDashboardViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(repository, viewModel) {
        repository.trustChanges.drop(1).collect { viewModel.loadData() }
    }
    val colors = BantuinTheme.colors

    Scaffold(
        topBar = {
            BantuinTopAppBar(title = "Dashboard Kinerja & Keuangan")
        },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading && uiState.trustResult == null) {
            LoadingView(
                message = "Memuat analisis keuangan & performa mitra...",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        val analytics = uiState.revenueAnalytics
        val activeResult = if (uiState.isSimulationActive) uiState.simResult else uiState.trustResult
        val trustScore = activeResult?.trustScore ?: 0.0
        val cancelRate = activeResult?.cancelRate ?: 0.0
        val onTimeRate = activeResult?.onTimeRate ?: 0.0
        val ratingText = activeResult?.ratingAvg?.let { String.format(Locale.US, "%.2f", it) } ?: "Baru"
        val isAnomaly = activeResult?.flagAnomaly == true

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error
                            )
                        }
                    }
                }
            }

            // Kartu 1 - Saldo Siap Cair
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Saldo Siap Cair",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textSecondary
                        )
                        Text(
                            text = CurrencyUtils.formatRupiah(analytics.availableBalance),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenWithdraw,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cairkan & Mutasi", fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = colors.borderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Sudah Dicairkan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = CurrencyUtils.formatRupiah(analytics.totalWithdrawn),
                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Sedang Diproses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = CurrencyUtils.formatRupiah(analytics.pendingWithdrawal),
                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Kartu 2 - Analisis Total Pendapatan (QRIS + Cash)
            item {
                val totalCombined = analytics.totalCombinedRevenue
                val qrisRatio = if (totalCombined > 0) {
                    (analytics.qrisGrossRevenue.toFloat() / totalCombined.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Analisis Total Pendapatan",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gabungan transaksi QRIS dan Tunai di lokasi yang sudah selesai",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }

                        Text(
                            text = CurrencyUtils.formatRupiah(totalCombined),
                            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        // Proportion Bar: 2 warna bermakna (QRIS = primary, Cash = success)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (totalCombined > 0) colors.success else colors.borderSubtle
                                )
                        ) {
                            if (totalCombined > 0 && qrisRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(qrisRatio)
                                        .fillMaxHeight()
                                        .background(colors.primary)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceSubtle)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.primary)
                                    )
                                    Text(
                                        text = "Pendapatan QRIS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = CurrencyUtils.formatRupiah(analytics.qrisGrossRevenue),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Masuk Saldo • ${analytics.completedQrisCount} transaksi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceSubtle)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.success)
                                    )
                                    Text(
                                        text = "Pendapatan Tunai",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = CurrencyUtils.formatRupiah(analytics.cashRevenue),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                    color = colors.success,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Cash di Lokasi • ${analytics.completedCashCount} transaksi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                }
            }

            // Kartu 3 - Provider Trust Score (F4 Index)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
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
                                    text = activeResult?.let { String.format(Locale.US, "%.2f", it.trustScore) } ?: "—",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 32.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
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
                                            activeResult == null -> "Data belum tersedia"
                                            else -> "Mitra Terpercaya"
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
                                    text = "45% Skor Rating + 35% Ketepatan SLA − 20% Rasio Pembatalan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Kartu 4 - Tiga Kartu Metrik Kinerja 30 Hari (Dinamis dari UseCase F4)
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
                            subtext = "Skor ${activeResult?.ratingScore?.toInt() ?: 0}%",
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

            // Kartu 5 - Tren Trust Score dari Snapshot Nyata
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
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
                            val dotCenterColor = colors.surface

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val count = snapshots.size.coerceAtLeast(1)
                                    val barWidth = (28.dp.toPx()).coerceAtMost((size.width / (count * 2f)))
                                    val spacing = (size.width - (barWidth * count)) / (count + 1)
                                    val points = snapshots.mapIndexed { idx, snap ->
                                        val x = spacing + idx * (barWidth + spacing)
                                        val barHeight = ((snap.trustScore.toFloat() / 100f).coerceIn(0.05f, 1f)) * (size.height - 12.dp.toPx())
                                        val y = size.height - barHeight
                                        val isLast = idx == snapshots.lastIndex
                                        drawRoundRect(
                                            color = if (isLast) activePrimary.copy(alpha = 0.25f) else mutedBar,
                                            topLeft = Offset(x, y),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                        )
                                        Offset(x + barWidth / 2f, y.coerceIn(6.dp.toPx(), size.height - 6.dp.toPx()))
                                    }
                                    for (i in 0 until points.lastIndex) {
                                        drawLine(
                                            color = activePrimary,
                                            start = points[i],
                                            end = points[i + 1],
                                            strokeWidth = 2.5.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                    points.forEachIndexed { idx, pt ->
                                        val isLast = idx == points.lastIndex
                                        drawCircle(
                                            color = if (isLast) activePrimary else dotCenterColor,
                                            radius = 4.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = activePrimary,
                                            radius = 4.dp.toPx(),
                                            center = pt,
                                            style = Stroke(width = 2.dp.toPx())
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
                                        text = "${snap.label} (${String.format(Locale.US, "%.2f", snap.trustScore)})",
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

            // Kartu 6 - Simulasi Dampak Performa & Link Evaluasi
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAnomaly) colors.errorContainer else colors.warningContainer
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(if (isAnomaly) colors.error else colors.borderSubtle)
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
                                activeResult?.reasons.orEmpty().joinToString("\n") { "• ${it.description}" } +
                                    "\nPengali skor rekomendasi 0,7×."
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
                                contentColor = if (colors.isDark) colors.background else Color.White
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

            item {
                OutlinedButton(
                    onClick = onNavigateToEvaluation,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                        .copy(brush = SolidColor(colors.outline)),
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
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
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
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
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
