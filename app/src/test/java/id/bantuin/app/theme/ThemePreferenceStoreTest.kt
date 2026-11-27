package id.bantuin.app.theme

import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceStoreTest {

    @Test
    fun setThemeMode_updatesStateFlowFromSystemToDarkAndLight() = runBlocking {
        val store = InMemoryThemeStore()
        assertEquals(ThemeMode.SYSTEM, store.themeMode.first())

        store.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, store.themeMode.first())

        store.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, store.themeMode.first())
    }
}
