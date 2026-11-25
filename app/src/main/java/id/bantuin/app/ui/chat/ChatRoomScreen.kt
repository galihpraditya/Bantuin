package id.bantuin.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.ChatMessage
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.EmptyView
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    bookingId: String,
    repository: BantuinRepository,
    onNavigateBack: () -> Unit,
    viewModel: ChatRoomViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors
    val listState = rememberLazyListState()

    LaunchedEffect(bookingId) {
        viewModel.initialize(bookingId)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    val booking = uiState.booking
    val counterpartyName = if (booking == null) {
        "Chat Pesanan"
    } else if (uiState.activeRole == "provider") {
        booking.customerName
    } else {
        booking.providerName
    }

    val quickReplies = if (uiState.activeRole == "provider") {
        listOf(
            "Saya segera berangkat ke lokasi",
            "Sudah tiba di lokasi",
            "Pekerjaan sedang berlangsung"
        )
    } else {
        listOf(
            "Sudah di jalan, Pak?",
            "Patokan rumah pagar hijau",
            "Mohon konfirmasi jadwal ya"
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(colors.surface)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = counterpartyName,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (booking != null) {
                                val paymentLabel = when {
                                    booking.paymentMethod == PaymentMethod.QRIS && booking.paymentStatus == PaymentStatus.PAID -> "QRIS • Lunas"
                                    booking.paymentMethod == PaymentMethod.QRIS -> "QRIS • Menunggu Bayar"
                                    else -> "Tunai • Bayar di Lokasi"
                                }
                                Text(
                                    text = "${booking.serviceName} • $paymentLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    actions = {
                        if (booking != null) {
                            Box(modifier = Modifier.padding(end = 12.dp)) {
                                BadgeContainer(
                                    text = booking.status.displayName,
                                    color = colors.info,
                                    backgroundColor = colors.infoContainer
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
                )
                HorizontalDivider(color = colors.borderSubtle)
            }
        },
        bottomBar = {
            Surface(
                color = colors.surface,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = colors.borderSubtle)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickReplies.forEach { reply ->
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(colors.surfaceSubtle)
                                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(20.dp))
                                    .clickable(enabled = !uiState.isSending) {
                                        viewModel.sendQuickReply(reply)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = reply,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 8.dp, bottom = 10.dp, top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.messageInput,
                            onValueChange = { viewModel.onMessageInputChange(it) },
                            placeholder = {
                                Text(
                                    text = "Tulis pesan untuk ${if (uiState.activeRole == "provider") "pelanggan" else "mitra"}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textMuted
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.background,
                                unfocusedContainerColor = colors.background,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.borderSubtle
                            ),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            enabled = !uiState.isSending && uiState.messageInput.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.messageInput.isNotBlank() && !uiState.isSending) {
                                        colors.primary
                                    } else {
                                        colors.surfaceSubtle
                                    }
                                )
                        ) {
                            if (uiState.isSending) {
                                CircularProgressIndicator(
                                    color = colors.primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Kirim pesan",
                                    tint = if (uiState.messageInput.isNotBlank()) colors.onPrimary else colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.messages.isEmpty() -> {
                LoadingView(
                    message = "Memuat riwayat pesan...",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            uiState.errorMessage != null && uiState.messages.isEmpty() && booking == null -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat ruang chat.",
                    onRetry = { viewModel.initialize(bookingId) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            uiState.messages.isEmpty() -> {
                EmptyView(
                    title = "Mulai Percakapan",
                    message = "Kirim pesan atau pilih balasan cepat di bawah untuk berkoordinasi langsung.",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { msg ->
                        val isMine = msg.senderId == uiState.currentUserId ||
                            msg.senderRole == uiState.activeRole
                        ChatBubbleItem(message = msg, isMine = isMine)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    isMine: Boolean
) {
    val colors = BantuinTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 290.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMine) colors.primary else colors.surface
            ),
            border = if (isMine) {
                null
            } else {
                CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
            }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isMine) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine) colors.onPrimary else colors.textPrimary
                )
                Text(
                    text = TimeUtils.formatInstantToTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isMine) colors.onPrimary.copy(alpha = 0.8f) else colors.textMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
