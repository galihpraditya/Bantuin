package id.bantuin.app.ui.customer.notifications

enum class NotificationType {
    ORDER_PENDING,
    ORDER_CONFIRMED,
    ORDER_IN_PROGRESS,
    ORDER_COMPLETED,
    ORDER_CANCELLED,
    SYSTEM_INFO
}

data class CustomerNotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeFormatted: String,
    val type: NotificationType,
    val bookingId: String? = null,
    val timestampMillis: Long = 0L
)
