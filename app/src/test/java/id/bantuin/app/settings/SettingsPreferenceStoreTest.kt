package id.bantuin.app.settings

import id.bantuin.app.core.settings.InMemorySettingsStore
import id.bantuin.app.core.settings.NotificationSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPreferenceStoreTest {

    @Test
    fun updateSettings_emitsUpdatedNotificationToggles() = runTest {
        val store = InMemorySettingsStore()
        val initial = store.settings.first()
        assertTrue(initial.orderUpdatesEnabled)
        assertTrue(initial.scheduleReminderEnabled)
        assertTrue(initial.providerQueueAlertEnabled)
        assertTrue(initial.payoutAlertEnabled)

        val updated = NotificationSettings(
            orderUpdatesEnabled = false,
            scheduleReminderEnabled = true,
            providerQueueAlertEnabled = false,
            payoutAlertEnabled = true
        )
        store.updateSettings(updated)

        val current = store.settings.first()
        assertEquals(updated, current)
        assertFalse(store.settingsValue.orderUpdatesEnabled)
        assertFalse(store.settingsValue.providerQueueAlertEnabled)
        assertTrue(store.settingsValue.scheduleReminderEnabled)
        assertTrue(store.settingsValue.payoutAlertEnabled)
    }
}
