package id.bantuin.app.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Set ikon UI kustom Bantuin dengan ketebalan garis konsisten (2.0f) dan sudut membulat hangat,
 * menyatukan bahasa visual header serta bottom navigation dengan ikon layanan (R-04, R-20).
 */
object BantuinUiIcons {

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinSearch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Lensa pembesar: dua setengah-lingkaran 180 derajat (isMoreThanHalf = false)
                moveTo(10.5f, 4.0f)
                arcTo(6.5f, 6.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 10.5f, 17.0f)
                arcTo(6.5f, 6.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 10.5f, 4.0f)
                // Tangkai diagonal
                moveTo(15.2f, 15.2f)
                lineTo(20.0f, 20.0f)
            }
        }.build()
    }

    val NotificationBell: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinBell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f
            ) {
                moveTo(12f, 2.5f)
                curveTo(11.3f, 2.5f, 10.8f, 3.0f, 10.8f, 3.7f)
                lineTo(10.8f, 4.3f)
                curveTo(7.9f, 4.8f, 5.8f, 7.3f, 5.8f, 10.3f)
                lineTo(5.8f, 14.8f)
                lineTo(4.2f, 16.8f)
                curveTo(3.9f, 17.2f, 4.2f, 17.8f, 4.7f, 17.8f)
                lineTo(19.3f, 17.8f)
                curveTo(19.8f, 17.8f, 20.1f, 17.2f, 19.8f, 16.8f)
                lineTo(18.2f, 14.8f)
                lineTo(18.2f, 10.3f)
                curveTo(18.2f, 7.3f, 16.1f, 4.8f, 13.2f, 4.3f)
                lineTo(13.2f, 3.7f)
                curveTo(13.2f, 3.0f, 12.7f, 2.5f, 12f, 2.5f)
                close()
                // Genta bawah lonceng
                moveTo(9.8f, 19.0f)
                curveTo(10.2f, 20.2f, 11.0f, 21.0f, 12f, 21.0f)
                curveTo(13.0f, 21.0f, 13.8f, 20.2f, 14.2f, 19.0f)
                close()
            }
        }.build()
    }

    val LocationPin: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinLocationPin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(12f, 2.5f)
                curveTo(7.86f, 2.5f, 4.5f, 5.86f, 4.5f, 10f)
                curveTo(4.5f, 15.2f, 12f, 21.5f, 12f, 21.5f)
                curveTo(12f, 21.5f, 19.5f, 15.2f, 19.5f, 10f)
                curveTo(19.5f, 5.86f, 16.14f, 2.5f, 12f, 2.5f)
                close()
                // Lubang tengah pin (EvenOdd)
                moveTo(12f, 7.2f)
                arcTo(2.8f, 2.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 12.8f)
                arcTo(2.8f, 2.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 7.2f)
                close()
            }
        }.build()
    }

    val ChevronDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinChevronDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 9.5f)
                lineTo(12f, 15f)
                lineTo(17.5f, 9.5f)
            }
        }.build()
    }

    val HomeOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinHomeOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.5f, 10.5f)
                lineTo(12f, 3.5f)
                lineTo(20.5f, 10.5f)
                moveTo(5.5f, 9.5f)
                lineTo(5.5f, 19.5f)
                curveTo(5.5f, 20.1f, 5.9f, 20.5f, 6.5f, 20.5f)
                lineTo(17.5f, 20.5f)
                curveTo(18.1f, 20.5f, 18.5f, 20.1f, 18.5f, 19.5f)
                lineTo(18.5f, 9.5f)
                moveTo(9.8f, 20.5f)
                lineTo(9.8f, 14.5f)
                curveTo(9.8f, 13.9f, 10.2f, 13.5f, 10.8f, 13.5f)
                lineTo(13.2f, 13.5f)
                curveTo(13.8f, 13.5f, 14.2f, 13.9f, 14.2f, 14.5f)
                lineTo(14.2f, 20.5f)
            }
        }.build()
    }

    val HomeFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinHomeFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(12f, 3.2f)
                lineTo(3.2f, 10.4f)
                curveTo(2.9f, 10.7f, 3.1f, 11.2f, 3.5f, 11.2f)
                lineTo(5.2f, 11.2f)
                lineTo(5.2f, 19.5f)
                curveTo(5.2f, 20.3f, 5.9f, 21f, 6.7f, 21f)
                lineTo(17.3f, 21f)
                curveTo(18.1f, 21f, 18.8f, 20.3f, 18.8f, 19.5f)
                lineTo(18.8f, 11.2f)
                lineTo(20.5f, 11.2f)
                curveTo(20.9f, 11.2f, 21.1f, 10.7f, 20.8f, 10.4f)
                close()
                // Pintu rumah (EvenOdd)
                moveTo(10f, 14f)
                lineTo(14f, 14f)
                lineTo(14f, 19f)
                lineTo(10f, 19f)
                close()
            }
        }.build()
    }

    val OrdersOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinOrdersOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Lembar pesanan membulat selaras dengan bobot ikon Beranda & Chat
                moveTo(7f, 4f)
                lineTo(17f, 4f)
                curveTo(18.1f, 4f, 19f, 4.9f, 19f, 6f)
                lineTo(19f, 18f)
                curveTo(19f, 19.1f, 18.1f, 20f, 17f, 20f)
                lineTo(7f, 20f)
                curveTo(5.9f, 20f, 5f, 19.1f, 5f, 18f)
                lineTo(5f, 6f)
                curveTo(5f, 4.9f, 5.9f, 4f, 7f, 4f)
                close()
                moveTo(8.5f, 9f)
                lineTo(15.5f, 9f)
                moveTo(8.5f, 12.8f)
                lineTo(15.5f, 12.8f)
                moveTo(8.5f, 16.4f)
                lineTo(12.5f, 16.4f)
            }
        }.build()
    }

    val OrdersFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinOrdersFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(7f, 3.5f)
                lineTo(17f, 3.5f)
                curveTo(18.4f, 3.5f, 19.5f, 4.6f, 19.5f, 6f)
                lineTo(19.5f, 18f)
                curveTo(19.5f, 19.4f, 18.4f, 20.5f, 17f, 20.5f)
                lineTo(7f, 20.5f)
                curveTo(5.6f, 20.5f, 4.5f, 19.4f, 4.5f, 18f)
                lineTo(4.5f, 6f)
                curveTo(4.5f, 4.6f, 5.6f, 3.5f, 7f, 3.5f)
                close()
                // Tiga baris daftar pesanan (EvenOdd)
                moveTo(8f, 8.2f)
                lineTo(16f, 8.2f)
                lineTo(16f, 9.8f)
                lineTo(8f, 9.8f)
                close()
                moveTo(8f, 11.8f)
                lineTo(16f, 11.8f)
                lineTo(16f, 13.4f)
                lineTo(8f, 13.4f)
                close()
                moveTo(8f, 15.4f)
                lineTo(12.8f, 15.4f)
                lineTo(12.8f, 17.0f)
                lineTo(8f, 17.0f)
                close()
            }
        }.build()
    }

    val ChatOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinChatOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.5f, 4.5f)
                lineTo(18.5f, 4.5f)
                curveTo(19.6f, 4.5f, 20.5f, 5.4f, 20.5f, 6.5f)
                lineTo(20.5f, 15.5f)
                curveTo(20.5f, 16.6f, 19.6f, 17.5f, 18.5f, 17.5f)
                lineTo(9.2f, 17.5f)
                lineTo(4.5f, 20.5f)
                lineTo(4.5f, 17.5f)
                curveTo(3.9f, 17.1f, 3.5f, 16.4f, 3.5f, 15.5f)
                lineTo(3.5f, 6.5f)
                curveTo(3.5f, 5.4f, 4.4f, 4.5f, 5.5f, 4.5f)
                close()
                moveTo(8f, 9.5f)
                lineTo(16f, 9.5f)
                moveTo(8f, 13f)
                lineTo(13.2f, 13f)
            }
        }.build()
    }

    val ChatFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinChatFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(5.5f, 4f)
                lineTo(18.5f, 4f)
                curveTo(19.9f, 4f, 21f, 5.1f, 21f, 6.5f)
                lineTo(21f, 15.5f)
                curveTo(21f, 16.9f, 19.9f, 18f, 18.5f, 18f)
                lineTo(9.4f, 18f)
                lineTo(4f, 21.2f)
                lineTo(4f, 17.6f)
                curveTo(3.4f, 17.1f, 3f, 16.3f, 3f, 15.5f)
                lineTo(3f, 6.5f)
                curveTo(3f, 5.1f, 4.1f, 4f, 5.5f, 4f)
                close()
                // Garis pesan (EvenOdd)
                moveTo(7.5f, 8.7f)
                lineTo(16.5f, 8.7f)
                lineTo(16.5f, 10.3f)
                lineTo(7.5f, 10.3f)
                close()
                moveTo(7.5f, 12.2f)
                lineTo(13.5f, 12.2f)
                lineTo(13.5f, 13.8f)
                lineTo(7.5f, 13.8f)
                close()
            }
        }.build()
    }

    val DashboardOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinDashboardOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Tile kiri atas
                moveTo(4.5f, 4.5f)
                lineTo(10.5f, 4.5f)
                lineTo(10.5f, 12.5f)
                lineTo(4.5f, 12.5f)
                close()
                // Tile kanan atas
                moveTo(13.5f, 4.5f)
                lineTo(19.5f, 4.5f)
                lineTo(19.5f, 9.5f)
                lineTo(13.5f, 9.5f)
                close()
                // Tile kanan bawah
                moveTo(13.5f, 12.5f)
                lineTo(19.5f, 12.5f)
                lineTo(19.5f, 19.5f)
                lineTo(13.5f, 19.5f)
                close()
                // Tile kiri bawah
                moveTo(4.5f, 15.5f)
                lineTo(10.5f, 15.5f)
                lineTo(10.5f, 19.5f)
                lineTo(4.5f, 19.5f)
                close()
            }
        }.build()
    }

    val DashboardFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinDashboardFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 4f)
                lineTo(10.8f, 4f)
                lineTo(10.8f, 12.8f)
                lineTo(4f, 12.8f)
                close()
                moveTo(13.2f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 9.8f)
                lineTo(13.2f, 9.8f)
                close()
                moveTo(13.2f, 12.2f)
                lineTo(20f, 12.2f)
                lineTo(20f, 20f)
                lineTo(13.2f, 20f)
                close()
                moveTo(4f, 15.2f)
                lineTo(10.8f, 15.2f)
                lineTo(10.8f, 20f)
                lineTo(4f, 20f)
                close()
            }
        }.build()
    }

    val ProfileOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinProfileOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 4.5f)
                arcTo(3.8f, 3.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 12.1f)
                arcTo(3.8f, 3.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 4.5f)
                moveTo(5f, 20f)
                curveTo(5f, 16.5f, 8.1f, 14.5f, 12f, 14.5f)
                curveTo(15.9f, 14.5f, 19f, 16.5f, 19f, 20f)
            }
        }.build()
    }

    val ProfileFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "BantuinProfileFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 4f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 12f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 4f)
                moveTo(4.5f, 20.5f)
                curveTo(4.5f, 16.4f, 7.8f, 14f, 12f, 14f)
                curveTo(16.2f, 14f, 19.5f, 16.4f, 19.5f, 20.5f)
                close()
            }
        }.build()
    }
}
