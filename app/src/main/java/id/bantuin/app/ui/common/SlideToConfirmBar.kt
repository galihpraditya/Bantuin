package id.bantuin.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import id.bantuin.app.ui.theme.BantuinTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SlideToConfirmBar(
    label: String,
    enabled: Boolean,
    isLoading: Boolean,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityActionLabel: String = label
) {
    val colors = BantuinTheme.colors
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val trackHeight = 60.dp
    val thumbSize = 52.dp
    val trackPadding = 4.dp

    val dragOffsetPx = remember { Animatable(0f) }
    var triggered by remember { mutableStateOf(false) }
    var thresholdHapticFired by remember { mutableStateOf(false) }

    LaunchedEffect(enabled, isLoading) {
        if (!enabled) {
            triggered = false
            thresholdHapticFired = false
            dragOffsetPx.snapTo(0f)
        } else if (!isLoading && triggered) {
            triggered = false
            thresholdHapticFired = false
            dragOffsetPx.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    val thumbIconTint = if (colors.isDark) colors.background else Color.White

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(30.dp))
                .background(if (enabled) colors.surfaceSubtle else colors.surfaceSubtle.copy(alpha = 0.6f))
                .border(
                    width = 1.5.dp,
                    color = if (enabled) colors.outline else colors.borderSubtle,
                    shape = RoundedCornerShape(30.dp)
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    if (!enabled) {
                        disabled()
                    } else {
                        onClick(label = accessibilityActionLabel) {
                            if (!isLoading && !triggered) {
                                triggered = true
                                onConfirmed()
                            }
                            true
                        }
                    }
                }
        ) {
            val maxDragPx = with(density) {
                (maxWidth - thumbSize - trackPadding * 2).toPx().coerceAtLeast(1f)
            }
            val thumbSizePx = with(density) { thumbSize.toPx() }
            val paddingPx = with(density) { trackPadding.toPx() }
            val progress = (dragOffsetPx.value / maxDragPx).coerceIn(0f, 1f)

            // Progress fill
            if (progress > 0f && enabled) {
                val fillFraction = ((dragOffsetPx.value + thumbSizePx + paddingPx) /
                    with(density) { maxWidth.toPx().coerceAtLeast(1f) }).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            if (progress >= 0.85f || triggered) colors.success.copy(alpha = 0.25f)
                            else colors.successContainer
                        )
                )
            }

            // Center label
            Text(
                text = when {
                    isLoading -> "Memproses penyelesaian..."
                    !enabled -> label
                    progress >= 0.85f -> "Lepaskan untuk konfirmasi"
                    else -> label
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) colors.textPrimary else colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 64.dp)
            )

            // Draggable knob
            Box(
                modifier = Modifier
                    .padding(trackPadding)
                    .offset { IntOffset(dragOffsetPx.value.roundToInt(), 0) }
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(if (enabled) colors.success else colors.borderSubtle)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        enabled = enabled && !isLoading && !triggered,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                val next = (dragOffsetPx.value + delta).coerceIn(0f, maxDragPx)
                                dragOffsetPx.snapTo(next)
                                val nextRatio = next / maxDragPx
                                if (nextRatio >= 0.85f && !thresholdHapticFired) {
                                    thresholdHapticFired = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } else if (nextRatio < 0.80f) {
                                    thresholdHapticFired = false
                                }
                            }
                        },
                        onDragStopped = {
                            val ratio = dragOffsetPx.value / maxDragPx
                            if (ratio >= 0.85f && !triggered) {
                                triggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    dragOffsetPx.animateTo(
                                        maxDragPx,
                                        spring(stiffness = Spring.StiffnessMedium)
                                    )
                                }
                                onConfirmed()
                            } else {
                                thresholdHapticFired = false
                                scope.launch {
                                    dragOffsetPx.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = thumbIconTint,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (progress >= 0.85f || triggered) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.DoubleArrow
                        },
                        contentDescription = null,
                        tint = if (enabled) thumbIconTint else colors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Alternatif aksesibilitas non-drag untuk pengguna TalkBack / Switch Access / keyboard
        TextButton(
            onClick = {
                if (enabled && !isLoading && !triggered) {
                    triggered = true
                    onConfirmed()
                }
            },
            enabled = enabled && !isLoading,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .heightIn(min = 48.dp)
        ) {
            Text(
                text = "Konfirmasi Tanpa Menggeser (Aksesibilitas)",
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) colors.primary else colors.textMuted
            )
        }
    }
}
