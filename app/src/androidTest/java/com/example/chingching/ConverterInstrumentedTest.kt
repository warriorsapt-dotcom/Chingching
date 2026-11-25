package com.example.chingching

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.containsString
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class ConverterInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = context.getSharedPreferences("converter", Context.MODE_PRIVATE)

    @Before fun seedRates() {
        preferences.edit().clear().commit()
        val now = System.currentTimeMillis() / 1000
        PreferencesRateCache(preferences).write(
            ExchangeRates(now, now + 86400, mapOf("USD" to BigDecimal.ONE, "KES" to BigDecimal("130")))
        )
    }

    @Test fun parsesProviderResponseAndRejectsError() {
        val body = """{"result":"success","base_code":"USD","time_last_update_unix":100,
            "time_next_update_unix":200,"rates":{"USD":1,"KES":130.5}}"""
        val rates = RateService.parse(body)
        assertEquals(BigDecimal("261.0"), rates.convert(BigDecimal("2"), "USD", "KES"))
        assertTrue(runCatching { RateService.parse("""{"result":"error"}""") }.isFailure)
        assertTrue(runCatching { RateService.parse(body.replace("130.5", "0")) }.isFailure)
        assertTrue(runCatching { RateService.parse(body.replace("USD", "EUR")) }.isFailure)
        assertTrue(runCatching { RateService.parse("not json") }.isFailure)
    }

    @Test fun cacheRoundTripsAndIgnoresCorruption() {
        val cache = PreferencesRateCache(preferences)
        assertEquals(BigDecimal("130"), cache.read()!!.rates["KES"])
        preferences.edit().putString("rates", "broken").commit()
        assertNull(cache.read())
    }

    @Test fun convertsSwapsAndRetainsInputOnRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Wait for the background cache load without relying on a network response.
            val deadline = System.currentTimeMillis() + 5000
            while (System.currentTimeMillis() < deadline) {
                var ready = false
                scenario.onActivity { ready = it.findViewById<android.view.View>(R.id.refresh).isEnabled }
                if (ready) break
                Thread.sleep(50)
            }
            onView(withId(R.id.amount)).perform(replaceText("2"), closeSoftKeyboard())
            onView(withId(R.id.result)).check(matches(withText(containsString("260"))))
            onView(withId(R.id.result)).check(matches(withText(containsString("KES"))))
            onView(withId(R.id.swap)).perform(scrollTo(), click())
            onView(withId(R.id.result)).check(matches(withText(containsString("USD"))))
            scenario.recreate()
            onView(withId(R.id.amount)).check(matches(withText("2")))
            onView(withId(R.id.amount)).perform(replaceText(""), closeSoftKeyboard())
            onView(withId(R.id.result)).check(matches(withText(R.string.result_placeholder)))
        }
    }
}