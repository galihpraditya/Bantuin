package id.bantuin.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

object BantuinMotion {
    const val ENTER_MS = 300
    const val EXIT_MS = 220
    const val TAB_FADE_IN_MS = 180
    const val TAB_FADE_OUT_MS = 140

    fun <T> bouncySpring(): SpringSpec<T> = spring(
        dampingRatio = 0.65f,
        stiffness = 500f
    )

    fun <T> snappySpring(): SpringSpec<T> = spring(
        dampingRatio = 0.8f,
        stiffness = 600f
    )
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

@Composable
fun Modifier.bantuinPressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    targetScale: Float = 0.95f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val desiredScale = if (enabled && isPressed) targetScale else 1f
    val scale by animateFloatAsState(
        targetValue = desiredScale,
        animationSpec = if (reducedMotion) snap() else BantuinMotion.bouncySpring(),
        label = "bantuinPressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
