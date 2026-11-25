package id.bantuin.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemeMode
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.ui.navigation.BantuinNavGraph
import id.bantuin.app.ui.theme.BantuinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as? BantuinApp
        val repository = app?.repository ?: FakeBantuinRepository()
        val themeStore = app?.themeStore ?: InMemoryThemeStore()
        val initialUser = app?.sessionStore?.currentUserValue
        val initialRole = app?.sessionStore?.activeRoleValue ?: "customer"

        setContent {
            val themeMode by themeStore.themeMode.collectAsState(initial = themeStore.themeModeValue)
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
            }

            BantuinTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BantuinTheme.colors.background
                ) {
                    BantuinNavGraph(
                        repository = repository,
                        themeStore = themeStore,
                        initialUser = initialUser,
                        initialRole = initialRole
                    )
                }
            }
        }
    }
}
