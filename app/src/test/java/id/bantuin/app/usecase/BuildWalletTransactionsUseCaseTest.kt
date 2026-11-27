package id.bantuin.app.usecase

import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.model.PayoutChannelType
import id.bantuin.app.domain.model.PayoutRequest
import id.bantuin.app.domain.model.PayoutStatus
import id.bantuin.app.domain.model.WalletTransactionType
import id.bantuin.app.domain.usecase.wallet.BuildWalletTransactionsUseCase
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildWalletTransactionsUseCaseTest {

    private val useCase = BuildWalletTransactionsUseCase()

    @Test
    fun invoke_combinesPaidCompletedQrisAndPayoutsSortedByNewest() {
        val t1 = Instant.parse("2026-11-20T09:00:00Z")
        val t2 = Instant.parse("2026-11-21T10:00:00Z")
        val t3 = Instant.parse("2026-11-22T12:00:00Z")

        val bookings = listOf(
            sampleBooking("b_qris_done", 120_000, BookingStatus.SELESAI, PaymentMethod.QRIS, PaymentStatus.PAID, t1, t2),
            sampleBooking("b_cash_done", 80_000, BookingStatus.SELESAI, PaymentMethod.CASH, PaymentStatus.PAID, t1, t2),
            sampleBooking("b_qris_ongoing", 95_000, BookingStatus.DALAM_PENGERJAAN, PaymentMethod.QRIS, PaymentStatus.PAID, t1, null)
        )
        val payouts = listOf(
            PayoutRequest(
                id = "w1",
                providerId = "p01",
                amount = 50_000,
                adminFee = 2_500,
                netAmount = 47_500,
                channelType = PayoutChannelType.BANK,
                channelName = "BCA",
                accountNumber = "123456",
                accountHolder = "Budi",
                status = PayoutStatus.COMPLETED,
                createdAt = t3
            )
        )

        val txList = useCase(bookings, payouts)

        assertEquals(2, txList.size)
        assertEquals(WalletTransactionType.DEBIT, txList[0].type)
        assertEquals(50_000, txList[0].amount)
        assertEquals("BCA", txList[0].channelName)

        assertEquals(WalletTransactionType.CREDIT, txList[1].type)
        assertEquals(120_000, txList[1].amount)
        assertEquals("QRIS", txList[1].channelName)
    }

    private fun sampleBooking(
        id: String,
        cost: Int,
        status: BookingStatus,
        method: PaymentMethod,
        payStatus: PaymentStatus,
        createdAt: Instant,
        finishedAt: Instant?
    ) = BookingItem(
        id = id,
        customerId = "c1",
        customerName = "Rani",
        providerId = "p01",
        providerName = "Budi",
        serviceName = "Cuci AC",
        scheduledDate = "2026-11-21",
        scheduledTime = "09:00 - 10:30 WIB",
        totalCost = cost,
        status = status,
        address = "Jl. Ijen",
        createdAt = createdAt,
        finishedAt = finishedAt,
        paymentMethod = method,
        paymentStatus = payStatus
    )
}
