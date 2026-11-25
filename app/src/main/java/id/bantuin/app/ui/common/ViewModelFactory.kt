package id.bantuin.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.bantuin.app.core.settings.InMemorySettingsStore
import id.bantuin.app.core.settings.SettingsPreferenceStore
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemePreferenceStore
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.auth.AuthViewModel
import id.bantuin.app.ui.chat.ChatListViewModel
import id.bantuin.app.ui.chat.ChatRoomViewModel
import id.bantuin.app.ui.customer.estimate.EstimateViewModel
import id.bantuin.app.ui.customer.home.CustomerHomeViewModel
import id.bantuin.app.ui.customer.notifications.CustomerNotificationsViewModel
import id.bantuin.app.ui.customer.orderdetail.OrderDetailViewModel
import id.bantuin.app.ui.customer.orders.CustomerOrdersViewModel
import id.bantuin.app.ui.customer.profile.CustomerProfileViewModel
import id.bantuin.app.ui.customer.providerdetail.ProviderDetailViewModel
import id.bantuin.app.ui.customer.recommendation.RecommendationViewModel
import id.bantuin.app.ui.customer.schedule.ScheduleViewModel
import id.bantuin.app.ui.payment.QrisPaymentViewModel
import id.bantuin.app.ui.provider.calendar.ProviderCalendarViewModel
import id.bantuin.app.ui.provider.dashboard.ProviderDashboardViewModel
import id.bantuin.app.ui.provider.evaluation.ProviderEvaluationViewModel
import id.bantuin.app.ui.provider.home.ProviderHomeViewModel
import id.bantuin.app.ui.provider.orders.CompleteOrderConfirmViewModel
import id.bantuin.app.ui.provider.orders.ProviderOrdersViewModel
import id.bantuin.app.ui.provider.profile.ProviderProfileEditViewModel
import id.bantuin.app.ui.provider.profile.ProviderProfileViewModel
import id.bantuin.app.ui.provider.services.ProviderServicesViewModel
import id.bantuin.app.ui.provider.wallet.ProviderWithdrawViewModel
import id.bantuin.app.ui.settings.SettingsViewModel

class BantuinViewModelFactory(
    private val repository: BantuinRepository,
    private val themeStore: ThemePreferenceStore = InMemoryThemeStore(),
    private val settingsStore: SettingsPreferenceStore = InMemorySettingsStore()
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(repository) as T
            modelClass.isAssignableFrom(CustomerHomeViewModel::class.java) ->
                CustomerHomeViewModel(repository, repository, repository, repository) as T
            modelClass.isAssignableFrom(RecommendationViewModel::class.java) ->
                RecommendationViewModel(repository) as T
            modelClass.isAssignableFrom(ProviderDetailViewModel::class.java) ->
                ProviderDetailViewModel(repository, repository) as T
            modelClass.isAssignableFrom(ScheduleViewModel::class.java) ->
                ScheduleViewModel(repository, repository) as T
            modelClass.isAssignableFrom(EstimateViewModel::class.java) ->
                EstimateViewModel(repository) as T
            modelClass.isAssignableFrom(OrderDetailViewModel::class.java) ->
                OrderDetailViewModel(repository, repository) as T
            modelClass.isAssignableFrom(CustomerOrdersViewModel::class.java) ->
                CustomerOrdersViewModel(repository) as T
            modelClass.isAssignableFrom(QrisPaymentViewModel::class.java) ->
                QrisPaymentViewModel(repository) as T
            modelClass.isAssignableFrom(ChatListViewModel::class.java) ->
                ChatListViewModel(repository, repository) as T
            modelClass.isAssignableFrom(ChatRoomViewModel::class.java) ->
                ChatRoomViewModel(repository, repository, repository) as T
            modelClass.isAssignableFrom(CustomerNotificationsViewModel::class.java) ->
                CustomerNotificationsViewModel(repository) as T
            modelClass.isAssignableFrom(CustomerProfileViewModel::class.java) ->
                CustomerProfileViewModel(repository, repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repository, themeStore, settingsStore) as T
            modelClass.isAssignableFrom(ProviderProfileViewModel::class.java) ->
                ProviderProfileViewModel(repository, repository, repository) as T
            modelClass.isAssignableFrom(ProviderHomeViewModel::class.java) ->
                ProviderHomeViewModel(repository, repository, repository, repository, repository, repository) as T
            modelClass.isAssignableFrom(ProviderOrdersViewModel::class.java) ->
                ProviderOrdersViewModel(repository) as T
            modelClass.isAssignableFrom(CompleteOrderConfirmViewModel::class.java) ->
                CompleteOrderConfirmViewModel(repository) as T
            modelClass.isAssignableFrom(ProviderCalendarViewModel::class.java) ->
                ProviderCalendarViewModel(repository, repository, repository) as T
            modelClass.isAssignableFrom(ProviderServicesViewModel::class.java) ->
                ProviderServicesViewModel(repository, repository) as T
            modelClass.isAssignableFrom(ProviderProfileEditViewModel::class.java) ->
                ProviderProfileEditViewModel(repository, repository) as T
            modelClass.isAssignableFrom(ProviderDashboardViewModel::class.java) ->
                ProviderDashboardViewModel(repository, repository, repository, repository) as T
            modelClass.isAssignableFrom(ProviderWithdrawViewModel::class.java) ->
                ProviderWithdrawViewModel(repository, repository) as T
            modelClass.isAssignableFrom(ProviderEvaluationViewModel::class.java) ->
                ProviderEvaluationViewModel(repository, repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
