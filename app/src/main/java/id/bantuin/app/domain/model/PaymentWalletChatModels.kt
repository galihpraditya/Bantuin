package id.bantuin.app.domain.model

import kotlinx.datetime.Instant

enum class PaymentMethod(val label: String, val shortLabel: String) {
    QRIS("QRIS", "QRIS"),
    CASH("Tunai di Tempat (Cash)", "Tunai")
}

enum class PaymentStatus(val label: String) {
    UNPAID("Bayar di Lokasi"),
    WAITING_PAYMENT("Menunggu Bayar"),
    PAID("Lunas"),
    REFUNDED("Dikembalikan")
}

data class ChatMessage(
    val id: String,
    val bookingId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val text: String,
    val createdAt: Instant
)

data class ChatThreadSummary(
    val bookingId: String,
    val counterpartyId: String,
    val counterpartyName: String,
    val serviceName: String,
    val bookingStatus: BookingStatus,
    val lastMessageText: String,
    val lastMessageAt: Instant,
    val unreadCount: Int = 0
)

enum class PayoutChannelType(val label: String) {
    BANK("Transfer Bank"),
    EWALLET("E-Wallet")
}

enum class PayoutStatus(val label: String) {
    PENDING("Menunggu"),
    PROCESSING("Diproses"),
    COMPLETED("Berhasil"),
    FAILED("Gagal")
}

data class PayoutRequest(
    val id: String,
    val providerId: String,
    val amount: Int,
    val adminFee: Int,
    val netAmount: Int,
    val channelType: PayoutChannelType,
    val channelName: String,
    val accountNumber: String,
    val accountHolder: String,
    val status: PayoutStatus,
    val createdAt: Instant
)

data class ProviderRevenueAnalytics(
    val totalCombinedRevenue: Int = 0,
    val qrisGrossRevenue: Int = 0,
    val cashRevenue: Int = 0,
    val availableBalance: Int = 0,
    val totalWithdrawn: Int = 0,
    val pendingWithdrawal: Int = 0,
    val completedQrisCount: Int = 0,
    val completedCashCount: Int = 0
)
