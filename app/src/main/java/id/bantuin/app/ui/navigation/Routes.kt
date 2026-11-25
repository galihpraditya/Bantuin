package id.bantuin.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Pelanggan Flow
    const val CUSTOMER_HOME = "customer_home"
    const val CUSTOMER_ORDERS = "customer_orders"
    const val CUSTOMER_NOTIFICATIONS = "customer_notifications"
    const val CUSTOMER_PROFILE = "customer_profile"

    // Chat & Pengaturan Universal
    const val CHAT_LIST = "chat_list"
    const val CHAT_ROOM = "chat_room/{bookingId}"
    const val SETTINGS = "settings"

    // Order Funnel & Pembayaran Pelanggan
    const val RECOMMENDATION = "recommendation/{categoryId}"
    const val PROVIDER_DETAIL = "provider_detail/{providerId}"
    const val SCHEDULE_SELECT = "schedule_select"
    const val ESTIMATE_ORDER = "estimate_order"
    const val QRIS_PAYMENT = "qris_payment/{bookingId}"
    const val ORDER_TRACKING = "order_tracking/{bookingId}"

    // Helper functions for parameterized routes
    fun recommendation(categoryId: Int) = "recommendation/$categoryId"
    fun providerDetail(providerId: String) = "provider_detail/$providerId"
    fun orderTracking(bookingId: String) = "order_tracking/$bookingId"
    fun chatRoom(bookingId: String) = "chat_room/$bookingId"
    fun qrisPayment(bookingId: String) = "qris_payment/$bookingId"
    fun completeOrder(bookingId: String) = "complete_order/$bookingId"

    // Mitra Flow
    const val PROVIDER_HOME = "provider_home"
    const val PROVIDER_ORDERS = "provider_orders"
    const val PROVIDER_CALENDAR = "provider_calendar"
    const val PROVIDER_DASHBOARD = "provider_dashboard"
    const val PROVIDER_EVALUATION = "provider_evaluation"
    const val PROVIDER_SERVICES = "provider_services"
    const val PROVIDER_PROFILE = "provider_profile"
    const val PROVIDER_PROFILE_EDIT = "provider_profile_edit"
    const val COMPLETE_ORDER = "complete_order/{bookingId}"
    const val PROVIDER_WITHDRAW = "provider_withdraw"
}
