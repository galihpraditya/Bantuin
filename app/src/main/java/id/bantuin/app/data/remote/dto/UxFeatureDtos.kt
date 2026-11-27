package id.bantuin.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookingMessageDto(
    @SerialName("id") val id: String,
    @SerialName("booking_id") val bookingId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("message_text") val messageText: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class BookingMessageInsertDto(
    @SerialName("booking_id") val bookingId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("message_text") val messageText: String
)

@Serializable
data class PayoutRequestDto(
    @SerialName("id") val id: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("amount") val amount: Int,
    @SerialName("admin_fee") val adminFee: Int = 2500,
    @SerialName("net_amount") val netAmount: Int,
    @SerialName("channel_type") val channelType: String,
    @SerialName("channel_name") val channelName: String,
    @SerialName("account_number") val accountNumber: String,
    @SerialName("account_holder") val accountHolder: String,
    @SerialName("status") val status: String = "COMPLETED",
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ConfirmQrisPaymentRpcParams(
    @SerialName("p_booking") val bookingId: String
)

@Serializable
data class CompleteBookingRpcParams(
    @SerialName("p_booking") val bookingId: String,
    @SerialName("p_completion_note") val completionNote: String? = null,
    @SerialName("p_cash_collected") val cashCollected: Boolean = false
)

@Serializable
data class RequestPayoutRpcParams(
    @SerialName("p_amount") val amount: Int,
    @SerialName("p_channel_type") val channelType: String,
    @SerialName("p_channel_name") val channelName: String,
    @SerialName("p_account_number") val accountNumber: String,
    @SerialName("p_account_holder") val accountHolder: String
)
