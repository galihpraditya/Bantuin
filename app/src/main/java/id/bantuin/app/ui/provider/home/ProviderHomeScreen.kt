package id.bantuin.app.ui.provider.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.util.CurrencyUtils
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BadgeContainer
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.navigation.Routes
import id.bantuin.app.ui.theme.BantuinTheme
import id.bantuin.app.ui.theme.bantuinPressScale
import id.bantuin.app.ui.theme.rememberReducedMotion
import java.util.Locale

@Composable
fun ProviderHomeScreen(
    repository: BantuinRepository,
    onNavigateToRoute: (String) -> Unit,
    viewModel: ProviderHomeViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors

    if (uiState.isLoading && uiState.provider == null) {
        LoadingView(
            message = "Memuat operasional mitra...",
            modifier = modifier.background(colors.background)
        )
        return
    }

    val trust = uiState.trustResult
    val isAccepting = uiState.isAcceptingOrders

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!uiState.errorMessage.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error
                        )
                    }
                }
            }
        }

        // 1. Header Operasional Mitra + Switch Cepat Terima Order
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Halo, ${uiState.provider?.name ?: uiState.user?.fullName ?: "Mitra Bantuin"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = uiState.specialtyLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        AnimatedProviderStatusPill(isAccepting = isAccepting)
                    }

                    HorizontalDivider(color = colors.borderSubtle)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceSubtle)
                            .clickable(enabled = !uiState.isTogglingAccepting) {
                                viewModel.toggleAcceptingOrders()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAccepting) "Status Aktif Menerima Order" else "Mode Istirahat Aktif",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isAccepting) {
                                    "Profil Anda tampil di hasil pencarian pelanggan."
                                } else {
                                    "Anda tidak menerima pesanan baru sementara ini."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isAccepting,
                            onCheckedChange = { checked -> viewModel.setAcceptingOrders(checked) },
                            enabled = !uiState.isTogglingAccepting,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onPrimary,
                                checkedTrackColor = colors.success,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.borderSubtle
                            )
                        )
                    }
                }
            }
        }

        // 2. Kartu Total Saldo & Akses Cepat Pencairan
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        val reducedMotion = rememberReducedMotion()
                        val animatedAvailable by animateIntAsState(
                            targetValue = uiState.availableBalance,
                            animationSpec = if (reducedMotion) snap() else tween(450),
                            label = "availableBalanceAnim"
                        )
                        val animatedTotalRevenue by animateIntAsState(
                            targetValue = uiState.totalCombinedRevenue,
                            animationSpec = if (reducedMotion) snap() else tween(450),
                            label = "totalRevenueAnim"
                        )
                        val withdrawInteraction = remember { MutableInteractionSource() }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Saldo Siap Cair (QRIS)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyUtils.formatRupiah(animatedAvailable),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { onNavigateToRoute(Routes.PROVIDER_WITHDRAW) },
                                interactionSource = withdrawInteraction,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .bantuinPressScale(withdrawInteraction, targetScale = 0.95f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tarik Dana", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        HorizontalDivider(color = colors.borderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Pendapatan Keseluruhan",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = CurrencyUtils.formatRupiah(animatedTotalRevenue),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                }
            }
        }

        // 3. Ringkasan Singkat Antrean & Skor
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeStatTile(
                    title = "Menunggu",
                    count = "${uiState.pendingCount}",
                    subtext = "Perlu konfirmasi",
                    color = colors.warning,
                    onClick = { onNavigateToRoute(Routes.PROVIDER_ORDERS) },
                    modifier = Modifier.weight(1f)
                )
                HomeStatTile(
                    title = "Jadwal Aktif",
                    count = "${uiState.todayOrdersCount}",
                    subtext = "Hari ini & jalan",
                    color = colors.primary,
                    onClick = { onNavigateToRoute(Routes.PROVIDER_ORDERS) },
                    modifier = Modifier.weight(1f)
                )
                HomeStatTile(
                    title = "Trust Score",
                    count = "${trust?.trustScore?.toInt() ?: 70}",
                    subtext = if (trust?.ratingAvg != null) {
                        "Rating ${String.format(Locale.US, "%.1f", trust.ratingAvg)}"
                    } else {
                        "Mitra Baru"
                    },
                    color = colors.success,
                    onClick = { onNavigateToRoute(Routes.PROVIDER_DASHBOARD) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Kartu 1 Tugas Prioritas Hari Ini
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tugas Prioritas Hari Ini",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                PriorityOrderCard(
                    order = uiState.priorityOrder,
                    onOpenQueue = { onNavigateToRoute(Routes.PROVIDER_ORDERS) },
                    onOpenChat = { id -> onNavigateToRoute(Routes.chatRoom(id)) },
                    onCompleteOrder = { id -> onNavigateToRoute(Routes.completeOrder(id)) }
                )
            }
        }

        // 5. Pintasan Operasional Mitra
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pintasan Operasional",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                OperationalShortcutItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "Pesan Masuk Pelanggan",
                    subtitle = if (uiState.unreadChatCount > 0) {
                        "${uiState.unreadChatCount} pesan baru menunggu balasan Anda"
                    } else {
                        "Koordinasi langsung terkait pesanan aktif"
                    },
                    badgeCount = uiState.unreadChatCount,
                    onClick = { onNavigateToRoute(Routes.CHAT_LIST) }
                )

                OperationalShortcutItem(
                    icon = Icons.Default.HomeRepairService,
                    title = "Kelola Layanan & Tarif",
                    subtitle = "Tambah atau ubah tarif jasa dan durasi pengerjaan",
                    onClick = { onNavigateToRoute(Routes.PROVIDER_SERVICES) }
                )

                OperationalShortcutItem(
                    icon = Icons.Default.CalendarMonth,
                    title = "Atur Kalender Kerja",
                    subtitle = "Kelola jam operasional dan jadwal libur harian",
                    onClick = { onNavigateToRoute(Routes.PROVIDER_CALENDAR) }
                )
            }
        }
    }
}

@Composable
private fun AnimatedProviderStatusPill(isAccepting: Boolean) {
    val colors = BantuinTheme.colors
    val reducedMotion = rememberReducedMotion()
    AnimatedContent(
        targetState = isAccepting,
        transitionSpec = {
            if (reducedMotion) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
            }
        },
        label = "providerStatusPill"
    ) { active ->
        val fg = if (active) colors.success else colors.warning
        val bg = if (active) colors.successContainer else colors.warningContainer
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (active) {
                val dotScale = if (reducedMotion) {
                    1f
                } else {
                    val inf = rememberInfiniteTransition(label = "statusDot")
                    val anim by inf.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
                        label = "dotScale"
                    )
                    anim
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            scaleX = dotScale
                            scaleY = dotScale
                        }
                        .clip(CircleShape)
                        .background(fg)
                )
            }
            Text(
                text = if (active) "Terima Pesanan" else "Sedang Istirahat",
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HomeStatTile(
    title: String,
    count: String,
    subtext: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BantuinTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .bantuinPressScale(interactionSource, targetScale = 0.95f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PriorityOrderCard(
    order: BookingItem?,
    onOpenQueue: () -> Unit,
    onOpenChat: (String) -> Unit,
    onCompleteOrder: (String) -> Unit
) {
    val colors = BantuinTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
    ) {
        if (order == null) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Belum Ada Tugas Prioritas",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pesanan yang sedang dikerjakan atau perlu segera ditangani akan muncul otomatis di sini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                OutlinedButton(
                    onClick = onOpenQueue,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                        .copy(brush = SolidColor(colors.outline)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("Buka Antrean Pesanan", color = colors.textPrimary)
                }
            }
        } else {
            val paymentLabel = when {
                order.paymentMethod == PaymentMethod.QRIS && order.paymentStatus == PaymentStatus.PAID ->
                    "QRIS • Lunas"
                order.paymentMethod == PaymentMethod.QRIS -> "QRIS • Menunggu Bayar"
                else -> "Tunai • Bayar di Lokasi"
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BadgeContainer(
                        text = order.status.displayName,
                        color = if (order.status == BookingStatus.DALAM_PENGERJAAN) colors.success else colors.info,
                        backgroundColor = if (order.status == BookingStatus.DALAM_PENGERJAAN) {
                            colors.successContainer
                        } else {
                            colors.infoContainer
                        }
                    )
                    Text(
                        text = CurrencyUtils.formatRupiah(order.totalCost),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "${order.customerName} • ${order.serviceName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${order.scheduledDate}, ${order.scheduledTime} • $paymentLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Text(
                        text = order.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onOpenChat(order.id) },
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                            .copy(brush = SolidColor(colors.outline)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chat Pelanggan", color = colors.textPrimary)
                    }

                    if (order.status == BookingStatus.DALAM_PENGERJAAN) {
                        Button(
                            onClick = { onCompleteOrder(order.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.success,
                                contentColor = if (colors.isDark) colors.background else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Selesaikan Order", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onOpenQueue,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Lihat Antrean", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationalShortcutItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val colors = BantuinTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .bantuinPressScale(interactionSource, targetScale = 0.97f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(colors.borderSubtle))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted
            )
        }
    }
}
