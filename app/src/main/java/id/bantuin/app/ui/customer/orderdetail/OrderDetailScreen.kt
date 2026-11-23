package id.bantuin.app.ui.customer.orderdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.EmptyView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.common.OsmMapView
import id.bantuin.app.ui.theme.BorderSubtle
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.GreenSuccessContainer
import id.bantuin.app.ui.theme.NeutralBackground
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangeContainer
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.TextMuted
import id.bantuin.app.ui.theme.TextPrimary
import id.bantuin.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

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

    androidx.compose.runtime.LaunchedEffect(bookingId) {
        viewModel.observeBooking(bookingId)
    }

    val booking = uiState.booking
    val currentStatus = booking?.status
    val isMitraAssigned = currentStatus?.isProviderAssigned == true

    if (booking == null || currentStatus == null) {
        if (uiState.isLoadingBooking) {
            LoadingView(message = "Memuat status pesanan...")
        } else {
            EmptyView(
                title = "Pesanan Tidak Ditemukan",
                message = "Pesanan ini tidak tersedia atau sudah dihapus. Kembali ke daftar pesanan untuk memuat ulang."
            )
        }
        return
    }

    // Review form states
    val rating = uiState.rating
    val reviewComment = uiState.reviewComment
    val selectedTags = uiState.selectedTags
    val isReviewSubmitted = uiState.reviewSubmitted

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tracking & Ulasan",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeutralBackground)
            )
        },
        containerColor = NeutralBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Banner Status Pelacakan Langsung
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFE082)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "● STATUS: ${currentStatus.displayName.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OrangePrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (currentStatus) {
                                    BookingStatus.DIKONFIRMASI -> "Estimasi tiba ±12 mnt"
                                    BookingStatus.DALAM_PENGERJAAN -> "Sedang dikerjakan"
                                    BookingStatus.SELESAI -> "Selesai"
                                    else -> "Menunggu mitra"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when (currentStatus) {
                                BookingStatus.PENDING -> "Menunggu Mitra Menerima Pesanan"
                                BookingStatus.DIKONFIRMASI -> "Mitra Menuju Lokasi Anda"
                                BookingStatus.DALAM_PENGERJAAN -> "Teknisi Sedang Mengerjakan Servis"
                                BookingStatus.SELESAI -> "Layanan Telah Selesai Dikerjakan"
                                BookingStatus.DITOLAK -> "Pesanan Ditolak Mitra"
                                BookingStatus.KEDALUWARSA -> "Pesanan Kedaluwarsa"
                                BookingStatus.DIBATALKAN_PELANGGAN -> "Pesanan Dibatalkan Oleh Anda"
                                BookingStatus.DIBATALKAN_MITRA -> "Pesanan Dibatalkan Mitra"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = when (currentStatus) {
                                BookingStatus.PENDING -> "Mitra belum menerima pesanan. Status akan berubah setelah mitra menekan Terima."
                                BookingStatus.DIKONFIRMASI -> "Pesanan sudah diterima mitra. Teknisi sedang menuju alamat Anda, belum mulai mengerjakan."
                                BookingStatus.DALAM_PENGERJAAN -> "Teknisi telah tiba dan sedang mengerjakan servis. Pembayaran tunai setelah pekerjaan diperiksa."
                                BookingStatus.SELESAI -> "Pekerjaan selesai. Silakan beri ulasan untuk membantu mitra lain."
                                else -> "Pesanan tidak dilanjutkan. Silakan buat pesanan baru bila masih membutuhkan jasa."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 2. Peta Posisi Teknisi (OSMDroid)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    OsmMapView(
                        centerPoint = FakeData.MALANG_CENTER,
                        markerTitle = when (currentStatus) {
                            BookingStatus.PENDING -> "Alamat Tujuan"
                            BookingStatus.DIKONFIRMASI -> "Mitra Menuju Lokasi"
                            else -> "Lokasi Pengerjaan"
                        },
                        zoomLevel = 15.0
                    )
                }
            }

            // 3. Kontak Teknisi
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
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
                                    .background(OrangeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isMitraAssigned) {
                                        booking.providerName.take(1).uppercase()
                                    } else {
                                        "?"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = OrangePrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isMitraAssigned) booking.providerName else "Mitra belum ditampilkan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = when {
                                        !isMitraAssigned -> "Mitra akan muncul setelah menerima pesanan"
                                        currentStatus == BookingStatus.DIKONFIRMASI -> "Sedang menuju lokasi Anda"
                                        else -> "Mitra tiba di lokasi pengerjaan"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {},
                                enabled = isMitraAssigned,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0EAE1)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hubungi", color = TextPrimary)
                            }

                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeContainer.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pesan Chat", color = OrangePrimary)
                            }
                        }
                    }
                }
            }

            // 4. Garis Waktu Proses Pengerjaan (Timeline)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Proses Pengerjaan",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "ID #${bookingId.take(8).uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        buildTrackingSteps(
                            status = currentStatus,
                            reviewSubmitted = isReviewSubmitted,
                            scheduledTime = booking.scheduledTime
                        ).forEachIndexed { index, step ->
                            TimelineStepItem(
                                step = step,
                                isLast = index == 3
                            )
                        }
                    }
                }
            }

            // 5. Aksi pelanggan: hanya membatalkan sebelum pengerjaan dimulai.
            // Menandai selesai adalah hak mitra (PRD 6.1), jadi tidak ditampilkan di sini.
            if (currentStatus.canCustomerCancel) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.updateStatus(BookingStatus.DIBATALKAN_PELANGGAN) },
                        enabled = !uiState.isUpdatingStatus,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Batalkan Pesanan", color = RedError)
                    }
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = RedError,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // 6. Formulir Ulasan (Muncul Otomatis saat Status SELESAI - CRUD-4)
            if (currentStatus == BookingStatus.SELESAI) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GreenSuccess.copy(alpha = 0.5f)))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(GreenSuccessContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GreenSuccess,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Layanan Telah Selesai!",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Bagaimana pengalaman pengerjaan bersama teknisi?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // 5 Bintang Interaktif
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..5).forEach { starIndex ->
                                    Icon(
                                        imageVector = if (starIndex <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Bintang $starIndex",
                                        tint = if (starIndex <= rating) Color(0xFFF59E0B) else TextMuted,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { viewModel.setRating(starIndex) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (rating) {
                                    5 -> "5.0 Luar Biasa Memuaskan!"
                                    4 -> "4.0 Sangat Baik"
                                    3 -> "3.0 Cukup Baik"
                                    else -> "$rating.0 Perlu Ditingkatkan"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Tag Apresiasi Cepat
                            val allTags = listOf("Tepat Waktu", "Pekerjaan Rapi", "Sopan & Ramah", "Harga Sesuai")
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allTags.forEach { tag ->
                                    val isSelected = selectedTags.contains(tag)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) OrangeContainer else Color(0xFFF0EAE1))
                                            .clickable {
                                                viewModel.toggleTag(tag)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) OrangePrimary else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = reviewComment,
                                onValueChange = {
                                    viewModel.setReviewComment(it)
                                },
                                label = { Text("Catatan Ulasan Anda") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    viewModel.submitReview()
                                },
                                enabled = !isReviewSubmitted && !uiState.isSubmittingReview,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrangePrimary,
                                    disabledContainerColor = GreenSuccess
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(if (isReviewSubmitted) "Ulasan Terkirim" else "Kirim Ulasan & Selesai")
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class TrackingStepState { DONE, ACTIVE, UPCOMING, STOPPED }

private data class TrackingStepUi(
    val number: String,
    val title: String,
    val description: String,
    val timeText: String,
    val state: TrackingStepState
)

/**
 * Level progres 0..4. Status terminal yang dibatalkan berhenti di level
 * yang sudah tercapai, sehingga tidak menampilkan "mitra menuju lokasi"
 * saat pesanan masih PENDING / baru diterima.
 */
private fun trackingProgressLevel(status: BookingStatus, reviewSubmitted: Boolean): Int = when (status) {
    BookingStatus.PENDING -> 0
    BookingStatus.DIKONFIRMASI,
    BookingStatus.DIBATALKAN_PELANGGAN,
    BookingStatus.DIBATALKAN_MITRA -> 1
    BookingStatus.DALAM_PENGERJAAN -> 2
    BookingStatus.SELESAI -> if (reviewSubmitted) 4 else 3
    BookingStatus.DITOLAK, BookingStatus.KEDALUWARSA -> 0
}

private fun buildTrackingSteps(
    status: BookingStatus,
    reviewSubmitted: Boolean,
    scheduledTime: String
): List<TrackingStepUi> {
    val level = trackingProgressLevel(status, reviewSubmitted)
    val isStopped = status.isCancelled
    val titles = listOf(
        "Pesanan Dibuat & Menunggu Mitra",
        "Mitra Menerima & Menuju Lokasi",
        "Pengerjaan Servis",
        "Konfirmasi Selesai & Ulasan"
    )
    val descriptions = listOf(
        "Sistem meneruskan pesanan ke mitra terdekat",
        "Pesanan diterima, teknisi sedang dalam perjalanan",
        "Teknisi tiba dan mengerjakan layanan sesuai pesanan",
        "Periksa hasil kerja dan berikan ulasan"
    )
    val times = listOf(
        scheduledTime,
        if (level >= 2) "Diterima" else "Menunggu",
        if (level >= 3) "Dikerjakan" else "Menunggu",
        if (reviewSubmitted) "Selesai" else "Menunggu"
    )

    return titles.mapIndexed { index, title ->
        val stepNumber = index + 1
        val state = when {
            stepNumber <= level -> TrackingStepState.DONE
            isStopped -> TrackingStepState.STOPPED
            stepNumber == level + 1 -> TrackingStepState.ACTIVE
            else -> TrackingStepState.UPCOMING
        }
        TrackingStepUi(
            number = "$stepNumber",
            title = title,
            description = descriptions[index],
            timeText = times[index],
            state = state
        )
    }
}

@Composable
private fun TimelineStepItem(
    step: TrackingStepUi,
    isLast: Boolean = false
) {
    val (circleColor, connectorColor) = when (step.state) {
        TrackingStepState.DONE -> GreenSuccess to GreenSuccess
        TrackingStepState.ACTIVE -> OrangePrimary to OrangePrimary
        TrackingStepState.STOPPED -> RedError to Color(0xFFE0E0E0)
        TrackingStepState.UPCOMING -> Color(0xFFE0E0E0) to Color(0xFFE0E0E0)
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(circleColor),
                contentAlignment = Alignment.Center
            ) {
                when (step.state) {
                    TrackingStepState.DONE -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    TrackingStepState.STOPPED -> Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    else -> Text(
                        text = step.number,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(connectorColor)
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
                    text = step.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    text = step.timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
