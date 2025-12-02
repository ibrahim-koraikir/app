# Technology Stack

## Build System

- **Gradle**: Kotlin DSL (build.gradle.kts)
- **AGP**: 8.13.0
- **Kotlin**: 2.0.21
- **Java**: Version 11 (source/target compatibility)
- **KSP**: 2.0.21-1.0.25 for annotation processing

## Core Technologies

- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt (Dagger)
- **Database**: Room (2.6.1) with KTX extensions
- **Networking**: OkHttp (4.12.0) with logging interceptor
- **Image Loading**: Coil (2.5.0) for Compose
- **Serialization**: Kotlinx Serialization JSON (1.6.2)
- **Async**: Kotlin Coroutines (1.8.0) and Flow
- **Navigation**: Jetpack Navigation Compose (2.7.6)
- **Preferences**: DataStore Preferences (1.1.1)
- **Background Tasks**: WorkManager (2.9.0)

## Key Libraries

- **Compose BOM**: 2024.10.00
- **Lifecycle**: 2.9.4
- **Activity Compose**: 1.11.0
- **Hilt Navigation Compose**: 1.1.0
- **LeakCanary**: 2.12 (debug builds only)

## Common Commands

### Build & Run
```bash
# Build debug APK
gradlew assembleDebug

# Build release APK (minified)
gradlew assembleRelease

# Install and run on connected device
gradlew installDebug

# Clean build
gradlew clean
```

### Testing
```bash
# Run unit tests
gradlew test

# Run instrumented tests
gradlew connectedAndroidTest

# Run specific test class
gradlew test --tests "com.entertainmentbrowser.ClassName"
```

### Code Quality
```bash
# Generate lint report
gradlew lint

# Check dependencies
gradlew dependencies
```

## ProGuard Configuration

Release builds use ProGuard with:
- `isMinifyEnabled = true`
- `isShrinkResources = true`
- Custom rules in `app/proguard-rules.pro`

## Package Structure

Base package: `com.entertainmentbrowser`
