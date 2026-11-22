package id.bantuin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.ui.navigation.BantuinNavGraph
import id.bantuin.app.ui.theme.BantuinTheme
import id.bantuin.app.ui.theme.NeutralBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as? BantuinApp
        val repository = app?.repository ?: FakeBantuinRepository()
        val initialUser = app?.sessionStore?.currentUserValue
        val initialRole = app?.sessionStore?.activeRoleValue ?: "customer"

        setContent {
            BantuinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NeutralBackground
                ) {
                    BantuinNavGraph(
                        repository = repository,
                        initialUser = initialUser,
                        initialRole = initialRole
                    )
                }
            }
        }
    }
}
