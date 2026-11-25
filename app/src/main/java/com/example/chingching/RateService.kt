package com.example.chingching

import android.content.SharedPreferences
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object RateService {
    fun download(): ExchangeRates {
        val connection = URL("https://open.er-api.com/v6/latest/USD")
            .openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Rates unavailable" }
            connection.inputStream.bufferedReader().use { parse(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }

    fun parse(body: String): ExchangeRates {
        val json = JSONObject(body)
        require(json.getString("result") == "success") { "Provider returned an error" }
        require(json.getString("base_code") == "USD") { "Unexpected base currency" }
        val values = json.getJSONObject("rates")
        val rates = values.keys().asSequence().associateWith { code ->
            require(code.matches(Regex("[A-Z]{3}"))) { "Invalid currency code" }
            values.get(code).toString().toBigDecimal()
        }
        return ExchangeRates(
            json.getLong("time_last_update_unix"),
            json.getLong("time_next_update_unix"),
            rates
        )
    }
}

class PreferencesRateCache(private val preferences: SharedPreferences) : RateCache {
    override fun read(): ExchangeRates? = try {
        preferences.getString("rates", null)?.let(RateService::parse)
    } catch (_: Exception) {
        null // A damaged cache must not prevent downloading new rates.
    }

    override fun write(rates: ExchangeRates) {
        val values = JSONObject()
        rates.rates.forEach { (code, value) -> values.put(code, value.toPlainString()) }
        val json = JSONObject()
            .put("result", "success")
            .put("base_code", "USD")
            .put("time_last_update_unix", rates.updatedAt)
            .put("time_next_update_unix", rates.nextUpdateAt)
            .put("rates", values)
        preferences.edit().putString("rates", json.toString()).apply()
    }
}