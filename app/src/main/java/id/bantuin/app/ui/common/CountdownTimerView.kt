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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.RedErrorContainer
import kotlinx.coroutines.delay

@Composable
fun CountdownTimerView(
    targetEpochMillis: Long,
    onExpired: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember(targetEpochMillis) {
        mutableLongStateOf((targetEpochMillis - System.currentTimeMillis()).coerceAtLeast(0) / 1000)
    }

    LaunchedEffect(targetEpochMillis) {
        while (remainingSeconds > 0) {
            delay(1000)
            val left = (targetEpochMillis - System.currentTimeMillis()).coerceAtLeast(0) / 1000
            remainingSeconds = left
            if (left <= 0) {
                onExpired()
                break
            }
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(RedErrorContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = null,
            tint = RedError,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = TimeUtils.formatCountdown(remainingSeconds),
            style = MaterialTheme.typography.labelMedium,
            color = RedError
        )
    }
}
