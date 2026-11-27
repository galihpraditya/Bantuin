package id.bantuin.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.bantuin.app.ui.theme.BantuinMotion
import id.bantuin.app.ui.theme.BantuinTheme
import id.bantuin.app.ui.theme.rememberReducedMotion

/**
 * Ikon layanan soft duotone tanpa garis luar (outline-free) di atas wadah palet hangat Bantuin.
 * Kontras tetap >= 4.60:1 melalui bidang isi penuh dua-nada dan cutout terang di dalamnya (R-25, R-29, R-31).
 */
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

    // Wadah seragam sesuai palet hangat aplikasi (R-29) dengan garis tepi lembut
    val containerColor = if (isDark) Color(0xFF2A1F14) else Color(0xFFFFF3E8)
    val borderColor = if (isDark) Color(0xFF44301E) else Color(0xFFF1DAC6)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), shape)
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

/**
 * Teknisi Listrik: steker terracotta penuh tanpa outline dengan pin baja dan lambang kilat putih kontras tinggi.
 */
private fun DrawScope.drawElectricityIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val metal = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val plugBody = if (isDark) Color(0xFFFB923C) else Color(0xFFC43E00)
    val collar = if (isDark) Color(0xFFEA580C) else Color(0xFF9A3412)
    val boltCutout = if (isDark) Color(0xFF1C1E22) else Color.White

    translate(top = pressProgress * h * 0.04f) {
        // Dua pin logam steker
        drawLine(
            color = metal,
            start = Offset(w * 0.39f, h * 0.08f),
            end = Offset(w * 0.39f, h * 0.27f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = metal,
            start = Offset(w * 0.61f, h * 0.08f),
            end = Offset(w * 0.61f, h * 0.27f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )

        // Kabel daya melengkung lembut
        val cord = Path().apply {
            moveTo(w * 0.50f, h * 0.72f)
            cubicTo(w * 0.50f, h * 0.89f, w * 0.36f, h * 0.90f, w * 0.20f, h * 0.88f)
        }
        drawPath(
            path = cord,
            color = metal,
            style = Stroke(width = w * 0.072f, cap = StrokeCap.Round)
        )

        // Kerah atas steker
        drawRoundRect(
            color = collar,
            topLeft = Offset(w * 0.26f, h * 0.23f),
            size = Size(w * 0.48f, h * 0.13f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )

        // Badan steker berwarna penuh (4.77:1 pada #FFF3E8)
        drawRoundRect(
            color = plugBody,
            topLeft = Offset(w * 0.20f, h * 0.31f),
            size = Size(w * 0.60f, h * 0.43f),
            cornerRadius = CornerRadius(w * 0.13f, w * 0.13f)
        )

        // Potongan kilat putih cerah di tengah badan steker (5.20:1 pada #C43E00)
        val bolt = Path().apply {
            moveTo(w * 0.55f, h * 0.37f)
            lineTo(w * 0.37f, h * 0.54f)
            lineTo(w * 0.49f, h * 0.54f)
            lineTo(w * 0.45f, h * 0.69f)
            lineTo(w * 0.63f, h * 0.50f)
            lineTo(w * 0.51f, h * 0.50f)
            close()
        }
        drawPath(path = bolt, color = boltCutout)
    }
}

/**
 * Teknisi AC & Elektronik: unit indoor cobalt dua-nada tanpa outline, kisi putih bersih, dan aliran angin lembut.
 */
private fun DrawScope.drawAirConditionerIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val unitBody = if (isDark) Color(0xFF0284C7) else Color(0xFF0369A1)
    val unitShade = if (isDark) Color(0xFF0369A1) else Color(0xFF075985)
    val topAccent = if (isDark) Color(0xFF7DD3FC) else Color(0xFFBAE6FD)
    val louverWhite = if (isDark) Color(0xFF1C1E22) else Color.White
    val windColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val ledGreen = if (isDark) Color(0xFF6EE7B7) else Color(0xFF34D399)

    // Badan unit indoor AC solid dua-nada (5.44:1 pada #FFF3E8)
    drawRoundRect(
        color = unitBody,
        topLeft = Offset(w * 0.08f, h * 0.15f),
        size = Size(w * 0.84f, h * 0.39f),
        cornerRadius = CornerRadius(w * 0.11f, w * 0.11f)
    )
    // Bagian bawah unit sedikit lebih gelap memberi kedalaman
    drawRoundRect(
        color = unitShade,
        topLeft = Offset(w * 0.08f, h * 0.39f),
        size = Size(w * 0.84f, h * 0.15f),
        cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
    )

    // Garis aksen panel depan & indikator LED
    drawLine(
        color = topAccent.copy(alpha = 0.80f),
        start = Offset(w * 0.18f, h * 0.25f),
        end = Offset(w * 0.62f, h * 0.25f),
        strokeWidth = w * 0.036f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = ledGreen,
        radius = w * 0.042f,
        center = Offset(w * 0.79f, h * 0.25f)
    )

    // Kisi hembusan AC putih kontras tinggi (5.93:1 pada #0369A1)
    drawRoundRect(
        color = louverWhite,
        topLeft = Offset(w * 0.17f, h * 0.41f),
        size = Size(w * 0.66f, h * 0.075f),
        cornerRadius = CornerRadius(w * 0.038f, w * 0.038f)
    )

    // Aliran udara sejuk
    translate(top = pressProgress * h * 0.05f) {
        val waveOffsets = listOf(0.28f, 0.50f, 0.72f)
        waveOffsets.forEach { xFrac ->
            val wave = Path().apply {
                val startX = w * xFrac
                moveTo(startX, h * 0.62f)
                cubicTo(
                    startX - w * 0.06f, h * 0.71f,
                    startX + w * 0.06f, h * 0.80f,
                    startX - w * 0.02f, h * 0.89f
                )
            }
            drawPath(
                path = wave,
                color = windColor,
                style = Stroke(width = w * 0.058f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Tukang Bangunan: kunci pas baja ber-rahang U di belakang helm proyek terracotta dua-nada tanpa outline.
 */
private fun DrawScope.drawConstructionIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val metalFill = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val helmetFill = if (isDark) Color(0xFFFB923C) else Color(0xFFC43E00)
    val helmetBrim = if (isDark) Color(0xFFEA580C) else Color(0xFF9A3412)
    val crestFill = if (isDark) Color(0xFFFDE68A) else Color(0xFFFFE0B2)

    // Kunci pas baja ber-rahang U (6.94:1 pada #FFF3E8)
    rotate(degrees = 36f + pressProgress * 8f, pivot = Offset(w * 0.52f, h * 0.50f)) {
        val wrench = Path().apply {
            moveTo(w * 0.445f, h * 0.05f)
            lineTo(w * 0.445f, h * 0.17f)
            lineTo(w * 0.555f, h * 0.17f)
            lineTo(w * 0.555f, h * 0.05f)
            cubicTo(w * 0.66f, h * 0.07f, w * 0.67f, h * 0.23f, w * 0.57f, h * 0.28f)
            lineTo(w * 0.57f, h * 0.84f)
            cubicTo(w * 0.57f, h * 0.91f, w * 0.43f, h * 0.91f, w * 0.43f, h * 0.84f)
            lineTo(w * 0.43f, h * 0.28f)
            cubicTo(w * 0.33f, h * 0.23f, w * 0.34f, h * 0.07f, w * 0.445f, h * 0.05f)
            close()
        }
        drawPath(path = wrench, color = metalFill)
    }

    // Helm proyek dua-nada di depan-kiri
    val dome = Path().apply {
        moveTo(w * 0.13f, h * 0.65f)
        cubicTo(w * 0.13f, h * 0.27f, w * 0.79f, h * 0.27f, w * 0.79f, h * 0.65f)
        close()
    }
    drawPath(path = dome, color = helmetFill)

    // Bubungan lembut di tengah helm
    drawRoundRect(
        color = crestFill,
        topLeft = Offset(w * 0.405f, h * 0.33f),
        size = Size(w * 0.11f, h * 0.25f),
        cornerRadius = CornerRadius(w * 0.055f, w * 0.055f)
    )

    // Pinggiran (brim) helm lebih gelap untuk pemisahan bentuk yang lembut
    drawRoundRect(
        color = helmetBrim,
        topLeft = Offset(w * 0.07f, h * 0.61f),
        size = Size(w * 0.78f, h * 0.12f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
    )
}

/**
 * Jasa Kebersihan: botol semprot teal dua-nada tanpa outline, label lembut, dan semburan kabut biru.
 */
private fun DrawScope.drawCleaningIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val bottleFill = if (isDark) Color(0xFF10B981) else Color(0xFF0F766E)
    val headFill = if (isDark) Color(0xFF34D399) else Color(0xFF0D9488)
    val labelFill = if (isDark) Color(0xFF1C1E22) else Color.White
    val triggerAccent = if (isDark) Color(0xFFFB923C) else Color(0xFFC43E00)
    val mistColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)

    // Tuas semprot
    val triggerShift = -pressProgress * w * 0.03f
    drawLine(
        color = triggerAccent,
        start = Offset(w * 0.48f + triggerShift, h * 0.29f),
        end = Offset(w * 0.59f + triggerShift, h * 0.40f),
        strokeWidth = w * 0.065f,
        cap = StrokeCap.Round
    )

    // Kepala semprot & nosel lebih cerah agar terpisah dari leher botol
    val sprayHead = Path().apply {
        moveTo(w * 0.27f, h * 0.16f)
        lineTo(w * 0.58f, h * 0.16f)
        lineTo(w * 0.65f, h * 0.23f)
        lineTo(w * 0.58f, h * 0.29f)
        lineTo(w * 0.29f, h * 0.29f)
        close()
    }
    drawPath(path = sprayHead, color = headFill)

    // Badan botol melengkung halus (5.01:1 pada #FFF3E8)
    val body = Path().apply {
        moveTo(w * 0.31f, h * 0.30f)
        lineTo(w * 0.47f, h * 0.30f)
        cubicTo(w * 0.49f, h * 0.42f, w * 0.58f, h * 0.44f, w * 0.58f, h * 0.56f)
        lineTo(w * 0.58f, h * 0.84f)
        cubicTo(w * 0.58f, h * 0.89f, w * 0.54f, h * 0.91f, w * 0.49f, h * 0.91f)
        lineTo(w * 0.26f, h * 0.91f)
        cubicTo(w * 0.21f, h * 0.91f, w * 0.18f, h * 0.89f, w * 0.18f, h * 0.84f)
        lineTo(w * 0.18f, h * 0.56f)
        cubicTo(w * 0.18f, h * 0.44f, w * 0.29f, h * 0.42f, w * 0.31f, h * 0.30f)
        close()
    }
    drawPath(path = body, color = bottleFill)

    // Label putih bersih di badan botol
    drawRoundRect(
        color = labelFill,
        topLeft = Offset(w * 0.25f, h * 0.55f),
        size = Size(w * 0.26f, h * 0.23f),
        cornerRadius = CornerRadius(w * 0.055f, w * 0.055f)
    )

    // Semburan kabut / butiran air terarah dari nosel
    translate(left = pressProgress * w * 0.04f) {
        drawLine(
            color = mistColor,
            start = Offset(w * 0.70f, h * 0.19f),
            end = Offset(w * 0.82f, h * 0.14f),
            strokeWidth = w * 0.052f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = mistColor,
            start = Offset(w * 0.71f, h * 0.25f),
            end = Offset(w * 0.88f, h * 0.25f),
            strokeWidth = w * 0.052f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = mistColor,
            start = Offset(w * 0.70f, h * 0.31f),
            end = Offset(w * 0.82f, h * 0.36f),
            strokeWidth = w * 0.052f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Angkut Barang: kardus amber-cokelat dua-nada di atas troli baja L-frame beroda tanpa outline.
 */
private fun DrawScope.drawMovingBoxIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val frameSteel = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val boxFront = if (isDark) Color(0xFFF59E0B) else Color(0xFFB45309)
    val boxTop = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    val tapeAndLabel = if (isDark) Color(0xFF1C1E22) else Color(0xFFFFF3E8)
    val hubColor = if (isDark) Color(0xFF2A1F14) else Color(0xFFFFF3E8)

    translate(left = pressProgress * w * 0.04f) {
        // Rangka troli baja L-frame
        val frame = Path().apply {
            moveTo(w * 0.10f, h * 0.16f)
            lineTo(w * 0.20f, h * 0.16f)
            lineTo(w * 0.26f, h * 0.76f)
            lineTo(w * 0.88f, h * 0.76f)
        }
        drawPath(
            path = frame,
            color = frameSteel,
            style = Stroke(width = w * 0.068f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Kardus paket duduk rapi di atas alas troli (4.60:1 pada #FFF3E8)
        drawRoundRect(
            color = boxFront,
            topLeft = Offset(w * 0.32f, h * 0.24f),
            size = Size(w * 0.52f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
        )
        drawRoundRect(
            color = boxTop,
            topLeft = Offset(w * 0.32f, h * 0.24f),
            size = Size(w * 0.52f, h * 0.13f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawRect(
            color = tapeAndLabel,
            topLeft = Offset(w * 0.52f, h * 0.24f),
            size = Size(w * 0.12f, h * 0.18f)
        )

        // Label pengiriman kecil lembut di sudut kanan bawah kardus
        drawRoundRect(
            color = tapeAndLabel,
            topLeft = Offset(w * 0.62f, h * 0.52f),
            size = Size(w * 0.15f, h * 0.11f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        // Dua roda troli
        val wheelCenters = listOf(w * 0.34f, w * 0.74f)
        wheelCenters.forEach { cx ->
            drawCircle(
                color = frameSteel,
                radius = w * 0.078f,
                center = Offset(cx, h * 0.84f)
            )
            drawCircle(
                color = hubColor,
                radius = w * 0.032f,
                center = Offset(cx, h * 0.84f)
            )
        }
    }
}

/**
 * Pipa & Pompa Air: pipa siku cobalt berdimensi, flensa baja, katup terracotta, dan tetesan air tanpa outline.
 */
private fun DrawScope.drawPlumbingPumpIcon(
    isDark: Boolean,
    pressProgress: Float
) {
    val w = size.width
    val h = size.height
    val pipeFill = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
    val pipeHighlight = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0284C7)
    val flangeSteel = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val valveAccent = if (isDark) Color(0xFFFB923C) else Color(0xFFC43E00)

    // Pipa siku L solid + sorotan halus di sisi atas agar berdimensi tanpa outline
    val pipePath = Path().apply {
        moveTo(w * 0.18f, h * 0.38f)
        lineTo(w * 0.54f, h * 0.38f)
        quadraticTo(w * 0.66f, h * 0.38f, w * 0.66f, h * 0.50f)
        lineTo(w * 0.66f, h * 0.61f)
    }
    drawPath(
        path = pipePath,
        color = pipeFill,
        style = Stroke(width = w * 0.20f, cap = StrokeCap.Butt, join = StrokeJoin.Round)
    )
    drawPath(
        path = pipePath,
        color = pipeHighlight,
        style = Stroke(width = w * 0.07f, cap = StrokeCap.Butt, join = StrokeJoin.Round)
    )

    // Flensa baja kiri & ujung keran (kontras tinggi terhadap pipa cobalt & latar hangat)
    drawRoundRect(
        color = flangeSteel,
        topLeft = Offset(w * 0.11f, h * 0.22f),
        size = Size(w * 0.11f, h * 0.32f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
    drawRoundRect(
        color = flangeSteel,
        topLeft = Offset(w * 0.52f, h * 0.54f),
        size = Size(w * 0.28f, h * 0.11f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )

    // Batang baja & tuas katup terracotta
    drawLine(
        color = flangeSteel,
        start = Offset(w * 0.38f, h * 0.29f),
        end = Offset(w * 0.38f, h * 0.16f),
        strokeWidth = w * 0.068f,
        cap = StrokeCap.Round
    )
    drawRoundRect(
        color = valveAccent,
        topLeft = Offset(w * 0.22f, h * 0.10f),
        size = Size(w * 0.32f, h * 0.09f),
        cornerRadius = CornerRadius(w * 0.045f, w * 0.045f)
    )

    // Tetesan air bersih kontras 5.44:1
    translate(top = pressProgress * h * 0.04f) {
        val drop = Path().apply {
            moveTo(w * 0.66f, h * 0.70f)
            cubicTo(w * 0.52f, h * 0.82f, w * 0.56f, h * 0.94f, w * 0.66f, h * 0.94f)
            cubicTo(w * 0.76f, h * 0.94f, w * 0.80f, h * 0.82f, w * 0.66f, h * 0.70f)
            close()
        }
        drawPath(path = drop, color = pipeFill)
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = w * 0.028f,
            center = Offset(w * 0.63f, h * 0.85f)
        )
    }
}
