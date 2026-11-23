package id.bantuin.app.ui.provider.evaluation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.LiveTrustRefresh
import id.bantuin.app.ui.common.ProviderBadgeChip
import id.bantuin.app.ui.provider.dashboard.AnomalyAlert
import id.bantuin.app.ui.provider.dashboard.MetricLine
import id.bantuin.app.ui.provider.dashboard.ProviderDashboardViewModel
import id.bantuin.app.ui.provider.dashboard.formatMetric
import id.bantuin.app.ui.theme.NeutralBackground
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEvaluationScreen(
    repository: BantuinRepository,
    onNavigateBack: () -> Unit,
    viewModel: ProviderDashboardViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val user by repository.currentUser().collectAsState(initial = null)
    val providerId = user?.id
    LiveTrustRefresh(repository.trustChanges, providerId) { background ->
        providerId?.let { viewModel.loadData(it, background) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = NeutralBackground,
        topBar = {
            TopAppBar(
                title = { Text("Evaluasi & Prioritas Mitra") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeutralBackground),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (providerId == null || state.providerId != providerId || state.isLoading) {
                item { CircularProgressIndicator() }
                return@LazyColumn
            }
            state.errorMessage?.let { message -> item { Text(message, color = RedError) } }
            state.trustResult?.let { result ->
                item { EvaluationMetrics(result) }
                if (result.flagAnomaly) item { AnomalyAlert(result) }
            }
            state.checkedAt?.let { timestamp ->
                item {
                    Text(
                        "Data diperiksa ${TimeUtils.formatInstantToDate(timestamp)} · ${TimeUtils.formatInstantToTime(timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            item {
                Button(
                    onClick = viewModel::refresh,
                    enabled = !state.isRefreshing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isRefreshing) "Menghitung ulang…" else "Hitung Ulang")
                }
            }
        }
    }
}

@Composable
private fun EvaluationMetrics(result: TrustResult) {
    Card(colors = CardDefaults.cardColors(containerColor = NeutralSurface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Kontribusi Metrik · 30 hari", style = MaterialTheme.typography.titleMedium)
            ProviderBadgeChip(result.badge, result.flagAnomaly)
            MetricLine("Skor Rating × 45%", "+${formatMetric(BantuinConfig.W_TRUST_RATING * result.ratingScore)}")
            MetricLine("On-Time SLA × 35%", "+${formatMetric(BantuinConfig.W_TRUST_ONTIME * result.onTimeRate)}")
            MetricLine("Rasio Pembatalan × 20%", "−${formatMetric(BantuinConfig.W_TRUST_CANCELLATION * result.cancelRate)}")
            HorizontalDivider()
            MetricLine("Trust Score", "${formatMetric(result.trustScore)} / 100")
            val priority = if (result.flagAnomaly) BantuinConfig.ANOMALY_PENALTY_FACTOR else 1.0
            MetricLine("Pengali skor rekomendasi", "${formatMetric(priority)}×")
        }
    }
}
