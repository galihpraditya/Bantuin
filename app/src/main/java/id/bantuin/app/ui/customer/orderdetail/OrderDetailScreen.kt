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
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.ui.common.BantuinViewModelFactory
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
    repository: FakeBantuinRepository,
    onNavigateBack: () -> Unit,
    viewModel: OrderDetailViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(bookingId) {
        viewModel.observeBooking(bookingId)
    }

    val booking = uiState.booking
    val currentStatus = booking?.status ?: BookingStatus.DALAM_PENGERJAAN

    // Review form states
    var rating = uiState.rating
    var reviewComment = uiState.reviewComment
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
                                text = "Estimasi 12 Menit",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
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
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Teknisi siaga di lokasi Anda. Pembayaran tunai dilakukan setelah pekerjaan diperiksa.",
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
                        markerTitle = "Teknisi Siaga",
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
                                    text = "B",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = OrangePrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = booking?.providerName ?: "Budi Santoso, S.T.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Kendaraan: N 4821 XY • Honda Vario Hitam",
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

                        TimelineStepItem(
                            stepNumber = "1",
                            title = "Pesanan Dikonfirmasi",
                            description = "Sistem menugaskan mitra terdekat",
                            timeText = "14:30 WIB",
                            isCompleted = true
                        )
                        TimelineStepItem(
                            stepNumber = "2",
                            title = "Mitra Menuju Lokasi",
                            description = "Sedang berkendara menuju alamat Anda",
                            timeText = "14:38 WIB",
                            isCompleted = currentStatus in listOf(BookingStatus.DALAM_PENGERJAAN, BookingStatus.SELESAI)
                        )
                        TimelineStepItem(
                            stepNumber = "3",
                            title = "Pengerjaan Servis AC",
                            description = "Pembersihan unit, cuci filter & cek tekanan",
                            timeText = "14:50 WIB",
                            isCompleted = currentStatus == BookingStatus.SELESAI
                        )
                        TimelineStepItem(
                            stepNumber = "4",
                            title = "Konfirmasi Selesai & Pembayaran",
                            description = "Pemeriksaan hasil kerja dan ulasan layanan",
                            timeText = if (currentStatus == BookingStatus.SELESAI) "Selesai" else "Menunggu",
                            isCompleted = currentStatus == BookingStatus.SELESAI,
                            isLast = true
                        )
                    }
                }
            }

            // 5. Tombol Simulasi Alur Pesanan (Demo Helper)
            if (currentStatus != BookingStatus.SELESAI) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.updateStatus(bookingId, BookingStatus.SELESAI)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tandai Selesai")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository.updateStatus(bookingId, BookingStatus.DIBATALKAN_PELANGGAN)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batalkan Pesanan", color = RedError)
                        }
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
                                            .clickable { rating = starIndex }
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
                                                if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
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
                                    reviewComment = it
                                    viewModel.setReviewComment(it)
                                },
                                label = { Text("Catatan Ulasan Anda") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    viewModel.setRating(rating)
                                    viewModel.setReviewComment(reviewComment)
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

@Composable
private fun TimelineStepItem(
    stepNumber: String,
    title: String,
    description: String,
    timeText: String,
    isCompleted: Boolean,
    isLast: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) GreenSuccess else Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        text = stepNumber,
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
                        .background(if (isCompleted) GreenSuccess else Color(0xFFE0E0E0))
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
                    color = TextPrimary
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
