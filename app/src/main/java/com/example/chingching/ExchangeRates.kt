package com.example.chingching

import java.math.BigDecimal
import java.math.MathContext

data class ExchangeRates(
    val updatedAt: Long,
    val nextUpdateAt: Long,
    val rates: Map<String, BigDecimal>
) {
    init {
        require(updatedAt > 0 && nextUpdateAt > updatedAt) { "Invalid rate timestamps" }
        require(rates["USD"]?.compareTo(BigDecimal.ONE) == 0) { "Expected USD base" }
        require(rates.isNotEmpty() && rates.values.all { it.signum() > 0 }) { "Invalid rates" }
    }

    fun convert(amount: BigDecimal, from: String, to: String): BigDecimal {
        require(amount.signum() >= 0) { "Amount must not be negative" }
        val source = requireNotNull(rates[from]) { "Unsupported source currency" }
        val target = requireNotNull(rates[to]) { "Unsupported target currency" }
        if (from == to) return amount
        return amount.multiply(target).divide(source, MathContext.DECIMAL128)
    }

    fun isStale(now: Long): Boolean = now >= nextUpdateAt
}

object AmountParser {
    fun parse(text: String): BigDecimal? {
        val normalized = text.trim().replace(',', '.')
        if (!normalized.matches(Regex("[0-9]{1,15}(\\.[0-9]{0,8})?"))) return null
        return normalized.toBigDecimalOrNull()
    }
}

interface RateCache {
    fun read(): ExchangeRates?
    fun write(rates: ExchangeRates)
}

data class RateResult(val rates: ExchangeRates, val offline: Boolean)

class RatesRepository(
    private val cache: RateCache,
    private val download: () -> ExchangeRates,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 }
) {
    fun load(forceRefresh: Boolean = false): RateResult {
        val saved = cache.read()
        if (!forceRefresh && saved != null && !saved.isStale(now())) {
            return RateResult(saved, offline = false)
        }
        return try {
            val fresh = download()
            cache.write(fresh)
            RateResult(fresh, offline = false)
        } catch (error: Exception) {
            if (saved != null) RateResult(saved, offline = true) else throw error
        }
    }
}