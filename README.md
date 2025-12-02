# Entertainment Browser

A privacy-focused Android browser app with advanced ad-blocking, video download capabilities, and unified access to 45+ entertainment websites.

## Features

- **95%+ Ad Blocking** - Advanced filter engine with EasyList/EasyPrivacy support
- **Privacy Protection** - WebRTC blocking, URL redaction, HTTPS enforcement
- **Video Downloads** - Detect and download videos from compatible sites
- **Tab Management** - Up to 20 tabs with session saving
- **45+ Entertainment Sites** - Streaming, TV shows, books, video platforms
- **Material Design 3** - Modern dark theme UI

## Tech Stack

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt (Dagger)
- **Database**: Room
- **Networking**: OkHttp
- **Build**: Gradle 8.13.0

## Security Features

- ✅ Mixed content protection (HTTPS enforcement)
- ✅ Comprehensive WebRTC blocking (prevents IP leaks)
- ✅ JavaScript interface hardening
- ✅ Privacy-safe logging (URL redaction in production)
- ✅ Strict ad blocking mode for advanced users
- ✅ Network security configuration with minimal cleartext traffic

## Building

```bash
# Clone the repository
git clone https://github.com/ibrahim-koraikir/app.git
cd app

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## Requirements

- Android Studio Hedgehog or later
- Android SDK 24+ (Android 7.0+)
- JDK 11

## Documentation

- [Quick Start Guide](QUICK_START.md)
- [Complete Documentation](COMPLETE_APP_DOCUMENTATION.md)
- [Ad Blocking Architecture](ADBLOCK_ARCHITECTURE.md)
- [Privacy Policy](PRIVACY_LOGGING_POLICY.md)
- [Build Guide](BUILD_YOUR_OWN_GUIDE.md)

## Project Structure

```
app/
├── src/main/java/com/entertainmentbrowser/
│   ├── core/           # Core utilities
│   ├── data/           # Data layer (Room, repositories)
│   ├── domain/         # Domain layer (models, use cases)
│   ├── presentation/   # UI layer (Compose screens)
│   └── util/           # Utilities (ad blocking, WebView pool)
├── src/main/res/       # Android resources
└── src/main/assets/    # Filter lists
```

## License

This project is for educational purposes. Please ensure compliance with applicable laws and terms of service when using this application.

## Contributing

Contributions are welcome! Please read the documentation before submitting pull requests.

## Acknowledgments

- EasyList/EasyPrivacy for filter lists
- Material Design 3 for UI components
- Jetpack Compose for modern Android UI
