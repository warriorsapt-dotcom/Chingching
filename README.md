# Ching ching

An Android currency converter calculator built with Kotlin, XML layouts, and View Binding.

## Features

- Convert between 21 currencies, including KES, USD, EUR, GBP, UGX, and TZS.
- Swap currencies and recalculate as you type.
- Download daily rates over HTTPS from [ExchangeRate-API](https://www.exchangerate-api.com/docs/free), without an API key.
- Show the provider's update time, refresh manually, and use saved rates if a refresh fails.
- Preserve currency selections between sessions and entered amounts through screen rotation.

Rates are indicative daily rates; results exclude bank fees and exchange spreads. This is not a streaming forex feed. Amounts support up to 15 integer digits and 8 decimal places, with a dot or comma decimal separator and no grouping separators.

## Development

Open the project in Android Studio. Install Android SDK 37, an NDK, and CMake 3.22.1. The Gradle daemon uses JDK 25; Java source compatibility is 11. The minimum device version is Android 10 (API 29).

From PowerShell:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:installDebug
```

Device tests require a connected emulator or phone. Launch **Ching ching** after installing. On macOS/Linux, use `./gradlew`.

## Implementation

`ExchangeRates.kt` handles decimal arithmetic, input validation, and cache refresh decisions. `RateService.kt` handles the HTTPS feed, response validation, and persistent rate cache. `MainActivity.kt` manages the converter screen and performs network requests on a background executor. The original native C++ sample remains in `app/src/main/cpp/`, but conversion uses Kotlin decimal arithmetic.

Unit tests cover cross rates, precision, invalid inputs, cache expiry, forced refresh, and offline fallback. Instrumentation tests cover response parsing, persistent cache, conversion, swapping, and screen recreation without depending on live exchange rates.

## Contribution workflow

After making changes, run the relevant tests and checks. Once they pass, commit and push the changes to GitHub. Do not publish failing or untested changes as verified. Report any authentication, permission, or environment blocker. See [AGENTS.md](AGENTS.md) for repository conventions.

Do not commit local SDK paths, credentials, signing keys, IDE state, or generated build files.
