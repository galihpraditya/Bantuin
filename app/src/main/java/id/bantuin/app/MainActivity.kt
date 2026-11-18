package id.bantuin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.ui.navigation.BantuinNavGraph
import id.bantuin.app.ui.theme.BantuinTheme
import id.bantuin.app.ui.theme.NeutralBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val repository = remember { FakeBantuinRepository() }

            BantuinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NeutralBackground
                ) {
                    BantuinNavGraph(repository = repository)
                }
            }
        }
    }
}
