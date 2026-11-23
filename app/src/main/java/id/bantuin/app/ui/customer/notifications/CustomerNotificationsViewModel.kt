package id.bantuin.app.ui.customer.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerNotificationsUiState(
    val notifications: List<CustomerNotificationItem> = emptyList(),
    val hasActiveOrder: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CustomerNotificationsViewModel(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerNotificationsUiState())
    val uiState: StateFlow<CustomerNotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            bookingRepository.observeCustomerBookings().collect { bookings ->
                val notifList = mapBookingsToNotifications(bookings)
                val hasActive = bookings.any {
                    it.status in listOf(
                        BookingStatus.PENDING,
                        BookingStatus.DIKONFIRMASI,
                        BookingStatus.DALAM_PENGERJAAN
                    )
                }
                _uiState.update {
                    it.copy(
                        notifications = notifList,
                        hasActiveOrder = hasActive,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun mapBookingsToNotifications(bookings: List<BookingItem>): List<CustomerNotificationItem> {
        val list = mutableListOf<CustomerNotificationItem>()

        bookings.forEach { booking ->
            val notif = when (booking.status) {
                BookingStatus.PENDING -> CustomerNotificationItem(
                    id = "notif_pending_${booking.id}",
                    title = "Pesanan Menunggu Konfirmasi",
                    message = "Layanan ${booking.serviceName} ke ${booking.providerName} sedang menunggu konfirmasi mitra (SLA 15 menit).",
                    timeFormatted = "${booking.scheduledDate} • ${booking.scheduledTime}",
                    type = NotificationType.ORDER_PENDING,
                    bookingId = booking.id,
                    timestampMillis = booking.createdAt.toEpochMilliseconds()
                )
                BookingStatus.DIKONFIRMASI -> CustomerNotificationItem(
                    id = "notif_confirmed_${booking.id}",
                    title = "Mitra Menuju Lokasi",
                    message = "${booking.providerName} telah menerima pesanan Anda dan sedang menuju lokasi. Pengerjaan belum dimulai.",
                    timeFormatted = "${booking.scheduledDate} • ${booking.scheduledTime}",
                    type = NotificationType.ORDER_CONFIRMED,
                    bookingId = booking.id,
                    timestampMillis = booking.createdAt.toEpochMilliseconds() + 60_000L
                )
                BookingStatus.DALAM_PENGERJAAN -> CustomerNotificationItem(
                    id = "notif_progress_${booking.id}",
                    title = "Pengerjaan Berlangsung",
                    message = "Teknisi ${booking.providerName} sedang melakukan pengerjaan ${booking.serviceName} di lokasi Anda.",
                    timeFormatted = "${booking.scheduledDate} • Sedang berjalan",
                    type = NotificationType.ORDER_IN_PROGRESS,
                    bookingId = booking.id,
                    timestampMillis = booking.createdAt.toEpochMilliseconds() + 120_000L
                )
                BookingStatus.SELESAI -> CustomerNotificationItem(
                    id = "notif_done_${booking.id}",
                    title = "Pesanan Telah Selesai",
                    message = if (booking.review != null) {
                        "Layanan ${booking.serviceName} selesai dikerjakan. Terima kasih atas ulasan yang Anda berikan!"
                    } else {
                        "Layanan ${booking.serviceName} selesai dikerjakan. Berikan ulasan untuk membantu reputasi mitra."
                    },
                    timeFormatted = "${booking.scheduledDate} • Selesai",
                    type = NotificationType.ORDER_COMPLETED,
                    bookingId = booking.id,
                    timestampMillis = booking.createdAt.toEpochMilliseconds() + 180_000L
                )
                BookingStatus.DITOLAK -> CustomerNotificationItem(
                    id = "notif_rejected_${booking.id}",
                    title = "Pesanan Ditolak Mitra",
                    message = "Mitra ${booking.providerName} berhalangan menerima pesanan ${booking.serviceName}. Silakan pilih mitra lain.",
                    timeFormatted = "${booking.scheduledDate}",
                    type = NotificationType.ORDER_CANCELLED,
                    bookingId = booking.id,
                    timestampMillis = booking.createdAt.toEpochMilliseconds() + 60_000L
                )
                BookingStatus.KEDALUWARSA -> CustomerNotificationItem(
                    id = "notif_expired_${booking.id}",
                    title = "Pesanan Kedaluwarsa",
                    message = "Batas waktu respon konfirmasi 15 menit telah habis untuk pesanan ${booking.serviceName}.",
                    timeFormatted = "${booking.scheduledDate}",
                    type = NotificationType.ORDER_CANCELLED,
                    bookingId = booking.id,
                    timestampMillis = booking.createdAt.toEpochMilliseconds() + 60_000L
                )
                BookingStatus.DIBATALKAN_PELANGGAN,
                BookingStatus.DIBATALKAN_MITRA -> CustomerNotificationItem(
                    id = "notif_cancelled_${booking.id}",
                    title = "Pesanan Dibatalkan",
                    message = "Pesanan ${booking.serviceName} telah dibatalkan.",
                    timeFormatted = "${booking.scheduledDate}",
                    type = NotificationType.ORDER_CANCELLED,
                    bookingId = booking.id,
                    timestampMillis = booking.createdAt.toEpochMilliseconds() + 60_000L
                )
            }
            list.add(notif)
        }

        list.addAll(defaultSystemNotifications())
        return list.sortedByDescending { it.timestampMillis }
    }

    private fun defaultSystemNotifications(): List<CustomerNotificationItem> {
        return listOf(
            CustomerNotificationItem(
                id = "sys_notif_garansi",
                title = "Garansi Pengerjaan 48 Jam",
                message = "Setiap pesanan di Bantuin dilindungi garansi 48 jam. Jika masalah serupa berulang, mitra kami akan menanganinya tanpa biaya tambahan.",
                timeFormatted = "Info Sistem",
                type = NotificationType.SYSTEM_INFO,
                bookingId = null,
                timestampMillis = 1000L
            ),
            CustomerNotificationItem(
                id = "sys_notif_welcome",
                title = "Selamat Datang di Bantuin!",
                message = "Temukan teknisi AC, kelistrikan, dan perpipaan terpercaya di sekitar Anda dengan sistem tarif transparan.",
                timeFormatted = "Info Sistem",
                type = NotificationType.SYSTEM_INFO,
                bookingId = null,
                timestampMillis = 500L
            )
        )
    }
}
