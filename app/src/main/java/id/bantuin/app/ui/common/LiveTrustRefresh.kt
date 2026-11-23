package id.bantuin.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
internal fun LiveTrustRefresh(changes: Flow<Long>, key: Any?, onRefresh: (background: Boolean) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val refresh = rememberUpdatedState(onRefresh)
    LaunchedEffect(lifecycle, changes, key) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            launch {
                var first = true
                changes.collect {
                    refresh.value(!first)
                    first = false
                }
            }
            while (true) {
                delay(15_000)
                refresh.value(true)
            }
        }
    }
}
