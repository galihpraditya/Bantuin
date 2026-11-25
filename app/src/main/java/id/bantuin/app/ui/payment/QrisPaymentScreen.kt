package id.bantuin.app.ui.payment

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.EmptyView
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme

@Composable
fun QrisPaymentScreen(
    bookingId: String,
    repository: BantuinRepository,
    onNavigateBack: () -> Unit,
    onViewOrderDetail: (String) -> Unit,
    viewModel: QrisPaymentViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
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
                title = "Pembayaran QRIS",
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
                    message = "Menyiapkan kode QRIS...",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            uiState.errorMessage != null && booking == null -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat tagihan QRIS.",
                    onRetry = { viewModel.loadBooking(bookingId) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            booking == null -> {
                EmptyView(
                    title = "Tagihan Tidak Ditemukan",
                    message = "Data pesanan tidak ditemukan atau sudah dihapus.",
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Ringkasan Pesanan & Total
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
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ID #${booking.id.take(8).uppercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textMuted
                                )
                                BadgeContainer(
                                    text = if (uiState.isPaid) "QRIS • Lunas" else booking.paymentStatus.label,
                                    color = if (uiState.isPaid) colors.success else colors.warning,
                                    backgroundColor = if (uiState.isPaid) colors.successContainer else colors.warningContainer
                                )
                            }

                            Text(
                                text = booking.serviceName,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${booking.providerName} • ${booking.scheduledDate}, ${booking.scheduledTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
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
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 2. Kartu QR Code Deterministik (Latar terang bersih agar selalu kontras & mudah dipindai)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "QRIS Nasional • Bantuin Pay",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (uiState.isPaid) {
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.successContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(colors.success),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Pembayaran Lunas",
                                                tint = if (colors.isDark) colors.background else Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Text(
                                            text = "Pembayaran Diterima",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.success,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Dana ditahan aman hingga layanan selesai.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DeterministicQrCanvas(
                                        seed = "${booking.id}|${booking.totalCost}",
                                        modifier = Modifier
                                            .size(200.dp)
                                            .semantics {
                                                contentDescription = "Kode QRIS untuk pesanan ${booking.id}"
                                            }
                                    )
                                }

                                Text(
                                    text = "NMID: ID1026000${booking.id.hashCode().ushr(1) % 1000000}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceSubtle)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = colors.success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (uiState.isPaid) {
                                        "Pembayaran Anda sudah terkonfirmasi lunas. Anda dapat memantau kedatangan mitra di detail pesanan."
                                    } else {
                                        "Pindai kode QRIS di atas melalui aplikasi e-wallet atau m-banking, atau gunakan tombol simulasi di bawah untuk pengujian."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    if (!uiState.errorMessage.isNullOrBlank()) {
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

                    // 3. Tombol Aksi
                    if (!uiState.isPaid && booking.paymentStatus != PaymentStatus.PAID) {
                        Button(
                            onClick = { viewModel.confirmSimulatedPayment() },
                            enabled = !uiState.isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            if (uiState.isProcessing) {
                                CircularProgressIndicator(
                                    color = colors.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (uiState.isProcessing) "Memverifikasi..." else "Simulasi Pembayaran QRIS Berhasil",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onViewOrderDetail(booking.id) },
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                            .copy(brush = SolidColor(colors.outline)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = if (uiState.isPaid) "Lihat Detail Pesanan" else "Bayar Nanti / Lihat Pesanan",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeterministicQrCanvas(
    seed: String,
    modifier: Modifier = Modifier
) {
    val matrix = remember(seed) { buildQrMatrix(seed, size = 21) }
    val darkColor = Color(0xFF111827)

    Canvas(modifier = modifier) {
        val count = matrix.size
        val cellSize = size.width / count.toFloat()
        for (row in 0 until count) {
            for (col in 0 until count) {
                if (matrix[row][col]) {
                    drawRect(
                        color = darkColor,
                        topLeft = Offset(col * cellSize, row * cellSize),
                        size = Size(cellSize + 0.5f, cellSize + 0.5f)
                    )
                }
            }
        }
    }
}

private fun buildQrMatrix(seed: String, size: Int): Array<BooleanArray> {
    val grid = Array(size) { BooleanArray(size) }

    fun placeFinder(top: Int, left: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                val isInner = r in 2..4 && c in 2..4
                grid[top + r][left + c] = isBorder || isInner
            }
        }
    }

    placeFinder(0, 0)
    placeFinder(0, size - 7)
    placeFinder(size - 7, 0)

    // Timing patterns
    for (i in 8 until size - 8) {
        val bit = i % 2 == 0
        grid[6][i] = bit
        grid[i][6] = bit
    }

    // Deterministic data fill outside finder + separator zones
    var state = seed.hashCode().toLong() xor 0x5DEECE66DL
    for (r in 0 until size) {
        for (c in 0 until size) {
            val inTopLeft = r < 8 && c < 8
            val inTopRight = r < 8 && c >= size - 8
            val inBottomLeft = r >= size - 8 && c < 8
            val isTiming = r == 6 || c == 6
            if (inTopLeft || inTopRight || inBottomLeft || isTiming) continue

            state = (state * 6364136223846793005L + 1442695040888963407L)
            grid[r][c] = ((state ushr 32) and 1L) == 1L
        }
    }
    return grid
}
