package id.bantuin.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import id.bantuin.app.ui.theme.BantuinTheme

/**
 * Ilustrasi status & lencana Bantuin bergaya soft duotone tanpa garis luar hitam tebal,
 * menjaga keselarasan visual dengan ikon layanan (R-04, R-22, R-25).
 */
@Composable
fun EmergencyBeaconIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(44.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = 0.20f),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
        drawCircle(
            color = Color(0xFFDC2626),
            radius = w * 0.34f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        val bolt = Path().apply {
            moveTo(w * 0.54f, h * 0.26f)
            lineTo(w * 0.37f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.52f)
            lineTo(w * 0.46f, h * 0.74f)
            lineTo(w * 0.64f, h * 0.46f)
            lineTo(w * 0.51f, h * 0.46f)
            close()
        }
        drawPath(path = bolt, color = Color(0xFFFDE047))
    }
}

@Composable
fun WarrantyShieldIllustration(
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Canvas(modifier = modifier.size(44.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = colors.successContainer,
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        val shield = Path().apply {
            moveTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.75f, h * 0.28f)
            lineTo(w * 0.75f, h * 0.50f)
            cubicTo(w * 0.75f, h * 0.68f, w * 0.58f, h * 0.80f, w * 0.50f, h * 0.84f)
            cubicTo(w * 0.42f, h * 0.80f, w * 0.25f, h * 0.68f, w * 0.25f, h * 0.50f)
            lineTo(w * 0.25f, h * 0.28f)
            close()
        }
        drawPath(path = shield, color = colors.success)

        val check = Path().apply {
            moveTo(w * 0.39f, h * 0.51f)
            lineTo(w * 0.47f, h * 0.60f)
            lineTo(w * 0.62f, h * 0.42f)
        }
        drawPath(
            path = check,
            color = if (colors.isDark) colors.background else Color.White,
            style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Ilustrasi status kosong khas Bantuin: papan kerja pesanan kosong dengan penjepit logam (tanpa bintang AI).
 */
@Composable
fun EmptyStateIllustration(
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Canvas(modifier = modifier.size(84.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = colors.primaryContainer.copy(alpha = if (colors.isDark) 0.45f else 0.70f),
            radius = w * 0.43f,
            center = Offset(w * 0.5f, h * 0.52f)
        )

        // Papan clipboard kerja lembut dengan tepi halus sesuai tema
        drawRoundRect(
            color = colors.surface,
            topLeft = Offset(w * 0.27f, h * 0.24f),
            size = Size(w * 0.46f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        drawRoundRect(
            color = colors.borderSubtle,
            topLeft = Offset(w * 0.27f, h * 0.24f),
            size = Size(w * 0.46f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.025f)
        )

        // Penjepit di atas papan kerja
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(w * 0.39f, h * 0.17f),
            size = Size(w * 0.22f, h * 0.11f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )

        // Baris checklist kosong dengan kotak centang
        val rowY = listOf(0.41f, 0.54f, 0.67f)
        rowY.forEachIndexed { idx, y ->
            drawRoundRect(
                color = colors.primary,
                topLeft = Offset(w * 0.35f, h * (y - 0.03f)),
                size = Size(w * 0.06f, w * 0.06f),
                cornerRadius = CornerRadius(w * 0.015f, w * 0.015f),
                style = Stroke(width = w * 0.022f)
            )
            val endX = if (idx == 2) 0.54f else 0.63f
            drawLine(
                color = if (idx == 0) colors.textSecondary else colors.textMuted,
                start = Offset(w * 0.45f, h * y),
                end = Offset(w * endX, h * y),
                strokeWidth = w * 0.036f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ErrorStateIllustration(
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Canvas(modifier = modifier.size(84.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = colors.errorContainer,
            radius = w * 0.43f,
            center = Offset(w * 0.5f, h * 0.52f)
        )

        val triangle = Path().apply {
            moveTo(w * 0.50f, h * 0.22f)
            lineTo(w * 0.78f, h * 0.74f)
            lineTo(w * 0.22f, h * 0.74f)
            close()
        }
        drawPath(path = triangle, color = colors.surface)

        drawLine(
            color = colors.error,
            start = Offset(w * 0.50f, h * 0.41f),
            end = Offset(w * 0.50f, h * 0.56f),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = colors.error,
            radius = w * 0.034f,
            center = Offset(w * 0.50f, h * 0.65f)
        )
    }
}
