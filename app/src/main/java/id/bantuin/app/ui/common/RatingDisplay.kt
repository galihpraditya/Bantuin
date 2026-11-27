package id.bantuin.app.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.bantuin.app.ui.theme.BantuinTheme
import java.util.Locale

@Composable
fun RatingDisplay(
    rating: Double?,
    reviewCount: Int? = null,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = colors.starTint,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = if (rating != null) String.format(Locale.US, "%.1f", rating) else "Baru",
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary
        )
        if (reviewCount != null && reviewCount > 0) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "($reviewCount)",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
    }
}
