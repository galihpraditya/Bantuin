package id.bantuin.app.ui.provider.wallet

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.PayoutChannelType
import id.bantuin.app.domain.model.PayoutStatus
import id.bantuin.app.domain.model.WalletTransaction
import id.bantuin.app.domain.model.WalletTransactionType
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.ErrorView
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.theme.BantuinTheme

@Composable
fun ProviderWithdrawScreen(
    repository: BantuinRepository,
    onNavigateBack: () -> Unit,
    viewModel: ProviderWithdrawViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    val bankChannels = listOf("BCA", "BRI", "Mandiri")
    val ewalletChannels = listOf("DANA", "GoPay", "OVO")

    Scaffold(
        topBar = {
            BantuinTopAppBar(
                title = "Dompet & Penarikan Saldo",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.providerId.isBlank() -> {
                LoadingView(
                    message = "Memuat informasi saldo mitra...",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            uiState.errorMessage != null && uiState.providerId.isBlank() -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Gagal memuat saldo mitra.",
                    onRetry = { viewModel.observeWallet() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                val filteredTransactions = uiState.filteredTransactions

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Kartu Ringkasan Saldo Siap Cair
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = colors.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = "Saldo Siap Cair",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    BadgeContainer(
                                        text = "Sumber: QRIS",
                                        color = colors.onPrimaryContainer,
                                        backgroundColor = colors.primaryContainer
                                    )
                                }

                                Text(
                                    text = CurrencyUtils.formatRupiah(uiState.availableBalance),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Min. tarik ${CurrencyUtils.formatRupiah(uiState.minPayoutAmount)} • Admin ${CurrencyUtils.formatRupiah(uiState.adminFee)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )

                                HorizontalDivider(color = colors.borderSubtle)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.surfaceSubtle)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Total Masuk QRIS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textMuted
                                        )
                                        Text(
                                            text = CurrencyUtils.formatRupiah(uiState.revenueAnalytics.qrisGrossRevenue),
                                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                            color = colors.success,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.surfaceSubtle)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Sudah Dicairkan",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textMuted
                                        )
                                        Text(
                                            text = CurrencyUtils.formatRupiah(uiState.revenueAnalytics.totalWithdrawn),
                                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Formulir Penarikan dengan Grid Logo Bank & E-Wallet
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Pilih Tujuan Pencairan",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )

                                // Grup 1: Transfer Bank
                                Text(
                                    text = "Transfer Bank",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    bankChannels.forEach { name ->
                                        val selected = uiState.channelName == name && uiState.channelType == PayoutChannelType.BANK
                                        ChannelSelectionTile(
                                            name = name,
                                            selected = selected,
                                            onClick = { viewModel.selectChannel(PayoutChannelType.BANK, name) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Grup 2: E-Wallet
                                Text(
                                    text = "E-Wallet",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ewalletChannels.forEach { name ->
                                        val selected = uiState.channelName == name && uiState.channelType == PayoutChannelType.EWALLET
                                        ChannelSelectionTile(
                                            name = name,
                                            selected = selected,
                                            onClick = { viewModel.selectChannel(PayoutChannelType.EWALLET, name) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Banner tujuan terpilih
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surfaceSubtle)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PayoutChannelLogo(channelName = uiState.channelName, size = 28.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Tujuan: ${uiState.channelType.label} ${uiState.channelName}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (uiState.channelType == PayoutChannelType.BANK) {
                                                "Masukkan nomor rekening bank aktif"
                                            } else {
                                                "Masukkan nomor HP terdaftar di ${uiState.channelName}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = uiState.accountNumber,
                                    onValueChange = { viewModel.onAccountNumberChange(it) },
                                    label = {
                                        Text(
                                            if (uiState.channelType == PayoutChannelType.BANK) {
                                                "Nomor Rekening ${uiState.channelName}"
                                            } else {
                                                "Nomor HP ${uiState.channelName}"
                                            }
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.background,
                                        unfocusedContainerColor = colors.background,
                                        focusedBorderColor = colors.primary,
                                        unfocusedBorderColor = colors.borderSubtle
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = uiState.accountHolder,
                                    onValueChange = { viewModel.onAccountHolderChange(it) },
                                    label = { Text("Nama Pemilik Rekening / Akun") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.background,
                                        unfocusedContainerColor = colors.background,
                                        focusedBorderColor = colors.primary,
                                        unfocusedBorderColor = colors.borderSubtle
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = uiState.amountInput,
                                    onValueChange = { viewModel.onAmountInputChange(it) },
                                    label = { Text("Nominal Penarikan (Rp)") },
                                    placeholder = { Text("Minimal 20000") },
                                    supportingText = if (uiState.parsedAmount > 0) {
                                        {
                                            Text(
                                                text = "Nominal: ${CurrencyUtils.formatRupiah(uiState.parsedAmount)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                color = colors.primary
                                            )
                                        }
                                    } else null,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colors.background,
                                        unfocusedContainerColor = colors.background,
                                        focusedBorderColor = colors.primary,
                                        unfocusedBorderColor = colors.borderSubtle
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "Rp50.000" to 50_000,
                                        "Rp100.000" to 100_000,
                                        "Semua" to uiState.availableBalance
                                    ).forEach { (label, preset) ->
                                        val isPresetActive = preset > 0 && uiState.parsedAmount == preset
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isPresetActive) colors.primaryContainer else colors.surfaceSubtle)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isPresetActive) colors.primary else colors.borderSubtle,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable(enabled = uiState.availableBalance > 0) {
                                                    viewModel.applyPresetAmount(preset)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isPresetActive) colors.onPrimaryContainer else colors.textPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = colors.borderSubtle)

                                // Rincian transparan
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SummaryAmountRow(
                                        label = "Nominal Penarikan",
                                        value = CurrencyUtils.formatRupiah(uiState.parsedAmount),
                                        valueColor = colors.textPrimary
                                    )
                                    SummaryAmountRow(
                                        label = "Biaya Admin",
                                        value = CurrencyUtils.formatRupiah(uiState.adminFee),
                                        valueColor = colors.textSecondary
                                    )
                                    SummaryAmountRow(
                                        label = "Dana Bersih Diterima",
                                        value = CurrencyUtils.formatRupiah(uiState.estimatedNetAmount),
                                        valueColor = colors.success,
                                        bold = true
                                    )
                                }

                                if (!uiState.errorMessage.isNullOrBlank()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
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

                                if (!uiState.successMessage.isNullOrBlank()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = colors.successContainer)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = colors.success,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = uiState.successMessage ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.success
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { viewModel.submitPayout() },
                                    enabled = !uiState.isSubmitting && uiState.availableBalance >= uiState.minPayoutAmount,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    if (uiState.isSubmitting) {
                                        CircularProgressIndicator(
                                            color = colors.onPrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = if (uiState.isSubmitting) "Memproses..." else "Ajukan Pencairan ke ${uiState.channelName}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 3. Riwayat Transaksi Saldo (Penambahan & Pengurangan)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Riwayat Transaksi Saldo",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${filteredTransactions.size} transaksi",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WalletHistoryFilter.entries.forEach { filter ->
                                    FilterChip(
                                        selected = uiState.historyFilter == filter,
                                        onClick = { viewModel.setHistoryFilter(filter) },
                                        label = { Text(filter.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.primaryContainer,
                                            selectedLabelColor = colors.onPrimaryContainer
                                        ),
                                        modifier = Modifier.heightIn(min = 40.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (filteredTransactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = when (uiState.historyFilter) {
                                            WalletHistoryFilter.ALL -> "Belum Ada Transaksi Saldo"
                                            WalletHistoryFilter.CREDIT -> "Belum Ada Dana Masuk QRIS"
                                            WalletHistoryFilter.DEBIT -> "Belum Ada Penarikan Saldo"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Setiap dana masuk dari pesanan QRIS selesai maupun penarikan ke rekening akan tercatat otomatis di sini.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredTransactions, key = { it.id }) { tx ->
                            WalletTransactionCard(transaction = tx)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelSelectionTile(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.primaryContainer.copy(alpha = 0.5f) else colors.surfaceSubtle)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.primary else colors.borderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PayoutChannelLogo(channelName = name, size = 36.dp)
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.onPrimaryContainer else colors.textPrimary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Terpilih",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryAmountRow(
    label: String,
    value: String,
    valueColor: Color,
    bold: Boolean = false
) {
    val colors = BantuinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun WalletTransactionCard(transaction: WalletTransaction) {
    val colors = BantuinTheme.colors
    val isCredit = transaction.type == WalletTransactionType.CREDIT

    val amountPrefix = if (isCredit) "+" else "-"
    val amountColor = if (isCredit) colors.success else colors.error

    val statusFg = when {
        isCredit -> colors.success
        transaction.payoutStatus == PayoutStatus.COMPLETED -> colors.success
        transaction.payoutStatus == PayoutStatus.FAILED -> colors.error
        else -> colors.warning
    }
    val statusBg = when {
        isCredit -> colors.successContainer
        transaction.payoutStatus == PayoutStatus.COMPLETED -> colors.successContainer
        transaction.payoutStatus == PayoutStatus.FAILED -> colors.errorContainer
        else -> colors.warningContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            if (isCredit) {
                WalletCreditLogo(size = 40.dp)
            } else {
                PayoutChannelLogo(
                    channelName = transaction.channelName ?: "BANK",
                    size = 40.dp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transaction.subtitle} • ${TimeUtils.formatInstantToDate(transaction.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isCredit) {
                    Text(
                        text = "Diterima: ${CurrencyUtils.formatRupiah(transaction.netAmount)} (Admin ${CurrencyUtils.formatRupiah(transaction.adminFee)})",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum", fontSize = 11.sp),
                        color = colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$amountPrefix${CurrencyUtils.formatRupiah(transaction.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                BadgeContainer(
                    text = transaction.statusLabel,
                    color = statusFg,
                    backgroundColor = statusBg
                )
            }
        }
    }
}
