package id.bantuin.app.usecase

import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.model.PayoutChannelType
import id.bantuin.app.domain.model.PayoutRequest
import id.bantuin.app.domain.model.PayoutStatus
import id.bantuin.app.domain.usecase.wallet.CalculateProviderRevenueUseCase
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateProviderRevenueUseCaseTest {
    private val useCase = CalculateProviderRevenueUseCase()

    @Test
    fun invoke_separatesQrisWithdrawableBalanceFromCashWhileReportingCombinedTotal() {
        val now = Clock.System.now()
        val bookings = listOf(
            sampleBooking("b1", 100_000, BookingStatus.SELESAI, PaymentMethod.QRIS, PaymentStatus.PAID, now),
            sampleBooking("b2", 75_000, BookingStatus.SELESAI, PaymentMethod.CASH, PaymentStatus.PAID, now),
            sampleBooking("b3", 90_000, BookingStatus.DALAM_PENGERJAAN, PaymentMethod.QRIS, PaymentStatus.PAID, now),
            sampleBooking("b4", 60_000, BookingStatus.DIBATALKAN_PELANGGAN, PaymentMethod.QRIS, PaymentStatus.REFUNDED, now)
        )
        val payouts = listOf(
            PayoutRequest(
                id = "w1",
                providerId = "p01",
                amount = 40_000,
                adminFee = 2_500,
                netAmount = 37_500,
                channelType = PayoutChannelType.BANK,
                channelName = "BCA",
                accountNumber = "123456",
                accountHolder = "Budi",
                status = PayoutStatus.COMPLETED,
                createdAt = now
            )
        )

        val res = useCase(bookings, payouts)

        assertEquals(175_000, res.totalCombinedRevenue)
        assertEquals(100_000, res.qrisGrossRevenue)
        assertEquals(75_000, res.cashRevenue)
        assertEquals(60_000, res.availableBalance)
        assertEquals(40_000, res.totalWithdrawn)
        assertEquals(0, res.pendingWithdrawal)
        assertEquals(1, res.completedQrisCount)
        assertEquals(1, res.completedCashCount)
    }

    @Test
    fun invoke_subtractsProcessingPayoutsFromAvailableBalanceButTracksPendingSeparately() {
        val now = Clock.System.now()
        val bookings = listOf(
            sampleBooking("b1", 150_000, BookingStatus.SELESAI, PaymentMethod.QRIS, PaymentStatus.PAID, now)
        )
        val payouts = listOf(
            PayoutRequest("w1", "p01", 50_000, 2_500, 47_500, PayoutChannelType.EWALLET, "DANA", "0812", "Budi", PayoutStatus.PROCESSING, now),
            PayoutRequest("w2", "p01", 30_000, 2_500, 27_500, PayoutChannelType.BANK, "BCA", "123", "Budi", PayoutStatus.COMPLETED, now),
            PayoutRequest("w3", "p01", 20_000, 2_500, 17_500, PayoutChannelType.BANK, "BRI", "456", "Budi", PayoutStatus.FAILED, now)
        )

        val res = useCase(bookings, payouts)

        assertEquals(150_000, res.totalCombinedRevenue)
        assertEquals(70_000, res.availableBalance)
        assertEquals(30_000, res.totalWithdrawn)
        assertEquals(50_000, res.pendingWithdrawal)
    }

    private fun sampleBooking(
        id: String,
        cost: Int,
        status: BookingStatus,
        method: PaymentMethod,
        payStatus: PaymentStatus,
        now: Instant
    ) = BookingItem(
        id = id,
        customerId = "c1",
        customerName = "Rani",
        providerId = "p01",
        providerName = "Budi",
        serviceName = "Servis AC",
        scheduledDate = "2026-11-25",
        scheduledTime = "09:00 - 10:30 WIB",
        totalCost = cost,
        status = status,
        address = "Jl. Ijen",
        createdAt = now,
        paymentMethod = method,
        paymentStatus = payStatus
    )
}
