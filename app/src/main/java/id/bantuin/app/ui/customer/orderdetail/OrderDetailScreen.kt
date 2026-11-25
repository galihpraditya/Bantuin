package id.bantuin.app.ui.customer.orderdetail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.BookingEvent
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.CountdownTimerView
import id.bantuin.app.ui.common.EmptyView
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.common.OsmMapView
import id.bantuin.app.ui.theme.BantuinTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderDetailScreen(
    bookingId: String,
    repository: BantuinRepository,
    onNavigateBack: () -> Unit,
    viewModel: OrderDetailViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bookingId) {
        viewModel.observeBooking(bookingId)
    }

    val booking = uiState.booking

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detail & Status Pesanan",
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
        when {
            uiState.isLoading && booking == null -> {
                LoadingView(
                    message = "Memuat detail pesanan...",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            uiState.errorMessage != null && booking == null -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat detail pesanan.",
                    onRetry = { viewModel.observeBooking(bookingId) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            booking == null -> {
                EmptyView(
                    title = "Pesanan Tidak Ditemukan",
                    message = "Data pesanan tidak tersedia atau sudah dihapus.",
                    actionText = "Kembali",
                    onAction = onNavigateBack,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                val currentStatus = booking.status
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Banner Status Pesanan + Countdown PENDING
                    item {
                        OrderStatusHeaderCard(
                            booking = booking,
                            onExpired = { viewModel.expireIfOverdue() }
                        )
                    }

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

                    // 2. Peta Lokasi Layanan / Posisi Mitra (jika koordinat tersedia)
                    val mapPoint = booking.providerLocation
                        ?: booking.customerLat?.let { lat ->
                            booking.customerLng?.let { lng -> GeoPoint(lat, lng) }
                        }
                    if (mapPoint != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                                )
                            ) {
                                OsmMapView(
                                    centerPoint = mapPoint,
                                    markerTitle = if (booking.providerLocation != null) {
                                        booking.providerName
                                    } else {
                                        "Lokasi Layanan"
                                    },
                                    zoomLevel = 15.0,
                                    isInteractive = true
                                )
                            }
                        }
                    }

                    // 3. Info Mitra & Aksi Hubungi via Telepon
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(colors.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = booking.providerName.take(1),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = colors.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = booking.providerName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "${booking.serviceName} • ${CurrencyUtils.formatRupiah(booking.totalCost)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                        Text(
                                            text = booking.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = {
                                        val phone = booking.providerPhone
                                        if (!phone.isNullOrBlank()) {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            context.startActivity(intent)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Nomor telepon mitra belum tersedia.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(colors.outline)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Hubungi Mitra via Telepon", color = colors.textPrimary)
                                }
                            }
                        }
                    }

                    // 4. Garis Waktu Proses Pengerjaan (Dinamis dari booking_events)
                    item {
                        BookingTimelineCard(
                            booking = booking,
                            events = uiState.events
                        )
                    }

                    // 5. Pembatalan Pelanggan (hanya di status PENDING / DIKONFIRMASI sesuai aturan)
                    if (currentStatus in listOf(BookingStatus.PENDING, BookingStatus.DIKONFIRMASI)) {
                        item {
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                enabled = !uiState.isUpdatingStatus,
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(colors.error)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Text(
                                    text = if (uiState.isUpdatingStatus) "Membatalkan..." else "Batalkan Pesanan",
                                    color = colors.error
                                )
                            }
                        }
                    }

                    // 6. Formulir Ulasan (Hanya saat Status SELESAI)
                    if (currentStatus == BookingStatus.SELESAI) {
                        item {
                            ReviewSectionCard(
                                rating = uiState.rating,
                                reviewComment = uiState.reviewComment,
                                selectedTags = uiState.selectedTags,
                                isSubmitting = uiState.isSubmittingReview,
                                isSubmitted = uiState.reviewSubmitted,
                                onRatingChange = { viewModel.setRating(it) },
                                onCommentChange = { viewModel.setReviewComment(it) },
                                onToggleTag = { viewModel.toggleTag(it) },
                                onSubmit = { viewModel.submitReview() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Batalkan Pesanan?") },
            text = {
                Text(
                    "Jadwal yang telah dipesan akan dilepaskan. Yakin ingin membatalkan pesanan ini?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        viewModel.updateStatus(BookingStatus.DIBATALKAN_PELANGGAN)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Ya, Batalkan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Kembali", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun OrderStatusHeaderCard(
    booking: BookingItem,
    onExpired: () -> Unit
) {
    val colors = BantuinTheme.colors
    val currentStatus = booking.status
    val isNegativeTerminal = currentStatus in listOf(
        BookingStatus.DITOLAK,
        BookingStatus.KEDALUWARSA,
        BookingStatus.DIBATALKAN_PELANGGAN,
        BookingStatus.DIBATALKAN_MITRA
    )
    val containerColor = when {
        currentStatus == BookingStatus.SELESAI -> colors.successContainer
        isNegativeTerminal -> colors.errorContainer
        else -> colors.warningContainer
    }
    val accentColor = when {
        currentStatus == BookingStatus.SELESAI -> colors.success
        isNegativeTerminal -> colors.error
        else -> colors.warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
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
                    text = "STATUS: ${currentStatus.displayName.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${booking.scheduledDate} • ${booking.scheduledTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (currentStatus) {
                    BookingStatus.PENDING -> "Menunggu Konfirmasi Mitra"
                    BookingStatus.DIKONFIRMASI -> "Pesanan Dikonfirmasi Mitra"
                    BookingStatus.DALAM_PENGERJAAN -> "Teknisi Sedang Mengerjakan Servis"
                    BookingStatus.SELESAI -> "Layanan Telah Selesai Dikerjakan"
                    else -> "Pesanan ${currentStatus.displayName}"
                },
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (currentStatus) {
                    BookingStatus.PENDING -> "Mitra memiliki batas waktu respon 15 menit sejak pesanan dibuat."
                    BookingStatus.DIKONFIRMASI -> "Mitra telah menjadwalkan kedatangan sesuai waktu terpilih."
                    BookingStatus.DALAM_PENGERJAAN -> "Pengerjaan sedang berlangsung di lokasi Anda."
                    BookingStatus.SELESAI -> "Pembayaran tunai di tempat selesai. Anda dapat memberi ulasan di bawah."
                    else -> "Pesanan tidak lagi aktif."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            if (currentStatus == BookingStatus.PENDING && booking.expiresAt != null) {
                Spacer(modifier = Modifier.height(10.dp))
                CountdownTimerView(
                    targetEpochMillis = booking.expiresAt.toEpochMilliseconds(),
                    onExpired = onExpired
                )
            }
        }
    }
}

@Composable
private fun BookingTimelineCard(
    booking: BookingItem,
    events: List<BookingEvent>
) {
    val colors = BantuinTheme.colors
    val status = booking.status

    fun findTimeFor(targetStatus: BookingStatus): String? {
        val eventTime = events.lastOrNull { it.status == targetStatus }?.createdAt
        val fallbackInstant = when (targetStatus) {
            BookingStatus.PENDING -> booking.createdAt
            BookingStatus.DALAM_PENGERJAAN -> booking.startedAt
            BookingStatus.SELESAI -> booking.finishedAt
            else -> null
        }
        val instant = eventTime ?: fallbackInstant
        return instant?.let { TimeUtils.formatInstantToTime(it) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
                    text = "Riwayat Status Pesanan",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "ID #${booking.id.take(8).uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isCancelledOrRejected = status in listOf(
                BookingStatus.DITOLAK,
                BookingStatus.KEDALUWARSA,
                BookingStatus.DIBATALKAN_PELANGGAN,
                BookingStatus.DIBATALKAN_MITRA
            )

            TimelineStepItem(
                stepNumber = "1",
                title = "Pesanan Dibuat",
                description = "Permintaan layanan ${booking.serviceName} terkirim ke mitra",
                timeText = findTimeFor(BookingStatus.PENDING) ?: "Tercatat",
                isCompleted = true,
                isLast = false
            )

            if (isCancelledOrRejected) {
                TimelineStepItem(
                    stepNumber = "2",
                    title = status.displayName,
                    description = "Pesanan dihentikan dan jadwal telah dilepaskan",
                    timeText = findTimeFor(status) ?: TimeUtils.formatInstantToTime(booking.createdAt),
                    isCompleted = true,
                    isError = true,
                    isLast = true
                )
            } else {
                val confirmedDone = status in listOf(
                    BookingStatus.DIKONFIRMASI,
                    BookingStatus.DALAM_PENGERJAAN,
                    BookingStatus.SELESAI
                )
                val progressDone = status in listOf(
                    BookingStatus.DALAM_PENGERJAAN,
                    BookingStatus.SELESAI
                )
                val finishedDone = status == BookingStatus.SELESAI

                TimelineStepItem(
                    stepNumber = "2",
                    title = "Dikonfirmasi Mitra",
                    description = "${booking.providerName} menyetujui jadwal kunjungan",
                    timeText = findTimeFor(BookingStatus.DIKONFIRMASI) ?: if (confirmedDone) "Terkonfirmasi" else "Menunggu",
                    isCompleted = confirmedDone
                )
                TimelineStepItem(
                    stepNumber = "3",
                    title = "Pengerjaan ${booking.serviceName}",
                    description = "Teknisi menangani pekerjaan di lokasi Anda",
                    timeText = findTimeFor(BookingStatus.DALAM_PENGERJAAN) ?: if (progressDone) "Berlangsung" else "Menunggu",
                    isCompleted = progressDone
                )
                TimelineStepItem(
                    stepNumber = "4",
                    title = "Selesai & Pembayaran Tunai",
                    description = "Pemeriksaan hasil kerja dan pemberian ulasan",
                    timeText = findTimeFor(BookingStatus.SELESAI) ?: if (finishedDone) "Selesai" else "Menunggu",
                    isCompleted = finishedDone,
                    isLast = true
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewSectionCard(
    rating: Int,
    reviewComment: String,
    selectedTags: Set<String>,
    isSubmitting: Boolean,
    isSubmitted: Boolean,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val colors = BantuinTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.success.copy(alpha = 0.5f))
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.successContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Layanan Telah Selesai",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Bagikan pengalaman Anda untuk menjaga kualitas layanan mitra.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { starIndex ->
                    IconButton(
                        onClick = { if (!isSubmitted) onRatingChange(starIndex) },
                        enabled = !isSubmitted
                    ) {
                        Icon(
                            imageVector = if (starIndex <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Bintang $starIndex",
                            tint = if (starIndex <= rating) colors.starTint else colors.textMuted,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Text(
                text = when (rating) {
                    5 -> "5.0 Sangat Memuaskan"
                    4 -> "4.0 Baik"
                    3 -> "3.0 Cukup Baik"
                    else -> "$rating.0 Perlu Ditingkatkan"
                },
                style = MaterialTheme.typography.labelMedium,
                color = colors.starTint,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            val allTags = listOf("Tepat Waktu", "Pekerjaan Rapi", "Sopan & Ramah", "Harga Sesuai")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTags.forEach { tag ->
                    val isSelected = selectedTags.contains(tag)
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) colors.primaryContainer else colors.surfaceSubtle)
                            .clickable(enabled = !isSubmitted) { onToggleTag(tag) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) colors.onPrimaryContainer else colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = reviewComment,
                onValueChange = { if (!isSubmitted) onCommentChange(it) },
                enabled = !isSubmitted,
                label = { Text("Catatan Ulasan (${reviewComment.length}/300)") },
                isError = reviewComment.trim().length > 300,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isSubmitted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.successContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ulasan Terkirim",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.success,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(if (isSubmitting) "Mengirim Ulasan..." else "Kirim Ulasan")
                }
            }
        }
    }
}

@Composable
private fun TimelineStepItem(
    stepNumber: String,
    title: String,
    description: String,
    timeText: String,
    isCompleted: Boolean,
    isError: Boolean = false,
    isLast: Boolean = false
) {
    val colors = BantuinTheme.colors
    val dotColor = when {
        isError -> colors.error
        isCompleted -> colors.success
        else -> colors.borderSubtle
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(dotColor),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        text = stepNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textPrimary
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(dotColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
