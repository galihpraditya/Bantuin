package id.bantuin.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.ChatThreadSummary
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.EmptyView
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme

@Composable
fun ChatListScreen(
    repository: BantuinRepository,
    onThreadClick: (String) -> Unit,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    viewModel: ChatListViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    Scaffold(
        topBar = {
            BantuinTopAppBar(
                title = "Chat Pesanan",
                canNavigateBack = canNavigateBack,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.threads.isNotEmpty() || uiState.searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = {
                        Text(
                            text = "Cari nama atau layanan...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari percakapan",
                            tint = colors.textMuted
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onSearchQueryChange("") },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Hapus pencarian",
                                    tint = colors.textMuted
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.borderSubtle
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            when {
                uiState.isLoading && uiState.threads.isEmpty() -> {
                    LoadingView(message = "Memuat percakapan pesanan...")
                }
                uiState.errorMessage != null && uiState.threads.isEmpty() -> {
                    ErrorView(
                        message = uiState.errorMessage ?: "Gagal memuat percakapan.",
                        onRetry = { viewModel.observeThreads() }
                    )
                }
                uiState.filteredThreads.isEmpty() -> {
                    EmptyView(
                        title = if (uiState.searchQuery.isNotBlank()) {
                            "Percakapan Tidak Ditemukan"
                        } else {
                            "Belum Ada Percakapan"
                        },
                        message = if (uiState.searchQuery.isNotBlank()) {
                            "Tidak ada obrolan yang cocok dengan kata kunci tersebut."
                        } else {
                            "Obrolan dengan mitra atau pelanggan akan otomatis muncul di sini saat pesanan aktif."
                        }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.filteredThreads, key = { it.bookingId }) { thread ->
                            ChatThreadCard(
                                thread = thread,
                                onClick = { onThreadClick(thread.bookingId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatThreadCard(
    thread: ChatThreadSummary,
    onClick: () -> Unit
) {
    val colors = BantuinTheme.colors
    val statusBg = when (thread.bookingStatus) {
        BookingStatus.SELESAI -> colors.successContainer
        BookingStatus.DITOLAK, BookingStatus.KEDALUWARSA,
        BookingStatus.DIBATALKAN_PELANGGAN, BookingStatus.DIBATALKAN_MITRA -> colors.errorContainer
        else -> colors.infoContainer
    }
    val statusFg = when (thread.bookingStatus) {
        BookingStatus.SELESAI -> colors.success
        BookingStatus.DITOLAK, BookingStatus.KEDALUWARSA,
        BookingStatus.DIBATALKAN_PELANGGAN, BookingStatus.DIBATALKAN_MITRA -> colors.error
        else -> colors.info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = thread.counterpartyName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.counterpartyName,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = TimeUtils.formatInstantToTime(thread.lastMessageAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BadgeContainer(
                        text = thread.bookingStatus.displayName,
                        color = statusFg,
                        backgroundColor = statusBg
                    )
                    Text(
                        text = thread.serviceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.lastMessageText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (thread.unreadCount > 0) colors.textPrimary else colors.textSecondary,
                        fontWeight = if (thread.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (thread.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        )
                    }
                }
            }
        }
    }
}
