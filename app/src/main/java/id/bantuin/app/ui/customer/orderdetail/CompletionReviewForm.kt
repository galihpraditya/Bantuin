package id.bantuin.app.ui.customer.orderdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.TextSecondary

private val reviewTags = listOf("Tepat Waktu", "Pekerjaan Rapi", "Sopan & Ramah", "Harga Sesuai")

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CompletionReviewForm(state: OrderDetailUiState, viewModel: OrderDetailViewModel) {
    val booking = state.booking ?: return
    val options = booking.status.completionOptions()
    var deleteTarget by rememberSaveable(state.bookingId) { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = NeutralSurface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Konfirmasi Penyelesaian & Ulasan", style = MaterialTheme.typography.titleMedium)
            Text("Status penyelesaian", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { status ->
                    FilterChip(
                        selected = status == state.completion,
                        onClick = { viewModel.setCompletion(status) },
                        enabled = !state.isSaving,
                        label = { Text(status.label) }
                    )
                }
            }

            Text("Rating pelanggan", style = MaterialTheme.typography.labelLarge)
            Row {
                (1..5).forEach { star ->
                    IconButton(
                        onClick = { viewModel.setRating(star) },
                        enabled = !state.isSaving,
                        modifier = Modifier.semantics { selected = star == state.rating }
                    ) {
                        Icon(
                            if (star <= state.rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "$star bintang",
                            tint = if (star <= state.rating) OrangePrimary else TextSecondary
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                reviewTags.forEach { tag ->
                    FilterChip(
                        selected = tag in state.selectedTags,
                        onClick = { viewModel.toggleTag(tag) },
                        enabled = !state.isSaving,
                        label = { Text(tag) }
                    )
                }
            }
            OutlinedTextField(
                value = state.reviewComment,
                onValueChange = viewModel::setReviewComment,
                enabled = !state.isSaving,
                label = { Text("Komentar (opsional)") },
                supportingText = { Text("${state.reviewComment.length}/300") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            state.successMessage?.let { Text(it, color = GreenSuccess) }
            Button(
                onClick = viewModel::submitReview,
                enabled = !state.isSaving && state.completion != null && state.completion in options,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(when {
                    state.isSaving -> "Memproses…"
                    booking.review != null -> "Simpan Perubahan"
                    else -> "Simpan Konfirmasi & Ulasan"
                })
            }
            if (booking.review != null || booking.outcome?.customerConfirmed == true) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (booking.review != null) {
                        TextButton(onClick = { deleteTarget = "ulasan" }, enabled = !state.isSaving) {
                            Text("Hapus ulasan")
                        }
                    }
                    if (booking.outcome?.customerConfirmed == true) {
                        TextButton(onClick = { deleteTarget = "konfirmasi" }, enabled = !state.isSaving) {
                            Text("Hapus konfirmasi")
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus $target?") },
            text = { Text(if (target == "ulasan") "Rating dan komentar akan dihapus."
                else "Status penyelesaian kembali mengikuti riwayat pesanan.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    if (target == "ulasan") viewModel.deleteReview() else viewModel.deleteConfirmation()
                }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } }
        )
    }
}
