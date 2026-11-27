package id.bantuin.app.domain.usecase.wallet

import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.model.PayoutRequest
import id.bantuin.app.domain.model.PayoutStatus
import id.bantuin.app.domain.model.ProviderRevenueAnalytics

/**
 * Menghitung ringkasan saldo & analisis pendapatan Mitra (Aturan 4A + Analisis Gabungan):
 * - Hanya pesanan SELESAI dengan pembayaran QRIS LUNAS yang masuk ke Saldo Siap Cair.
 * - Pesanan SELESAI dengan pembayaran Cash dicatat pada pendapatan tunai terpisah.
 * - Total pendapatan gabungan selalu persis sama dengan qrisGrossRevenue + cashRevenue.
 */
class CalculateProviderRevenueUseCase {
    operator fun invoke(
        bookings: List<BookingItem>,
        payouts: List<PayoutRequest>
    ): ProviderRevenueAnalytics {
        val completedPaidBookings = bookings.filter {
            it.status == BookingStatus.SELESAI && it.paymentStatus == PaymentStatus.PAID
        }
        val completedQris = completedPaidBookings.filter { it.paymentMethod == PaymentMethod.QRIS }
        val completedCash = completedPaidBookings.filter { it.paymentMethod == PaymentMethod.CASH }

        val qrisGrossRevenue = completedQris.sumOf { it.totalCost }
        val cashRevenue = completedCash.sumOf { it.totalCost }
        val totalCombinedRevenue = qrisGrossRevenue + cashRevenue

        val totalWithdrawn = payouts
            .filter { it.status == PayoutStatus.COMPLETED }
            .sumOf { it.amount }
        val pendingWithdrawal = payouts
            .filter { it.status == PayoutStatus.PENDING || it.status == PayoutStatus.PROCESSING }
            .sumOf { it.amount }
        val availableBalance = (qrisGrossRevenue - totalWithdrawn - pendingWithdrawal).coerceAtLeast(0)

        return ProviderRevenueAnalytics(
            totalCombinedRevenue = totalCombinedRevenue,
            qrisGrossRevenue = qrisGrossRevenue,
            cashRevenue = cashRevenue,
            availableBalance = availableBalance,
            totalWithdrawn = totalWithdrawn,
            pendingWithdrawal = pendingWithdrawal,
            completedQrisCount = completedQris.size,
            completedCashCount = completedCash.size
        )
    }
}
