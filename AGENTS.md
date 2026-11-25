# Repository Guidelines

## Project Structure & Module Organization

This project contains one Android application module, `app`, using Kotlin, XML layouts, View Binding, and a C++ JNI library.

- `app/src/main/java/com/example/chingching/`: Kotlin code, including `MainActivity.kt`.
- `app/src/main/cpp/`: native implementation and `CMakeLists.txt`; builds the `chingching` shared library.
- `app/src/main/res/`: layouts, strings, themes, colors, and launcher assets.
- `app/src/main/AndroidManifest.xml`: application and activity declarations.
- `app/src/test/` and `app/src/androidTest/`: local JVM and device tests.
- `gradle/libs.versions.toml`: dependency and plugin versions; Gradle configuration uses Kotlin DSL.

## Build, Test, and Development Commands

Run from the repository root in PowerShell:

- `.\gradlew.bat :app:assembleDebug`: build the debug APK, including native code.
- `.\gradlew.bat :app:testDebugUnitTest`: run local unit tests.
- `.\gradlew.bat :app:connectedDebugAndroidTest`: run instrumentation tests on a connected device or emulator.
- `.\gradlew.bat :app:lintDebug`: run Android Lint.
- `.\gradlew.bat :app:installDebug`: install the debug app; launch it from the device launcher or use Android Studio's Run action.

Use `./gradlew` on macOS/Linux. Configure Android SDK 37, an Android NDK, and CMake 3.22.1. The Gradle daemon requests JDK 25; Java source/target compatibility is 11. Devices must support API 29 or later.

## Coding Style & Naming Conventions

Use four-space indentation for Kotlin and follow surrounding C++ and XML formatting. Use `PascalCase` for classes, `camelCase` for functions/properties, and `snake_case` for resource names. Prefer View Binding for view access and string resources for UI text. Keep JNI symbols aligned with Kotlin package, class, and native method names. No standalone formatter is configured; use Android Studio formatting and Android Lint.

## Testing Guidelines

Local tests use JUnit 4; device tests use AndroidX JUnit and AndroidJUnitRunner, with Espresso available. Name test classes `*Test` and methods by behavior, such as `addition_isCorrect`. Add meaningful tests for changed behavior; verify JNI and UI changes on a device. No coverage threshold is configured.

## Commit & Pull Request Guidelines

Git history is unavailable in this checkout. Use concise, imperative commit subjects, such as `Fix native greeting display`. PRs should explain the change, link relevant issues, report validation, and include screenshots for UI changes.

## Security & Configuration

Keep SDK paths in ignored `local.properties`. Never commit credentials, signing keys, or generated build artifacts.
