package id.bantuin.app.core.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val rupiahLocale = Locale.forLanguageTag("id-ID")

    fun formatRupiah(amount: Int): String {
        val formatter = NumberFormat.getCurrencyInstance(rupiahLocale).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        return formatter.format(amount).replace("Rp", "Rp ")
    }

    fun formatRupiahShort(amount: Int): String {
        return when {
            amount >= 1_000_000 -> {
                val jt = amount / 1_000_000.0
                String.format(Locale.US, "Rp %.1f jt", jt).replace(".0", "")
            }
            amount >= 1_000 -> {
                val rb = amount / 1_000
                "Rp ${rb}rb"
            }
            else -> "Rp $amount"
        }
    }
}
