package id.bantuin.app.domain.usecase.voucher

import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.domain.model.PromoVoucher
import id.bantuin.app.domain.model.VoucherResult
import id.bantuin.app.domain.model.VoucherType
import kotlinx.datetime.Clock

/**
 * SCALE FEATURE: Promo / Voucher Engine Transparan (F2 Extension)
 *
 * Menghitung potongan harga dari kode promo secara deterministik.
 * Mendukung dua tipe: potongan persen (PERCENT) dan potongan langsung/flat (FLAT).
 * Validasi mencakup: kode valid, minimum order, masa berlaku, dan batas maksimum diskon.
 */
class VoucherEngine(
    private val clock: Clock = Clock.System
) {
    private val availableVouchers: List<PromoVoucher> = listOf(
        PromoVoucher(
            code = "BANTUIN10",
            type = VoucherType.PERCENT,
            value = 10,
            minOrderAmount = 50_000,
            maxDiscount = 25_000,
            description = "Hemat 10% maks Rp25.000 untuk semua layanan"
        ),
        PromoVoucher(
            code = "GRATIS15K",
            type = VoucherType.FLAT,
            value = 15_000,
            minOrderAmount = 100_000,
            description = "Potongan langsung Rp15.000 untuk pesanan >= Rp100.000"
        ),
        PromoVoucher(
            code = "NEWUSER20",
            type = VoucherType.PERCENT,
            value = 20,
            minOrderAmount = 75_000,
            maxDiscount = 50_000,
            description = "Promo pengguna baru: hemat 20% maks Rp50.000"
        ),
        PromoVoucher(
            code = "WEEKEND5K",
            type = VoucherType.FLAT,
            value = 5_000,
            minOrderAmount = 0,
            description = "Potongan akhir pekan Rp5.000 tanpa minimum pembelian"
        )
    )

    fun apply(code: String, orderTotal: Int): VoucherResult {
        val trimmedCode = code.trim()
        if (trimmedCode.isEmpty()) return VoucherResult.Empty

        if (trimmedCode.length > BantuinConfig.VOUCHER_CODE_MAX_LENGTH) {
            return VoucherResult.Invalid("Kode promo tidak valid.")
        }

        val voucher = availableVouchers.find {
            it.code.equals(trimmedCode, ignoreCase = true)
        } ?: return VoucherResult.Invalid("Kode promo \"$trimmedCode\" tidak ditemukan.")

        if (!voucher.isActive) {
            return VoucherResult.Invalid("Promo \"${voucher.code}\" sudah tidak aktif.")
        }

        voucher.expiresAt?.let { expiry ->
            if (clock.now() > expiry) {
                return VoucherResult.Invalid("Promo \"${voucher.code}\" sudah kedaluwarsa.")
            }
        }

        if (orderTotal < voucher.minOrderAmount) {
            val needed = voucher.minOrderAmount - orderTotal
            return VoucherResult.Invalid(
                "Minimum pemesanan Rp${"%,d".format(voucher.minOrderAmount)}. " +
                    "Tambahkan Rp${"%,d".format(needed)} lagi untuk menggunakan promo ini."
            )
        }

        val rawDiscount = when (voucher.type) {
            VoucherType.PERCENT -> (orderTotal * voucher.value / 100.0).toInt()
            VoucherType.FLAT -> voucher.value
        }
        val discountAmount = rawDiscount.coerceAtMost(voucher.maxDiscount)
        val finalTotal = (orderTotal - discountAmount).coerceAtLeast(0)

        return VoucherResult.Valid(
            voucher = voucher,
            discountAmount = discountAmount,
            finalTotal = finalTotal
        )
    }

    fun getAvailableVouchers(): List<PromoVoucher> = availableVouchers.filter { it.isActive }
}
