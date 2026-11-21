package id.bantuin.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Pelanggan Flow
    const val CUSTOMER_HOME = "customer_home"
    const val CUSTOMER_ORDERS = "customer_orders"
    const val CUSTOMER_NOTIFICATIONS = "customer_notifications"
    const val CUSTOMER_SCHEDULE = "customer_schedule"
    const val CUSTOMER_PROFILE = "customer_profile"

    // Order Funnel Pelanggan
    const val RECOMMENDATION = "recommendation/{categoryId}"
    const val PROVIDER_DETAIL = "provider_detail/{providerId}"
    const val SCHEDULE_SELECT = "schedule_select"
    const val ESTIMATE_ORDER = "estimate_order"
    const val ORDER_TRACKING = "order_tracking/{bookingId}"

    // Helper functions for parameterized routes
    fun recommendation(categoryId: Int) = "recommendation/$categoryId"
    fun providerDetail(providerId: String) = "provider_detail/$providerId"
    fun orderTracking(bookingId: String) = "order_tracking/$bookingId"

    // Mitra Flow
    const val PROVIDER_HOME = "provider_home"
    const val PROVIDER_ORDERS = "provider_orders"
    const val PROVIDER_CALENDAR = "provider_calendar"
    const val PROVIDER_DASHBOARD = "provider_dashboard"
    const val PROVIDER_EVALUATION = "provider_evaluation"
    const val PROVIDER_SERVICES = "provider_services"
    const val PROVIDER_PROFILE_EDIT = "provider_profile_edit"
}
