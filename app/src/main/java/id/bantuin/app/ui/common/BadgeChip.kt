package id.bantuin.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.GreenSuccessContainer
import id.bantuin.app.ui.theme.NeutralBadge
import id.bantuin.app.ui.theme.NeutralBadgeContainer
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.RedErrorContainer

@Composable
fun ProviderBadgeChip(
    badge: ProviderBadge,
    flagAnomaly: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (flagAnomaly) {
        BadgeContainer(
            text = "Anomali Akun",
            color = RedError,
            backgroundColor = RedErrorContainer,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RedError,
                    modifier = Modifier.size(13.dp)
                )
            },
            modifier = modifier
        )
        return
    }

    when (badge) {
        ProviderBadge.TRUSTED -> {
            BadgeContainer(
                text = "Mitra Terpercaya",
                color = GreenSuccess,
                backgroundColor = GreenSuccessContainer,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GreenSuccess,
                        modifier = Modifier.size(13.dp)
                    )
                },
                modifier = modifier
            )
        }
        ProviderBadge.NEW -> {
            BadgeContainer(
                text = "Mitra Baru",
                color = NeutralBadge,
                backgroundColor = NeutralBadgeContainer,
                icon = {
                    Icon(
                        imageVector = Icons.Default.FiberNew,
                        contentDescription = null,
                        tint = NeutralBadge,
                        modifier = Modifier.size(14.dp)
                    )
                },
                modifier = modifier
            )
        }
        ProviderBadge.NONE -> {
            // Mitra reguler tidak menampilkan badge khusus
        }
    }
}

@Composable
fun BadgeContainer(
    text: String,
    color: Color,
    backgroundColor: Color,
    icon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
