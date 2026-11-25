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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import id.bantuin.app.ui.theme.BantuinTheme

@Composable
fun HeroTechnicianIllustration(
    modifier: Modifier = Modifier
) {
    val isDark = BantuinTheme.colors.isDark
    Canvas(modifier = modifier.size(width = 96.dp, height = 78.dp)) {
        val w = size.width
        val h = size.height

        // Lingkaran cahaya latar belakang
        val glowColor = if (isDark) {
            Color(0xFFFB8C00).copy(alpha = 0.20f)
        } else {
            Color.White.copy(alpha = 0.16f)
        }
        drawCircle(
            color = glowColor,
            radius = w * 0.36f,
            center = Offset(w * 0.52f, h * 0.52f)
        )

        // Siluet rumah / perbukitan Malang di dasar
        val hillColor = if (isDark) {
            Color(0xFFFFB74D).copy(alpha = 0.18f)
        } else {
            Color.White.copy(alpha = 0.18f)
        }
        val hills = Path().apply {
            moveTo(w * 0.05f, h * 0.92f)
            quadraticTo(w * 0.28f, h * 0.72f, w * 0.52f, h * 0.88f)
            quadraticTo(w * 0.74f, h * 0.70f, w * 0.95f, h * 0.92f)
            close()
        }
        drawPath(path = hills, color = hillColor)

        // Badan teknisi (seragam krem/oranye cerah)
        val jacketColor = if (isDark) Color(0xFFFB8C00) else Color(0xFFFFEDD5)
        val collarColor = if (isDark) Color(0xFF1C1E22) else Color(0xFF9A3412)
        val torso = Path().apply {
            moveTo(w * 0.28f, h * 0.92f)
            cubicTo(w * 0.28f, h * 0.66f, w * 0.72f, h * 0.66f, w * 0.72f, h * 0.92f)
            close()
        }
        drawPath(path = torso, color = jacketColor)
        drawLine(
            color = collarColor,
            start = Offset(w * 0.44f, h * 0.72f),
            end = Offset(w * 0.50f, h * 0.84f),
            strokeWidth = w * 0.03f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = collarColor,
            start = Offset(w * 0.56f, h * 0.72f),
            end = Offset(w * 0.50f, h * 0.84f),
            strokeWidth = w * 0.03f,
            cap = StrokeCap.Round
        )

        // Wajah ramah
        drawCircle(
            color = Color(0xFFFDE0C6),
            radius = w * 0.13f,
            center = Offset(w * 0.50f, h * 0.52f)
        )
        // Mata & senyum tipis
        drawCircle(color = Color(0xFF1E293B), radius = w * 0.016f, center = Offset(w * 0.455f, h * 0.51f))
        drawCircle(color = Color(0xFF1E293B), radius = w * 0.016f, center = Offset(w * 0.545f, h * 0.51f))
        drawArc(
            color = Color(0xFF9A3412),
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.445f, h * 0.535f),
            size = Size(w * 0.11f, h * 0.07f),
            style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
        )

        // Topi / Helm Teknisi
        val helmet = Path().apply {
            moveTo(w * 0.35f, h * 0.44f)
            cubicTo(w * 0.35f, h * 0.25f, w * 0.65f, h * 0.25f, w * 0.65f, h * 0.44f)
            close()
        }
        drawPath(path = helmet, color = Color(0xFFFACC15))
        drawRoundRect(
            color = Color(0xFFEAB308),
            topLeft = Offset(w * 0.32f, h * 0.41f),
            size = Size(w * 0.38f, h * 0.055f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        // Kunci pas mengambang di kanan atas teknisi
        rotate(degrees = 28f, pivot = Offset(w * 0.79f, h * 0.44f)) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.76f, h * 0.26f),
                size = Size(w * 0.06f, h * 0.34f),
                cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.065f,
                center = Offset(w * 0.79f, h * 0.26f)
            )
        }

        // Bintang kilau kecil
        drawStar4(Offset(w * 0.20f, h * 0.34f), w * 0.05f, Color(0xFFFDE047))
        drawStar4(Offset(w * 0.84f, h * 0.70f), w * 0.04f, Color.White.copy(alpha = 0.85f))
    }
}

@Composable
fun EmergencyBeaconIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(44.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = 0.28f),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
        drawCircle(
            color = Color(0xFFDC2626),
            radius = w * 0.34f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        val bolt = Path().apply {
            moveTo(w * 0.54f, h * 0.24f)
            lineTo(w * 0.36f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.76f)
            lineTo(w * 0.65f, h * 0.46f)
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
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        val shield = Path().apply {
            moveTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.76f, h * 0.28f)
            lineTo(w * 0.76f, h * 0.50f)
            cubicTo(w * 0.76f, h * 0.68f, w * 0.58f, h * 0.80f, w * 0.50f, h * 0.84f)
            cubicTo(w * 0.42f, h * 0.80f, w * 0.24f, h * 0.68f, w * 0.24f, h * 0.50f)
            lineTo(w * 0.24f, h * 0.28f)
            close()
        }
        drawPath(path = shield, color = colors.success)

        val check = Path().apply {
            moveTo(w * 0.39f, h * 0.51f)
            lineTo(w * 0.47f, h * 0.60f)
            lineTo(w * 0.63f, h * 0.42f)
        }
        drawPath(
            path = check,
            color = if (colors.isDark) colors.background else Color.White,
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

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
            radius = w * 0.44f,
            center = Offset(w * 0.5f, h * 0.52f)
        )

        // Kartu / Kotak bersih
        drawRoundRect(
            color = colors.surface,
            topLeft = Offset(w * 0.26f, h * 0.28f),
            size = Size(w * 0.48f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(w * 0.26f, h * 0.28f),
            size = Size(w * 0.48f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.035f)
        )

        // Garis isi ringkas
        drawLine(
            color = colors.primary,
            start = Offset(w * 0.36f, h * 0.44f),
            end = Offset(w * 0.64f, h * 0.44f),
            strokeWidth = w * 0.045f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = colors.textMuted,
            start = Offset(w * 0.36f, h * 0.56f),
            end = Offset(w * 0.56f, h * 0.56f),
            strokeWidth = w * 0.04f,
            cap = StrokeCap.Round
        )

        drawStar4(Offset(w * 0.82f, h * 0.24f), w * 0.06f, colors.primary)
        drawStar4(Offset(w * 0.16f, h * 0.68f), w * 0.045f, colors.warning)
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
            radius = w * 0.44f,
            center = Offset(w * 0.5f, h * 0.52f)
        )

        val triangle = Path().apply {
            moveTo(w * 0.50f, h * 0.22f)
            lineTo(w * 0.78f, h * 0.74f)
            lineTo(w * 0.22f, h * 0.74f)
            close()
        }
        drawPath(
            path = triangle,
            color = colors.error,
            style = Stroke(width = w * 0.055f, join = StrokeJoin.Round)
        )

        drawLine(
            color = colors.error,
            start = Offset(w * 0.50f, h * 0.41f),
            end = Offset(w * 0.50f, h * 0.56f),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = colors.error,
            radius = w * 0.032f,
            center = Offset(w * 0.50f, h * 0.65f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar4(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path = path, color = color)
}
