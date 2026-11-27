package id.bantuin.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.ui.theme.BantuinTheme
import kotlinx.coroutines.delay

@Composable
fun CountdownTimerView(
    targetEpochMillis: Long,
    onExpired: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    val latestOnExpired by rememberUpdatedState(onExpired)
    var remainingSeconds by remember(targetEpochMillis) {
        mutableLongStateOf(remainingCeilSeconds(targetEpochMillis))
    }

    LaunchedEffect(targetEpochMillis) {
        while (true) {
            val now = System.currentTimeMillis()
            remainingSeconds = remainingCeilSeconds(targetEpochMillis)
            if (now >= targetEpochMillis + 500L) {
                repeat(3) { attempt ->
                    latestOnExpired()
                    if (attempt < 2) delay(2000L)
                }
                break
            }
            delay(250L)
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.errorContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = null,
            tint = colors.error,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = TimeUtils.formatCountdown(remainingSeconds),
            style = MaterialTheme.typography.labelMedium,
            color = colors.error
        )
    }
}

private fun remainingCeilSeconds(targetEpochMillis: Long): Long {
    val diff = targetEpochMillis - System.currentTimeMillis()
    return if (diff <= 0L) 0L else (diff + 999L) / 1000L
}
