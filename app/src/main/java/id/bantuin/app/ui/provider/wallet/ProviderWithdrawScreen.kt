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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.domain.model.PayoutChannelType
import id.bantuin.app.domain.model.PayoutRequest
import id.bantuin.app.domain.model.PayoutStatus
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.EmptyView
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

    val channelOptions = listOf(
        PayoutChannelType.BANK to "BCA",
        PayoutChannelType.BANK to "BRI",
        PayoutChannelType.BANK to "Mandiri",
        PayoutChannelType.EWALLET to "DANA",
        PayoutChannelType.EWALLET to "GoPay",
        PayoutChannelType.EWALLET to "OVO"
    )

    Scaffold(
        topBar = {
            BantuinTopAppBar(
                title = "Tarik Saldo Mitra",
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Saldo Siap Cair
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Saldo Siap Cair (Dari QRIS)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = CurrencyUtils.formatRupiah(uiState.availableBalance),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Minimal penarikan ${CurrencyUtils.formatRupiah(uiState.minPayoutAmount)} • Biaya admin ${CurrencyUtils.formatRupiah(uiState.adminFee)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }

                    // 2. Formulir Penarikan
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
                                    text = "Tujuan Pencairan",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    channelOptions.forEach { (type, name) ->
                                        val selected = uiState.channelName == name && uiState.channelType == type
                                        Box(
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selected) colors.primaryContainer else colors.surfaceSubtle)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (selected) colors.primary else colors.borderSubtle,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { viewModel.selectChannel(type, name) }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) colors.onPrimaryContainer else colors.textPrimary,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
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
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.surfaceSubtle)
                                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                                                .clickable(enabled = uiState.availableBalance > 0) {
                                                    viewModel.applyPresetAmount(preset)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colors.textPrimary,
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
                                        label = "Dana Diterima",
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
                                        text = if (uiState.isSubmitting) "Memproses..." else "Ajukan Pencairan Dana",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 3. Riwayat Pencairan
                    item {
                        Text(
                            text = "Riwayat Pencairan",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.payouts.isEmpty()) {
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
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = "Belum Ada Penarikan Saldo",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Riwayat penarikan dana QRIS ke bank atau e-wallet akan tampil di sini.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.payouts, key = { it.id }) { payout ->
                            PayoutHistoryCard(payout = payout)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryAmountRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
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
private fun PayoutHistoryCard(payout: PayoutRequest) {
    val colors = BantuinTheme.colors
    val statusFg = when (payout.status) {
        PayoutStatus.COMPLETED -> colors.success
        PayoutStatus.FAILED -> colors.error
        else -> colors.warning
    }
    val statusBg = when (payout.status) {
        PayoutStatus.COMPLETED -> colors.successContainer
        PayoutStatus.FAILED -> colors.errorContainer
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${payout.channelName} • ${payout.accountNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "a.n. ${payout.accountHolder} • ${TimeUtils.formatInstantToDate(payout.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Text(
                    text = "Diterima: ${CurrencyUtils.formatRupiah(payout.netAmount)} (Admin ${CurrencyUtils.formatRupiah(payout.adminFee)})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = colors.textMuted
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = CurrencyUtils.formatRupiah(payout.amount),
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
                BadgeContainer(
                    text = payout.status.label,
                    color = statusFg,
                    backgroundColor = statusBg
                )
            }
        }
    }
}
