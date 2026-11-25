package id.bantuin.app.ui.provider.evaluation

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEvaluationScreen(
    repository: BantuinRepository,
    onNavigateBack: () -> Unit,
    viewModel: ProviderEvaluationViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Evaluasi & Bobot Ranking Mitra",
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
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        val result = uiState.trustResult

        when {
            uiState.isLoading && result == null -> {
                LoadingView(
                    message = "Menghitung evaluasi algoritma...",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            uiState.errorMessage != null && result == null -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat evaluasi.",
                    onRetry = { viewModel.loadEvaluation() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                val isAnomaly = result?.flagAnomaly == true
                val ratingDisplay = result?.ratingAvg?.let {
                    String.format(Locale.US, "Rating aktual %.2f / 5.0", it)
                } ?: "Belum ada ulasan (skor default 70%)"
                val completionDisplay = result?.completionRate?.let {
                    String.format(Locale.US, "Tingkat penyelesaian %.0f%% • Batal %.1f%%", it * 100.0, result.cancelRate)
                } ?: "Belum ada transaksi selesai (< 5 transaksi)"

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Transparansi algoritma pencocokan order berdasarkan performa aktual 30 hari terakhir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }

                    // 1. Status Evaluasi Terkini & Pengali Ranking
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "STATUS EVALUASI TERKINI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isAnomaly) colors.errorContainer else colors.successContainer)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isAnomaly) "Anomali Aktif" else "Aktif",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isAnomaly) colors.error else colors.success,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = colors.starTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.tierTitle,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isAnomaly) colors.errorContainer else colors.primaryContainer)
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = if (isAnomaly) colors.error else colors.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Pengali Matching:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.textPrimary
                                            )
                                        }
                                        Text(
                                            text = uiState.multiplierText,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isAnomaly) colors.error else colors.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Diagram Alur Umpan Balik Otomatis (F4 -> F1)
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Alur Umpan Balik Otomatis",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Otomatis F4 ke F1",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.success
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                FeedbackStepItem(
                                    stepNumber = "1",
                                    title = "Pesanan & Ulasan Selesai",
                                    subtitle = "Data selesai, ketepatan SLA, dan rating masuk",
                                    isDone = true
                                )
                                FeedbackStepItem(
                                    stepNumber = "2",
                                    title = "Evaluasi Trust Score (F4)",
                                    subtitle = "Menghitung skor ${result?.trustScore?.toInt() ?: 70}/100 dalam jendela ${BantuinConfig.TRUST_WINDOW_DAYS} hari",
                                    isDone = true
                                )
                                FeedbackStepItem(
                                    stepNumber = "3",
                                    title = "Pembaruan Bobot Rekomendasi (F1)",
                                    subtitle = "Menyesuaikan prioritas tampil di hasil pencarian",
                                    isDone = true,
                                    isLast = true
                                )
                            }
                        }
                    }

                    // 3. Distribusi Bobot Algoritma (50% Jarak, 35% Rating, 15% Penyelesaian)
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Distribusi Bobot Algoritma",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Total: 100%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                AlgorithmWeightBar(
                                    label = "Kedekatan Jarak (Haversine GPS)",
                                    percentage = (BantuinConfig.W_DISTANCE * 100).toInt(),
                                    sublabel = "Batas jangkauan maksimal ${BantuinConfig.MAX_RADIUS_KM.toInt()} km"
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                AlgorithmWeightBar(
                                    label = "Rating & Kepuasan Pelanggan",
                                    percentage = (BantuinConfig.W_RATING * 100).toInt(),
                                    sublabel = ratingDisplay
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                AlgorithmWeightBar(
                                    label = "Tingkat Penyelesaian Tugas",
                                    percentage = (BantuinConfig.W_COMPLETION * 100).toInt(),
                                    sublabel = completionDisplay
                                )
                            }
                        }
                    }

                    // 4. Log Sinkronisasi & Status Akun
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
                                    text = "Log Sinkronisasi Sistem",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = uiState.lastSyncLabel.ifBlank { "Siap dihitung ulang" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Metrik dihitung ulang otomatis setiap pesanan selesai atau dibatalkan.",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = colors.textMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (isAnomaly) colors.error else colors.success,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isAnomaly) {
                                                "Status Akun: Dalam Evaluasi Anomali"
                                            } else {
                                                "Status Akun: Bebas Pelanggaran"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = if (isAnomaly) {
                                                result?.reasons?.joinToString(", ") { it.description }.orEmpty()
                                            } else {
                                                "Tidak ada penalti aktif terdeteksi (${result?.txCount ?: 0} transaksi terekam)."
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = colors.textMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Dampak Visibilitas Terkini:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = uiState.visibilityText,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = colors.primary
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

                    // 5. Tombol Hitung Ulang & Kembali
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.recalculate() },
                                enabled = !uiState.isRecalculating,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(colors.outline)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        uiState.isRecalculating -> "Menghitung..."
                                        uiState.recalculatedRecently -> "Tersinkron"
                                        else -> "Hitung Ulang"
                                    },
                                    color = colors.textPrimary
                                )
                            }

                            Button(
                                onClick = onNavigateBack,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("Ke Dashboard")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackStepItem(
    stepNumber: String,
    title: String,
    subtitle: String,
    isDone: Boolean,
    isLast: Boolean = false
) {
    val colors = BantuinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isDone) colors.primary else colors.borderSubtle),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceSubtle)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary
                )
            }
        }
    }
    if (!isLast) {
        Box(
            modifier = Modifier
                .padding(start = 13.dp)
                .width(2.dp)
                .height(14.dp)
                .background(colors.borderSubtle)
        )
    }
}

@Composable
private fun AlgorithmWeightBar(
    label: String,
    percentage: Int,
    sublabel: String
) {
    val colors = BantuinTheme.colors
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textPrimary
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { percentage / 100f },
            color = colors.primary,
            trackColor = colors.surfaceSubtle,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = sublabel,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = colors.textMuted
        )
    }
}
