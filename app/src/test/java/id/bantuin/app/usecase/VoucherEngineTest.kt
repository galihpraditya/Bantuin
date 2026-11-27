package id.bantuin.app.usecase

import id.bantuin.app.domain.model.VoucherResult
import id.bantuin.app.domain.usecase.voucher.VoucherEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoucherEngineTest {

    private val engine = VoucherEngine()

    @Test
    fun emptyCodeReturnsEmptyResult() {
        assertEquals(VoucherResult.Empty, engine.apply("   ", 100_000))
    }

    @Test
    fun unknownCodeReturnsInvalid() {
        val res = engine.apply("TIDAKADA", 100_000)
        assertTrue(res is VoucherResult.Invalid)
    }

    @Test
    fun percentVoucherAppliesCaseInsensitivelyAndRespectsMaxCap() {
        // BANTUIN10: 10% max 25.000, min order 50.000
        val res = engine.apply("bantuin10", 300_000)
        assertTrue(res is VoucherResult.Valid)
        val valid = res as VoucherResult.Valid
        assertEquals(25_000, valid.discountAmount)
        assertEquals(275_000, valid.finalTotal)
    }

    @Test
    fun flatVoucherAppliesCorrectly() {
        // GRATIS15K: flat 15.000, min order 100.000
        val res = engine.apply("GRATIS15K", 120_000)
        assertTrue(res is VoucherResult.Valid)
        val valid = res as VoucherResult.Valid
        assertEquals(15_000, valid.discountAmount)
        assertEquals(105_000, valid.finalTotal)
    }

    @Test
    fun belowMinOrderReturnsInvalid() {
        val res = engine.apply("GRATIS15K", 80_000)
        assertTrue(res is VoucherResult.Invalid)
    }

    @Test
    fun availableVouchersReturnsActivePromos() {
        assertTrue(engine.getAvailableVouchers().size >= 4)
    }
}
