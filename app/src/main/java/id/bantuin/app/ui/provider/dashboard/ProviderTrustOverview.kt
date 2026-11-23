package id.bantuin.app.ui.provider.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.CompletionStatus
import id.bantuin.app.domain.model.MetricSnapshot
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.ui.common.ProviderBadgeChip
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.RedErrorContainer
import id.bantuin.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
internal fun ProviderTrustOverview(state: ProviderDashboardUiState, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.isLoading) CircularProgressIndicator()
        state.errorMessage?.let { message ->
            Text(message, color = RedError)
            TextButton(onClick = onRetry) { Text("Coba lagi") }
        }
        state.trustResult?.let { result ->
            TrustScoreCard(result)
            if (result.flagAnomaly) AnomalyAlert(result)
        }
        if (state.snapshots.size >= 2) TrustHistoryChart(state.snapshots)
        if (state.demoExamples.isNotEmpty()) {
            Text("Contoh performa mitra", style = MaterialTheme.typography.titleMedium)
            state.demoExamples.forEach { example ->
                TrustScoreCard(example.result, title = example.name)
            }
        }
    }
}

@Composable
private fun TrustScoreCard(result: TrustResult, title: String = "Trust Score") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NeutralSurface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("${formatMetric(result.trustScore)} / 100", style = MaterialTheme.typography.headlineLarge)
            ProviderBadgeChip(result.badge, result.flagAnomaly)
            LinearProgressIndicator(
                progress = { (result.trustScore / 100).toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = if (result.flagAnomaly) RedError else GreenSuccess
            )
            Text(
                "30 hari terakhir · ${result.txCount} transaksi",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            HorizontalDivider()
            MetricLine("Skor Rating", "${formatMetric(result.ratingScore)} / 100")
            MetricLine("On-Time SLA", "${formatMetric(result.onTimeRate)}%")
            MetricLine("Rasio Pembatalan", "${formatMetric(result.cancelRate)}%")
        }
    }
}

@Composable
internal fun AnomalyAlert(result: TrustResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RedErrorContainer, contentColor = RedError)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Peringatan Anomali", style = MaterialTheme.typography.titleMedium)
            result.reasons.forEach { reason -> Text("• ${reason.description}") }
            Text("Prioritas rekomendasi diturunkan.")
        }
    }
}

@Composable
private fun TrustHistoryChart(snapshots: List<MetricSnapshot>) {
    val description = snapshots.joinToString { "${it.label} ${formatMetric(it.trustScore)}" }
    Card(colors = CardDefaults.cardColors(containerColor = NeutralSurface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tren Trust Score", style = MaterialTheme.typography.titleMedium)
            Canvas(Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = description }) {
                val slotWidth = size.width / snapshots.size
                snapshots.forEachIndexed { index, snapshot ->
                    val barHeight = size.height * (snapshot.trustScore.toFloat() / 100f)
                    drawRect(
                        color = OrangePrimary,
                        topLeft = Offset(slotWidth * (index + 0.2f), size.height - barHeight),
                        size = Size(slotWidth * 0.6f, barHeight)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(snapshots.first().label, style = MaterialTheme.typography.labelSmall)
                Text(snapshots.last().label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
internal fun MetricLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

internal fun formatMetric(value: Double): String = String.format(Locale.forLanguageTag("id-ID"), "%.2f", value)

internal fun LazyListScope.transactionHistory(
    state: ProviderDashboardUiState,
    applyFilter: (CompletionStatus?) -> Unit
) {
    item { HistoryFilters(state.historyFilter, applyFilter) }
    state.historyError?.let { message ->
        item { Text(message, color = RedError) }
    }
    val filtered = state.transactions.filter { state.historyFilter == null || it.status == state.historyFilter }
    if (filtered.isEmpty() && state.historyError == null && !state.isLoading) {
        item { Text("Belum ada transaksi untuk filter ini.", color = TextSecondary) }
    }
    items(filtered, key = { it.bookingId }) { entry ->
        Card(colors = CardDefaults.cardColors(containerColor = NeutralSurface)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.status.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    "#${entry.bookingId.takeLast(8).uppercase()} · ${TimeUtils.formatInstantToDate(entry.resolvedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryFilters(current: CompletionStatus?, applyFilter: (CompletionStatus?) -> Unit) {
    Column {
        Text("Riwayat Transaksi · 30 hari", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = current == null, onClick = { applyFilter(null) }, label = { Text("Semua") })
            CompletionStatus.entries.forEach { status ->
                FilterChip(selected = current == status, onClick = { applyFilter(status) }, label = { Text(status.label) })
            }
        }
    }
}
