package id.bantuin.app.core.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationSettings(
    val orderUpdatesEnabled: Boolean = true,
    val scheduleReminderEnabled: Boolean = true,
    val providerQueueAlertEnabled: Boolean = true,
    val payoutAlertEnabled: Boolean = true
)

interface SettingsPreferenceStore {
    val settings: Flow<NotificationSettings>
    val settingsValue: NotificationSettings
    suspend fun updateSettings(settings: NotificationSettings)
}

class InMemorySettingsStore(
    initialSettings: NotificationSettings = NotificationSettings()
) : SettingsPreferenceStore {
    private val _settings = MutableStateFlow(initialSettings)
    override val settings: Flow<NotificationSettings> = _settings.asStateFlow()
    override val settingsValue: NotificationSettings get() = _settings.value

    override suspend fun updateSettings(settings: NotificationSettings) {
        _settings.value = settings
    }
}

class SharedPreferencesSettingsStore(
    private val context: Context
) : SettingsPreferenceStore {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("bantuin_settings_prefs", Context.MODE_PRIVATE)
    }

    private val _settings = MutableStateFlow(readSettings())
    override val settings: Flow<NotificationSettings> = _settings.asStateFlow()
    override val settingsValue: NotificationSettings get() = _settings.value

    private fun readSettings(): NotificationSettings {
        return try {
            NotificationSettings(
                orderUpdatesEnabled = prefs.getBoolean(KEY_ORDER_UPDATES, true),
                scheduleReminderEnabled = prefs.getBoolean(KEY_SCHEDULE_REMINDER, true),
                providerQueueAlertEnabled = prefs.getBoolean(KEY_PROVIDER_QUEUE_ALERT, true),
                payoutAlertEnabled = prefs.getBoolean(KEY_PAYOUT_ALERT, true)
            )
        } catch (_: Exception) {
            NotificationSettings()
        }
    }

    override suspend fun updateSettings(settings: NotificationSettings) {
        _settings.value = settings
        try {
            prefs.edit()
                .putBoolean(KEY_ORDER_UPDATES, settings.orderUpdatesEnabled)
                .putBoolean(KEY_SCHEDULE_REMINDER, settings.scheduleReminderEnabled)
                .putBoolean(KEY_PROVIDER_QUEUE_ALERT, settings.providerQueueAlertEnabled)
                .putBoolean(KEY_PAYOUT_ALERT, settings.payoutAlertEnabled)
                .apply()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val KEY_ORDER_UPDATES = "notif_order_updates"
        private const val KEY_SCHEDULE_REMINDER = "notif_schedule_reminder"
        private const val KEY_PROVIDER_QUEUE_ALERT = "notif_provider_queue"
        private const val KEY_PAYOUT_ALERT = "notif_payout_alert"
    }
}
