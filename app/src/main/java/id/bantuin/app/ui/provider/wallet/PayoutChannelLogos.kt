package id.bantuin.app.ui.provider.wallet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.bantuin.app.ui.theme.BantuinTheme

@Composable
fun PayoutChannelLogo(
    channelName: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val normalized = channelName.trim().uppercase()
    val cornerRadius = (size.value * 0.26f).dp

    when (normalized) {
        "BCA" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color(0xFF00529C)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val ribbon = Path().apply {
                        moveTo(0f, this@Canvas.size.height * 0.78f)
                        lineTo(this@Canvas.size.width, this@Canvas.size.height * 0.54f)
                        lineTo(this@Canvas.size.width, this@Canvas.size.height * 0.70f)
                        lineTo(0f, this@Canvas.size.height * 0.94f)
                        close()
                    }
                    drawPath(ribbon, color = Color(0xFF3FA9F5).copy(alpha = 0.45f))
                }
                Text(
                    text = "BCA",
                    color = Color.White,
                    fontSize = (size.value * 0.30f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp
                )
            }
        }

        "BRI" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color(0xFF003B73)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRoundRect(
                        color = Color(0xFFF37021),
                        topLeft = Offset(this.size.width * 0.14f, this.size.height * 0.76f),
                        size = Size(this.size.width * 0.72f, this.size.height * 0.10f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
                Text(
                    text = "BRI",
                    color = Color.White,
                    fontSize = (size.value * 0.31f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                )
            }
        }

        "MANDIRI" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color(0xFF003D79)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val goldWave = Path().apply {
                        moveTo(this@Canvas.size.width * 0.12f, this@Canvas.size.height * 0.36f)
                        cubicTo(
                            this@Canvas.size.width * 0.36f,
                            this@Canvas.size.height * 0.14f,
                            this@Canvas.size.width * 0.64f,
                            this@Canvas.size.height * 0.48f,
                            this@Canvas.size.width * 0.88f,
                            this@Canvas.size.height * 0.24f
                        )
                    }
                    drawPath(
                        path = goldWave,
                        color = Color(0xFFFFB81C),
                        style = Stroke(width = this.size.minDimension * 0.11f, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "MDR",
                    color = Color.White,
                    fontSize = (size.value * 0.27f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        "DANA" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color(0xFF118EEA)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.56f)) {
                    val wave = Path().apply {
                        moveTo(0f, this@Canvas.size.height * 0.30f)
                        cubicTo(
                            this@Canvas.size.width * 0.32f,
                            0f,
                            this@Canvas.size.width * 0.68f,
                            this@Canvas.size.height * 0.55f,
                            this@Canvas.size.width,
                            this@Canvas.size.height * 0.25f
                        )
                        lineTo(this@Canvas.size.width, this@Canvas.size.height * 0.72f)
                        cubicTo(
                            this@Canvas.size.width * 0.68f,
                            this@Canvas.size.height,
                            this@Canvas.size.width * 0.32f,
                            this@Canvas.size.height * 0.45f,
                            0f,
                            this@Canvas.size.height * 0.78f
                        )
                        close()
                    }
                    drawPath(wave, color = Color.White)
                }
            }
        }

        "GOPAY" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color(0xFF00AED6)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.54f)) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(0f, this.size.height * 0.18f),
                        size = Size(this.size.width, this.size.height * 0.64f),
                        cornerRadius = CornerRadius(this.size.minDimension * 0.22f, this.size.minDimension * 0.22f)
                    )
                    drawCircle(
                        color = Color(0xFF00AED6),
                        radius = this.size.minDimension * 0.14f,
                        center = Offset(this.size.width * 0.72f, this.size.height * 0.50f)
                    )
                }
            }
        }

        "OVO" -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color(0xFF4C3494)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.48f)) {
                    drawCircle(
                        color = Color.White,
                        radius = this.size.minDimension * 0.42f,
                        style = Stroke(width = this.size.minDimension * 0.24f)
                    )
                }
            }
        }

        else -> {
            val colors = BantuinTheme.colors
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = channelName,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}

@Composable
fun WalletCreditLogo(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    val cornerRadius = (size.value * 0.26f).dp
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.successContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = "Dana Masuk QRIS",
            tint = colors.success,
            modifier = Modifier.size(size * 0.58f)
        )
    }
}
