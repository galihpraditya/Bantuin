package id.bantuin.app.ui.provider.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.domain.model.PayoutChannelType
import id.bantuin.app.domain.model.PayoutRequest
import id.bantuin.app.domain.model.ProviderRevenueAnalytics
import id.bantuin.app.domain.model.WalletTransaction
import id.bantuin.app.domain.model.WalletTransactionType
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.WalletRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WalletHistoryFilter(val label: String) {
    ALL("Semua"),
    CREDIT("Saldo Masuk (+)"),
    DEBIT("Penarikan (-)")
}

private data class WalletSnapshot(
    val providerId: String,
    val analytics: ProviderRevenueAnalytics,
    val payouts: List<PayoutRequest>,
    val transactions: List<WalletTransaction>
)

data class ProviderWithdrawUiState(
    val providerId: String = "",
    val availableBalance: Int = 0,
    val revenueAnalytics: ProviderRevenueAnalytics = ProviderRevenueAnalytics(),
    val payouts: List<PayoutRequest> = emptyList(),
    val walletTransactions: List<WalletTransaction> = emptyList(),
    val historyFilter: WalletHistoryFilter = WalletHistoryFilter.ALL,
    val channelType: PayoutChannelType = PayoutChannelType.BANK,
    val channelName: String = "BCA",
    val accountNumber: String = "",
    val accountHolder: String = "",
    val amountInput: String = "",
    val adminFee: Int = BantuinConfig.PAYOUT_ADMIN_FEE,
    val minPayoutAmount: Int = BantuinConfig.MIN_PAYOUT_AMOUNT,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
) {
    val parsedAmount: Int
        get() = amountInput.filter { it.isDigit() }.toIntOrNull() ?: 0

    val estimatedNetAmount: Int
        get() = (parsedAmount - adminFee).coerceAtLeast(0)

    val filteredTransactions: List<WalletTransaction>
        get() = when (historyFilter) {
            WalletHistoryFilter.ALL -> walletTransactions
            WalletHistoryFilter.CREDIT -> walletTransactions.filter { it.type == WalletTransactionType.CREDIT }
            WalletHistoryFilter.DEBIT -> walletTransactions.filter { it.type == WalletTransactionType.DEBIT }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProviderWithdrawViewModel(
    private val walletRepository: WalletRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderWithdrawUiState())
    val uiState: StateFlow<ProviderWithdrawUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeWallet()
    }

    fun observeWallet() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        observeJob = viewModelScope.launch {
            authRepository.currentUser()
                .flatMapLatest { user ->
                    val providerId = user?.id.orEmpty()
                    if (providerId.isBlank()) {
                        flowOf(
                            WalletSnapshot(
                                providerId = "",
                                analytics = ProviderRevenueAnalytics(),
                                payouts = emptyList(),
                                transactions = emptyList()
                            )
                        )
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                providerId = providerId,
                                accountHolder = if (state.accountHolder.isBlank()) user?.fullName.orEmpty() else state.accountHolder
                            )
                        }
                        combine(
                            walletRepository.observeProviderRevenue(providerId),
                            walletRepository.observePayoutHistory(providerId),
                            walletRepository.observeWalletTransactions(providerId)
                        ) { analytics, payouts, transactions ->
                            WalletSnapshot(
                                providerId = providerId,
                                analytics = analytics,
                                payouts = payouts,
                                transactions = transactions
                            )
                        }.catch { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = e.message ?: "Gagal memuat data saldo."
                                )
                            }
                        }
                    }
                }
                .collect { snapshot ->
                    _uiState.update { state ->
                        state.copy(
                            providerId = snapshot.providerId,
                            availableBalance = snapshot.analytics.availableBalance,
                            revenueAnalytics = snapshot.analytics,
                            payouts = snapshot.payouts,
                            walletTransactions = snapshot.transactions,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun setHistoryFilter(filter: WalletHistoryFilter) {
        _uiState.update { it.copy(historyFilter = filter) }
    }

    fun selectChannel(type: PayoutChannelType, name: String) {
        _uiState.update {
            it.copy(
                channelType = type,
                channelName = name,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onAccountNumberChange(value: String) {
        _uiState.update {
            it.copy(
                accountNumber = value.filter { ch -> ch.isDigit() }.take(24),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onAccountHolderChange(value: String) {
        _uiState.update {
            it.copy(
                accountHolder = value.take(80),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onAmountInputChange(value: String) {
        _uiState.update {
            it.copy(
                amountInput = value.filter { ch -> ch.isDigit() }.take(10),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun applyPresetAmount(amount: Int) {
        val clamped = amount.coerceAtLeast(0)
        _uiState.update {
            it.copy(
                amountInput = if (clamped > 0) clamped.toString() else "",
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun submitPayout() {
        val state = _uiState.value
        val providerId = state.providerId
        if (providerId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Sesi mitra tidak ditemukan.") }
            return
        }
        val amount = state.parsedAmount
        if (amount < BantuinConfig.MIN_PAYOUT_AMOUNT) {
            _uiState.update {
                it.copy(errorMessage = "Minimal penarikan dana adalah Rp20.000.")
            }
            return
        }
        if (amount > state.availableBalance) {
            _uiState.update {
                it.copy(errorMessage = "Saldo siap cair tidak mencukupi.")
            }
            return
        }
        if (state.channelName.isBlank() || state.accountNumber.isBlank() || state.accountHolder.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Lengkapi nomor rekening/HP dan nama pemilik akun.")
            }
            return
        }
        if (state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            when (
                val res = walletRepository.requestPayout(
                    providerId = providerId,
                    amount = amount,
                    channelType = state.channelType,
                    channelName = state.channelName,
                    accountNumber = state.accountNumber.trim(),
                    accountHolder = state.accountHolder.trim()
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            amountInput = "",
                            successMessage = "Pencairan ${CurrencyUtils.formatRupiah(res.data.amount)} ke ${res.data.channelName} berhasil diproses.",
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = res.message
                        )
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
