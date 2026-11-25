package com.example.chingching

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.example.chingching.databinding.ActivityMainBinding
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val executor = Executors.newSingleThreadExecutor()
    private var rates: ExchangeRates? = null
    private var loading = false
    private val codes = listOf(
        "USD", "KES", "EUR", "GBP", "UGX", "TZS", "RWF", "ETB", "ZAR", "NGN", "GHS",
        "EGP", "AED", "SAR", "INR", "CNY", "JPY", "AUD", "CAD", "CHF", "SGD"
    )
    private val preferences by lazy { getSharedPreferences("converter", MODE_PRIVATE) }
    private val repository by lazy {
        RatesRepository(PreferencesRateCache(preferences), RateService::download)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val labels = codes.map { "$it — ${Currency.getInstance(it).displayName}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.fromCurrency.adapter = adapter
        binding.toCurrency.adapter = adapter
        val from = savedInstanceState?.getString("from") ?: preferences.getString("from", "USD")
        val to = savedInstanceState?.getString("to") ?: preferences.getString("to", "KES")
        binding.fromCurrency.setSelection(codes.indexOf(from).coerceAtLeast(0))
        binding.toCurrency.setSelection(codes.indexOf(to).coerceAtLeast(0))
        binding.amount.setText(savedInstanceState?.getString("amount") ?: "1")
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateConversion()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.fromCurrency.onItemSelectedListener = listener
        binding.toCurrency.onItemSelectedListener = listener
        binding.amount.doAfterTextChanged { updateConversion() }
        binding.swap.setOnClickListener {
            val fromPosition = binding.fromCurrency.selectedItemPosition
            binding.fromCurrency.setSelection(binding.toCurrency.selectedItemPosition)
            binding.toCurrency.setSelection(fromPosition)
            updateConversion()
        }
        binding.refresh.setOnClickListener { loadRates(forceRefresh = true) }
        binding.attribution.text = HtmlCompat.fromHtml(
            getString(R.string.rates_attribution), HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.attribution.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onStart() {
        super.onStart()
        loadRates()
    }

    private fun loadRates(forceRefresh: Boolean = false) {
        if (loading) return
        loading = true
        binding.refresh.isEnabled = false
        binding.progress.visibility = View.VISIBLE
        binding.status.setText(R.string.loading_rates)
        executor.execute {
            val result = runCatching { repository.load(forceRefresh) }
            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                loading = false
                binding.refresh.isEnabled = true
                binding.progress.visibility = View.GONE
                result.onSuccess { loaded ->
                    rates = loaded.rates
                    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(loaded.rates.updatedAt * 1000))
                    val status = when {
                        loaded.offline -> R.string.offline_rates
                        loaded.rates.isStale(System.currentTimeMillis() / 1000) -> R.string.stale_rates
                        else -> R.string.updated_rates
                    }
                    binding.status.text = getString(status, date)
                }.onFailure {
                    binding.status.setText(R.string.rates_error)
                }
                updateConversion()
            }
        }
    }

    private fun updateConversion() {
        val amount = AmountParser.parse(binding.amount.text.toString())
        binding.amountLayout.error = if (amount == null) getString(R.string.invalid_amount) else null
        binding.result.setText(R.string.result_placeholder)
        binding.unitRate.text = ""
        val available = rates ?: return
        if (amount == null) return
        val from = codes[binding.fromCurrency.selectedItemPosition]
        val to = codes[binding.toCurrency.selectedItemPosition]
        if (from !in available.rates || to !in available.rates) {
            binding.unitRate.setText(R.string.unsupported_currency)
            return
        }
        val formatter = NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = Currency.getInstance(to).defaultFractionDigits.coerceAtLeast(0)
            maximumFractionDigits = minimumFractionDigits
        }
        binding.result.text = getString(
            R.string.converted_amount, formatter.format(available.convert(amount, from, to)), to
        )
        val unitFormatter = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 6 }
        binding.unitRate.text = getString(
            R.string.unit_rate, from, unitFormatter.format(available.convert(BigDecimal.ONE, from, to)), to
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("amount", binding.amount.text.toString())
        outState.putString("from", codes[binding.fromCurrency.selectedItemPosition])
        outState.putString("to", codes[binding.toCurrency.selectedItemPosition])
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        preferences.edit()
            .putString("from", codes[binding.fromCurrency.selectedItemPosition])
            .putString("to", codes[binding.toCurrency.selectedItemPosition])
            .apply()
        super.onStop()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}