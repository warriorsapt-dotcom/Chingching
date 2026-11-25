package com.example.chingching

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

class ExchangeRatesTest {
    private val rates = ExchangeRates(
        100, 200, mapOf("USD" to BigDecimal.ONE, "KES" to BigDecimal("130"), "EUR" to BigDecimal("0.8"))
    )

    @Test fun convertsAcrossNonUsdCurrencies() {
        assertDecimal("1625", rates.convert(BigDecimal.TEN, "EUR", "KES"))
    }

    @Test fun convertsBackToBaseCurrency() {
        assertDecimal("2", rates.convert(BigDecimal("260"), "KES", "USD"))
    }

    @Test fun preservesSameCurrencyAndZero() {
        assertDecimal("12.345678", rates.convert(BigDecimal("12.345678"), "KES", "KES"))
        assertDecimal("0", rates.convert(BigDecimal.ZERO, "USD", "KES"))
    }

    @Test fun retainsPrecisionUntilDisplayRounding() {
        assertDecimal("0.104", rates.convert(BigDecimal("0.0008"), "USD", "KES"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeAmounts() {
        rates.convert(BigDecimal("-1"), "USD", "KES")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingCurrency() {
        rates.convert(BigDecimal.ONE, "USD", "XXX")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroRate() {
        ExchangeRates(100, 200, mapOf("USD" to BigDecimal.ONE, "KES" to BigDecimal.ZERO))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsIncorrectBaseRate() {
        ExchangeRates(100, 200, mapOf("USD" to BigDecimal.TEN))
    }

    @Test fun acceptsDecimalSeparatorsAndRejectsAmbiguousInput() {
        assertDecimal("12.50", AmountParser.parse(" 12.50 ")!!)
        assertDecimal("12.50", AmountParser.parse("12,50")!!)
        listOf("", "-2", "NaN", "1e3", "1,000.00", "1 000", "1234567890123456", "0.123456789")
            .forEach { assertNull(it, AmountParser.parse(it)) }
    }

    @Test fun validCacheAvoidsNetwork() {
        val repository = RatesRepository(MemoryCache(rates), { error("Should not download") }, { 199 })
        assertFalse(repository.load().offline)
        assertEquals(rates, repository.load().rates)
    }

    @Test fun refreshesAtExpiryAndSavesNewRates() {
        val cache = MemoryCache(rates)
        val fresh = rates.copy(updatedAt = 200, nextUpdateAt = 300)
        val result = RatesRepository(cache, { fresh }, { 200 }).load()
        assertEquals(fresh, result.rates)
        assertEquals(fresh, cache.saved)
        assertFalse(result.offline)
    }

    @Test fun manualRefreshBypassesValidCache() {
        var downloads = 0
        RatesRepository(MemoryCache(rates), { downloads++; rates }, { 150 }).load(true)
        assertEquals(1, downloads)
    }

    @Test fun failedRefreshReturnsLabelledCachedRates() {
        val result = RatesRepository(MemoryCache(rates), { throw IOException() }, { 250 }).load()
        assertTrue(result.offline)
        assertEquals(rates, result.rates)
    }

    @Test(expected = IOException::class)
    fun firstLaunchWithoutInternetReportsFailure() {
        RatesRepository(MemoryCache(null), { throw IOException() }).load()
    }

    private fun assertDecimal(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }

    private class MemoryCache(var saved: ExchangeRates?) : RateCache {
        override fun read() = saved
        override fun write(rates: ExchangeRates) { saved = rates }
    }
}