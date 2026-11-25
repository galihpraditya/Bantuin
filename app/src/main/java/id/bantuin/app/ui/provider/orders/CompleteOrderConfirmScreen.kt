package id.bantuin.app.ui.provider.orders

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.EmptyView
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.common.SlideToConfirmBar
import id.bantuin.app.ui.theme.BantuinMotion
import id.bantuin.app.ui.theme.BantuinTheme
import id.bantuin.app.ui.theme.rememberReducedMotion

@Composable
fun CompleteOrderConfirmScreen(
    bookingId: String,
    repository: BantuinRepository,
    onNavigateBack: () -> Unit,
    onCompleted: () -> Unit,
    viewModel: CompleteOrderConfirmViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    LaunchedEffect(bookingId) {
        viewModel.loadBooking(bookingId)
    }

    Scaffold(
        topBar = {
            BantuinTopAppBar(
                title = "Selesaikan Pesanan",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        val booking = uiState.booking
        when {
            uiState.isLoading && booking == null -> {
                LoadingView(
                    message = "Memuat detail penyelesaian pesanan...",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            uiState.errorMessage != null && booking == null -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat detail pesanan.",
                    onRetry = { viewModel.loadBooking(bookingId) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            booking == null -> {
                EmptyView(
                    title = "Pesanan Tidak Ditemukan",
                    message = "Data pesanan tidak ditemukan atau sudah diperbarui.",
                    actionText = "Kembali",
                    onAction = onNavigateBack,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Ringkasan Pesanan
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ringkasan Pekerjaan",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = booking.serviceName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Pelanggan: ${booking.customerName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Lokasi: ${booking.address}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "Jadwal: ${booking.scheduledDate} • ${booking.scheduledTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )

                            HorizontalDivider(color = colors.borderSubtle)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Tagihan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = CurrencyUtils.formatRupiah(booking.totalCost),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 2. Kartu Verifikasi Pembayaran
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (booking.paymentMethod == PaymentMethod.QRIS) colors.infoContainer
                                            else colors.warningContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (booking.paymentMethod == PaymentMethod.QRIS) {
                                            Icons.Default.QrCode2
                                        } else {
                                            Icons.Default.Payments
                                        },
                                        contentDescription = null,
                                        tint = if (booking.paymentMethod == PaymentMethod.QRIS) {
                                            colors.info
                                        } else {
                                            colors.warning
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Verifikasi Pembayaran",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = booking.paymentMethod.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            if (booking.paymentMethod == PaymentMethod.QRIS) {
                                val isPaid = booking.paymentStatus == PaymentStatus.PAID
                                BadgeContainer(
                                    text = if (isPaid) "QRIS Terverifikasi / Lunas" else "Menunggu Pelanggan Bayar QRIS",
                                    color = if (isPaid) colors.success else colors.warning,
                                    backgroundColor = if (isPaid) colors.successContainer else colors.warningContainer
                                )
                                Text(
                                    text = if (isPaid) {
                                        "Dana sebesar ${CurrencyUtils.formatRupiah(booking.totalCost)} otomatis masuk ke Saldo Siap Cair setelah pesanan diselesaikan."
                                    } else {
                                        "Pelanggan belum menyelesaikan pembayaran QRIS. Minta pelanggan menekan 'Bayar QRIS' di halaman detail pesanan."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            } else {
                                Text(
                                    text = "Pembayaran tunai diterima langsung di lokasi dan dicatat pada Total Pendapatan Tunai (tidak menambah Saldo Siap Cair).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surfaceSubtle)
                                        .clickable(enabled = !uiState.isCompleted && !uiState.isSubmitting) {
                                            viewModel.setCashCollectedConfirmed(!uiState.cashCollectedConfirmed)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = uiState.cashCollectedConfirmed,
                                        onCheckedChange = { checked ->
                                            viewModel.setCashCollectedConfirmed(checked)
                                        },
                                        enabled = !uiState.isCompleted && !uiState.isSubmitting,
                                        colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Saya telah menerima pembayaran tunai sebesar ${CurrencyUtils.formatRupiah(booking.totalCost)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // 3. Catatan Kerja Opsional
                    OutlinedTextField(
                        value = uiState.completionNote,
                        onValueChange = { viewModel.onCompletionNoteChange(it) },
                        label = { Text("Catatan Penyelesaian (Opsional)") },
                        placeholder = { Text("Contoh: Filter dibersihkan, tekanan freon sudah normal.") },
                        enabled = !uiState.isCompleted && !uiState.isSubmitting,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.borderSubtle
                        ),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.errorContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error
                                )
                            }
                        }
                    }

                    // 4. SlideToConfirmBar atau Status Selesai
                    if (uiState.isCompleted) {
                        val reducedMotion = rememberReducedMotion()
                        val popScale = androidx.compose.runtime.remember {
                            Animatable(if (reducedMotion) 1f else 0.55f)
                        }
                        LaunchedEffect(uiState.isCompleted) {
                            if (!reducedMotion) {
                                popScale.animateTo(1f, BantuinMotion.bouncySpring())
                            }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.successContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .graphicsLayer {
                                            scaleX = popScale.value
                                            scaleY = popScale.value
                                        }
                                        .clip(CircleShape)
                                        .background(colors.success),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (colors.isDark) colors.background else Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Text(
                                    text = "Pesanan Berhasil Diselesaikan",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.success,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = onCompleted,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("Kembali ke Antrean Pesanan")
                                }
                            }
                        }
                    } else {
                        SlideToConfirmBar(
                            label = "Geser untuk Selesaikan Pesanan",
                            enabled = uiState.canConfirm,
                            isLoading = uiState.isSubmitting,
                            onConfirmed = {
                                viewModel.confirmCompletion(onSuccess = onCompleted)
                            },
                            accessibilityActionLabel = "Konfirmasi Selesaikan Pesanan"
                        )
                    }
                }
            }
        }
    }
}
