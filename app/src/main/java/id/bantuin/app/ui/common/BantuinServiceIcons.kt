package id.bantuin.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.bantuin.app.ui.theme.BantuinMotion
import id.bantuin.app.ui.theme.BantuinTheme
import id.bantuin.app.ui.theme.rememberReducedMotion

fun serviceTagForCategory(categoryId: Int): String = when (categoryId) {
    1 -> "Populer"
    2 -> "Cepat"
    3 -> "Teruji"
    4 -> "Higienis"
    5 -> "Pindahan"
    6 -> "Saluran"
    else -> ""
}

/**
 * Mengembalikan pasangan (backgroundColor, textColor) ber-kontras WCAG AA (>= 4.5:1)
 * untuk pill tag di sudut ikon kategori.
 */
fun serviceTagColors(categoryId: Int, isDark: Boolean): Pair<Color, Color> {
    return if (isDark) {
        when (categoryId) {
            1 -> Color(0xFF3E2208) to Color(0xFFFFB74D)
            2 -> Color(0xFF0E2B42) to Color(0xFF7DD3FC)
            3 -> Color(0xFF3D1F10) to Color(0xFFFDBA74)
            4 -> Color(0xFF0F3020) to Color(0xFF6EE7B7)
            5 -> Color(0xFF3A280A) to Color(0xFFFDE68A)
            6 -> Color(0xFF0D2E38) to Color(0xFF67E8F9)
            else -> Color(0xFF2B2E33) to Color(0xFFE5E7EB)
        }
    } else {
        when (categoryId) {
            1 -> Color(0xFFC43E00) to Color.White // 5.20:1
            2 -> Color(0xFF0369A1) to Color.White // 5.92:1
            3 -> Color(0xFF9A3412) to Color.White // 7.31:1
            4 -> Color(0xFF15803D) to Color.White // 5.13:1
            5 -> Color(0xFFB45309) to Color.White // 5.02:1
            6 -> Color(0xFF0E7490) to Color.White // 5.36:1
            else -> Color(0xFF475569) to Color.White
        }
    }
}

@Composable
fun BantuinServiceIcon(
    categoryId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 18.dp,
    isPressed: Boolean = false
) {
    val isDark = BantuinTheme.colors.isDark
    val reducedMotion = rememberReducedMotion()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else BantuinMotion.bouncySpring(),
        label = "serviceIconPress"
    )

    val containerColor = when (categoryId) {
        1 -> if (isDark) Color(0xFF33210D) else Color(0xFFFFEFE0)
        2 -> if (isDark) Color(0xFF112638) else Color(0xFFE0F2FE)
        3 -> if (isDark) Color(0xFF311D12) else Color(0xFFFFEAD8)
        4 -> if (isDark) Color(0xFF112A1F) else Color(0xFFDCFCE7)
        5 -> if (isDark) Color(0xFF2E220E) else Color(0xFFFEF3C7)
        6 -> if (isDark) Color(0xFF0F2830) else Color(0xFFE0F7FA)
        else -> if (isDark) Color(0xFF24272C) else Color(0xFFF1F5F9)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .padding((size.value * 0.14f).dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (categoryId) {
                1 -> drawElectricityIcon(isDark, pressProgress)
                2 -> drawAirConditionerIcon(isDark, pressProgress)
                3 -> drawConstructionIcon(isDark, pressProgress)
                4 -> drawCleaningIcon(isDark, pressProgress)
                5 -> drawMovingBoxIcon(isDark, pressProgress)
                6 -> drawPlumbingPumpIcon(isDark, pressProgress)
                else -> drawConstructionIcon(isDark, pressProgress)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawElectricityIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val halo = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.22f) else Color(0xFFFDE68A)
    drawCircle(
        color = halo,
        radius = w * (0.38f + pressProgress * 0.05f),
        center = Offset(w * 0.5f, h * 0.5f)
    )

    // Steker bawah halus
    val plugColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    drawLine(
        color = plugColor,
        start = Offset(w * 0.5f, h * 0.78f),
        end = Offset(w * 0.5f, h * 0.94f),
        strokeWidth = w * 0.08f,
        cap = StrokeCap.Round
    )

    // Kilat oranye-emas bertumpuk
    val boltColor = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
    val boltHighlight = Color(0xFFFACC15)

    rotate(degrees = pressProgress * -8f, pivot = Offset(w * 0.5f, h * 0.5f)) {
        val bolt = Path().apply {
            moveTo(w * 0.56f, h * 0.08f)
            lineTo(w * 0.24f, h * 0.52f)
            lineTo(w * 0.48f, h * 0.52f)
            lineTo(w * 0.40f, h * 0.84f)
            lineTo(w * 0.76f, h * 0.40f)
            lineTo(w * 0.52f, h * 0.40f)
            close()
        }
        drawPath(path = bolt, color = boltColor)

        val innerSpark = Path().apply {
            moveTo(w * 0.54f, h * 0.17f)
            lineTo(w * 0.35f, h * 0.46f)
            lineTo(w * 0.50f, h * 0.46f)
            lineTo(w * 0.45f, h * 0.68f)
            lineTo(w * 0.65f, h * 0.44f)
            lineTo(w * 0.50f, h * 0.44f)
            close()
        }
        drawPath(path = innerSpark, color = boltHighlight)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAirConditionerIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val bodyColor = if (isDark) Color(0xFFE2E8F0) else Color.White
    val bodyStroke = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val ventColor = Color(0xFF0369A1)
    val windColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0EA5E9)

    // Unit indoor AC
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(w * 0.08f, h * 0.16f),
        size = Size(w * 0.84f, h * 0.40f),
        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
    )
    drawRoundRect(
        color = bodyStroke,
        topLeft = Offset(w * 0.08f, h * 0.16f),
        size = Size(w * 0.84f, h * 0.40f),
        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
        style = Stroke(width = w * 0.055f)
    )

    // Kisi hembusan AC & indikator hijau
    drawRoundRect(
        color = ventColor,
        topLeft = Offset(w * 0.18f, h * 0.42f),
        size = Size(w * 0.64f, h * 0.07f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    drawCircle(
        color = Color(0xFF10B981),
        radius = w * 0.04f,
        center = Offset(w * 0.80f, h * 0.27f)
    )

    // Aliran udara sejuk bergerak halus saat ditekan
    translate(top = pressProgress * h * 0.06f) {
        val waveOffsets = listOf(0.28f, 0.50f, 0.72f)
        waveOffsets.forEachIndexed { idx, xFrac ->
            val wave = Path().apply {
                val startX = w * xFrac
                moveTo(startX, h * 0.63f)
                cubicTo(
                    startX - w * 0.06f, h * 0.72f,
                    startX + w * 0.06f, h * 0.80f,
                    startX - w * 0.02f, h * 0.90f
                )
            }
            drawPath(
                path = wave,
                color = if (idx == 1) Color(0xFF06B6D4) else windColor,
                style = Stroke(width = w * 0.058f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConstructionIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val metalColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val helmetBody = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
    val helmetDark = if (isDark) Color(0xFFEA580C) else Color(0xFF9A3412)
    val helmetHighlight = Color(0xFFFED7AA)

    // Kunci pas di belakang
    rotate(degrees = -32f + pressProgress * 10f, pivot = Offset(w * 0.5f, h * 0.55f)) {
        drawRoundRect(
            color = metalColor,
            topLeft = Offset(w * 0.43f, h * 0.14f),
            size = Size(w * 0.14f, h * 0.72f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawCircle(
            color = metalColor,
            radius = w * 0.14f,
            center = Offset(w * 0.50f, h * 0.79f)
        )
    }

    // Helm proyek
    val dome = Path().apply {
        moveTo(w * 0.16f, h * 0.62f)
        cubicTo(w * 0.16f, h * 0.24f, w * 0.84f, h * 0.24f, w * 0.84f, h * 0.62f)
        close()
    }
    drawPath(path = dome, color = helmetBody)

    // Ridge tengah helm
    drawRoundRect(
        color = helmetHighlight,
        topLeft = Offset(w * 0.44f, h * 0.27f),
        size = Size(w * 0.12f, h * 0.28f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
    )

    // Pinggiran (brim) helm
    drawRoundRect(
        color = helmetDark,
        topLeft = Offset(w * 0.10f, h * 0.58f),
        size = Size(w * 0.80f, h * 0.11f),
        cornerRadius = CornerRadius(w * 0.055f, w * 0.055f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCleaningIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val bottleColor = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A)
    val bottleCap = if (isDark) Color(0xFF0EA5E9) else Color(0xFF0F766E)
    val sparkleColor = Color(0xFFF59E0B)

    // Botol semprot higienis
    rotate(degrees = -10f + pressProgress * 8f, pivot = Offset(w * 0.45f, h * 0.55f)) {
        // Leher & trigger
        drawRoundRect(
            color = bottleCap,
            topLeft = Offset(w * 0.34f, h * 0.16f),
            size = Size(w * 0.30f, h * 0.11f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        drawLine(
            color = bottleCap,
            start = Offset(w * 0.56f, h * 0.25f),
            end = Offset(w * 0.68f, h * 0.34f),
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Round
        )
        // Badan botol
        val body = Path().apply {
            moveTo(w * 0.36f, h * 0.30f)
            lineTo(w * 0.52f, h * 0.30f)
            lineTo(w * 0.62f, h * 0.52f)
            lineTo(w * 0.62f, h * 0.86f)
            lineTo(w * 0.26f, h * 0.86f)
            lineTo(w * 0.26f, h * 0.52f)
            close()
        }
        drawPath(path = body, color = bottleColor)

        // Label botol terang
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.33f, h * 0.54f),
            size = Size(w * 0.22f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
    }

    // Kilau 4 titik
    val starScale = 1f + pressProgress * 0.25f
    drawSparkleStar(
        center = Offset(w * 0.78f, h * 0.28f),
        radius = w * 0.11f * starScale,
        color = sparkleColor
    )
    drawSparkleStar(
        center = Offset(w * 0.80f, h * 0.64f),
        radius = w * 0.08f * starScale,
        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF0D9488)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMovingBoxIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val boxFront = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    val boxTop = if (isDark) Color(0xFFFCD34D) else Color(0xFFF59E0B)
    val tape = if (isDark) Color(0xFF78350F) else Color(0xFF92400E)
    val speedLine = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)

    // Garis gerak cepat di kiri
    drawLine(
        color = speedLine,
        start = Offset(w * 0.08f, h * 0.38f),
        end = Offset(w * 0.24f, h * 0.38f),
        strokeWidth = w * 0.055f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = speedLine,
        start = Offset(w * 0.04f, h * 0.54f),
        end = Offset(w * 0.22f, h * 0.54f),
        strokeWidth = w * 0.055f,
        cap = StrokeCap.Round
    )

    translate(left = pressProgress * w * 0.06f) {
        // Sisi atas boks
        val topFlap = Path().apply {
            moveTo(w * 0.28f, h * 0.34f)
            lineTo(w * 0.42f, h * 0.20f)
            lineTo(w * 0.88f, h * 0.20f)
            lineTo(w * 0.78f, h * 0.34f)
            close()
        }
        drawPath(path = topFlap, color = boxTop)

        // Badan boks
        drawRoundRect(
            color = boxFront,
            topLeft = Offset(w * 0.28f, h * 0.34f),
            size = Size(w * 0.54f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )

        // Lakban vertikal
        drawRect(
            color = tape,
            topLeft = Offset(w * 0.50f, h * 0.34f),
            size = Size(w * 0.11f, h * 0.18f)
        )

        // Roda troli kecil di bawah
        val wheelColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
        drawCircle(color = wheelColor, radius = w * 0.075f, center = Offset(w * 0.38f, h * 0.84f))
        drawCircle(color = wheelColor, radius = w * 0.075f, center = Offset(w * 0.72f, h * 0.84f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlumbingPumpIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val pipeMain = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val pipeFlange = if (isDark) Color(0xFF0284C7) else Color(0xFF0369A1)
    val dropColor = if (isDark) Color(0xFF67E8F9) else Color(0xFF06B6D4)

    // Pipa siku L
    val pipePath = Path().apply {
        moveTo(w * 0.18f, h * 0.36f)
        lineTo(w * 0.54f, h * 0.36f)
        quadraticTo(w * 0.68f, h * 0.36f, w * 0.68f, h * 0.52f)
        lineTo(w * 0.68f, h * 0.64f)
    }
    drawPath(
        path = pipePath,
        color = pipeMain,
        style = Stroke(width = w * 0.20f, cap = StrokeCap.Butt, join = StrokeJoin.Round)
    )

    // Flensa / sambungan pipa kiri & ujung keran
    drawRoundRect(
        color = pipeFlange,
        topLeft = Offset(w * 0.12f, h * 0.22f),
        size = Size(w * 0.11f, h * 0.28f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    drawRoundRect(
        color = pipeFlange,
        topLeft = Offset(w * 0.54f, h * 0.58f),
        size = Size(w * 0.28f, h * 0.11f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )

    // Tuas katup merah-oranye di atas pipa
    drawLine(
        color = Color(0xFFEA580C),
        start = Offset(w * 0.40f, h * 0.26f),
        end = Offset(w * 0.40f, h * 0.16f),
        strokeWidth = w * 0.06f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFEA580C),
        start = Offset(w * 0.28f, h * 0.16f),
        end = Offset(w * 0.52f, h * 0.16f),
        strokeWidth = w * 0.065f,
        cap = StrokeCap.Round
    )

    // Tetesan air jernih memantul saat ditekan
    translate(top = pressProgress * h * 0.05f) {
        val drop = Path().apply {
            moveTo(w * 0.68f, h * 0.72f)
            cubicTo(w * 0.54f, h * 0.84f, w * 0.58f, h * 0.96f, w * 0.68f, h * 0.96f)
            cubicTo(w * 0.78f, h * 0.96f, w * 0.82f, h * 0.84f, w * 0.68f, h * 0.72f)
            close()
        }
        drawPath(path = drop, color = dropColor)
        drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            radius = w * 0.025f,
            center = Offset(w * 0.65f, h * 0.86f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkleStar(
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
