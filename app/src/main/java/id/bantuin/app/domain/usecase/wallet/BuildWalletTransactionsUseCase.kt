package id.bantuin.app.domain.usecase.wallet

import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.model.PayoutRequest
import id.bantuin.app.domain.model.WalletTransaction
import id.bantuin.app.domain.model.WalletTransactionType

/**
 * Menggabungkan mutasi saldo Mitra:
 * - CREDIT: pesanan SELESAI dengan QRIS PAID (penambahan saldo siap cair)
 * - DEBIT: pengajuan pencairan dana ke bank / e-wallet (pengurangan saldo)
 */
class BuildWalletTransactionsUseCase {
    operator fun invoke(
        bookings: List<BookingItem>,
        payouts: List<PayoutRequest>
    ): List<WalletTransaction> {
        val creditTransactions = bookings
            .filter {
                it.status == BookingStatus.SELESAI &&
                    it.paymentMethod == PaymentMethod.QRIS &&
                    it.paymentStatus == PaymentStatus.PAID
            }
            .map { booking ->
                WalletTransaction(
                    id = "credit_${booking.id}",
                    type = WalletTransactionType.CREDIT,
                    title = booking.serviceName,
                    subtitle = "${booking.customerName} • QRIS Lunas",
                    amount = booking.totalCost,
                    adminFee = 0,
                    netAmount = booking.totalCost,
                    channelName = "QRIS",
                    channelType = null,
                    payoutStatus = null,
                    statusLabel = "Dana Masuk",
                    createdAt = booking.finishedAt ?: booking.createdAt
                )
            }

        val debitTransactions = payouts.map { payout ->
            WalletTransaction(
                id = "debit_${payout.id}",
                type = WalletTransactionType.DEBIT,
                title = "Penarikan ke ${payout.channelName}",
                subtitle = "${payout.accountNumber} • a.n. ${payout.accountHolder}",
                amount = payout.amount,
                adminFee = payout.adminFee,
                netAmount = payout.netAmount,
                channelName = payout.channelName,
                channelType = payout.channelType,
                payoutStatus = payout.status,
                statusLabel = payout.status.label,
                createdAt = payout.createdAt
            )
        }

        return (creditTransactions + debitTransactions).sortedByDescending { it.createdAt }
    }
}
