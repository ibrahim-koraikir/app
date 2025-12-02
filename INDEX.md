# Project Index: Entertainment Browser

This index provides quick navigation to modules, key source files, tests, assets, and documentation within this project.

---

## Build & Run

- Clean: `./gradlew clean`
- Assemble Debug: `./gradlew assembleDebug`
- Assemble Release: `./gradlew assembleRelease`
- Unit tests: `./gradlew test`
- Instrumented tests (device/emulator): `./gradlew connectedAndroidTest`
- Lint: `./gradlew lint`

---

## Modules & Structure (high-level)

- app/
  - src/
    - main/
      - java/com/entertainmentbrowser/
        - core/
        - data/
          - local/
          - repository/
          - worker/
        - di/
        - domain/
        - presentation/
          - common/
          - downloads/
          - favorites/
          - home/
          - navigation/
          - onboarding/
          - sessions/
          - settings/
          - tabs/
          - theme/
          - webview/
        - util/
          - adblock/
      - assets/
        - adblock/
      - AndroidManifest.xml
    - androidTest/
    - test/
  - build.gradle.kts
- build.gradle.kts (root)
- settings.gradle.kts
- gradle/libs.versions.toml

---

## Key Application Files

- App entry
  - app/src/main/java/com/entertainmentbrowser/EntertainmentBrowserApp.kt
  - app/src/main/java/com/entertainmentbrowser/MainActivity.kt

- Navigation & UI (Compose)
  - app/src/main/java/com/entertainmentbrowser/presentation/navigation/EntertainmentNavHost.kt
  - app/src/main/java/com/entertainmentbrowser/presentation/theme/Theme.kt
  - app/src/main/java/com/entertainmentbrowser/presentation/home/HomeScreen.kt
  - app/src/main/java/com/entertainmentbrowser/presentation/favorites/FavoritesScreen.kt
  - app/src/main/java/com/entertainmentbrowser/presentation/webview/WebViewScreen.kt
  - app/src/main/java/com/entertainmentbrowser/presentation/webview/CustomWebView.kt
  - Common components:
    - app/src/main/java/com/entertainmentbrowser/presentation/common/components/AnimatedSnackbar.kt
    - app/src/main/java/com/entertainmentbrowser/presentation/common/components/AnimatedEmptyState.kt
    - app/src/main/java/com/entertainmentbrowser/presentation/common/components/AnimatedSearchBar.kt
    - app/src/main/java/com/entertainmentbrowser/presentation/common/components/AnimatedTabBar.kt

- WebView & Ad-blocking
  - app/src/main/java/com/entertainmentbrowser/presentation/webview/AdBlockWebViewClient.kt
  - app/src/main/java/com/entertainmentbrowser/util/adblock/FastAdBlockEngine.kt
  - app/src/main/java/com/entertainmentbrowser/util/adblock/HardcodedFilters.kt
  - app/src/main/java/com/entertainmentbrowser/util/adblock/AdBlockMetrics.kt
  - app/src/main/java/com/entertainmentbrowser/presentation/webview/VideoDetector.kt
  - app/src/main/java/com/entertainmentbrowser/util/WebViewStateManager.kt
  - app/src/main/java/com/entertainmentbrowser/util/WebViewPool.kt

- Tabs / Sessions / Downloads
  - app/src/main/java/com/entertainmentbrowser/util/TabManager.kt
  - app/src/main/java/com/entertainmentbrowser/domain/model/Tab.kt
  - app/src/main/java/com/entertainmentbrowser/domain/model/Session.kt
  - app/src/main/java/com/entertainmentbrowser/presentation/webview/DownloadDialog.kt
  - app/src/main/java/com/entertainmentbrowser/data/repository/DownloadRepositoryImpl.kt
  - app/src/main/java/com/entertainmentbrowser/data/worker/TabCleanupWorker.kt

- Dependency Injection (Hilt modules)
  - app/src/main/java/com/entertainmentbrowser/di/AppModule.kt
  - app/src/main/java/com/entertainmentbrowser/di/DatabaseModule.kt
  - app/src/main/java/com/entertainmentbrowser/di/DataStoreModule.kt
  - app/src/main/java/com/entertainmentbrowser/di/DownloadModule.kt
  - app/src/main/java/com/entertainmentbrowser/di/ImageLoadingModule.kt
  - app/src/main/java/com/entertainmentbrowser/di/NetworkModule.kt
  - app/src/main/java/com/entertainmentbrowser/di/RepositoryModule.kt

- Performance / Utilities
  - app/src/main/java/com/entertainmentbrowser/util/GpuMemoryManager.kt
  - app/src/main/java/com/entertainmentbrowser/presentation/common/performance/PerformanceUtils.kt
  - app/src/debug/kotlin/com/entertainmentbrowser/debug/ProfilerConfig.kt

---

## Assets

- Ad-block filter lists:
  - app/src/main/assets/adblock/easylist.txt
  - app/src/main/assets/adblock/easyprivacy.txt
  - app/src/main/assets/adblock/fanboy-annoyance.txt

---

## Tests

- Unit tests (app/src/test/java/…)
  - com/entertainmentbrowser/presentation/webview/AdBlockWebViewClientTest.kt
  - com/entertainmentbrowser/util/adblock/FastAdBlockEngineTest.kt
  - com/entertainmentbrowser/util/adblock/HardcodedFiltersTest.kt
  - com/entertainmentbrowser/util/TabManagerTest.kt
  - com/entertainmentbrowser/data/repository/DownloadRepositoryImplTest.kt
  - com/entertainmentbrowser/data/repository/SessionRepositoryImplTest.kt
  - com/entertainmentbrowser/data/repository/TabRepositoryImplTest.kt
  - com/entertainmentbrowser/data/repository/WebsiteRepositoryImplTest.kt
  - com/entertainmentbrowser/presentation/home/HomeViewModelTest.kt
  - com/entertainmentbrowser/presentation/tabs/TabsViewModelTest.kt
  - com/entertainmentbrowser/presentation/downloads/DownloadsViewModelTest.kt
  - com/example/bro/ExampleUnitTest.kt

- Instrumented tests (app/src/androidTest/java/…)
  - com/entertainmentbrowser/util/adblock/AdBlockingIntegrationTest.kt
  - com/example/bro/ExampleInstrumentedTest.kt

- Test reports
  - After running tests:
    - app/build/reports/tests/testDebugUnitTest/index.html
    - app/build/reports/tests/testReleaseUnitTest/index.html

---

## Configuration

- Root Gradle: build.gradle.kts
- App Gradle: app/build.gradle.kts
- Versions catalog: gradle/libs.versions.toml
- ProGuard rules: app/proguard-rules.pro
- Android Manifest: app/src/main/AndroidManifest.xml

---

## Documentation

- QUICK_START.md
- COMPLETE_APP_DOCUMENTATION.md
- COMPLETE_INTEGRATION_SUMMARY.md
- HOW_TO_BUILD_SIMILAR_APPS.md
- QUICK_REFERENCE_CHEATSHEET.md
- BUILD_YOUR_OWN_GUIDE.md
- BUILD_READY.md / FINAL_BUILD_STATUS.md
- WEBVIEW_NAVIGATION_IMPROVEMENTS.md
- WEBVIEW_AD_BLOCKER_FIXES.md / DIRECT_LINK_AD_BLOCKING_IMPROVEMENTS.md / via-adblocking.md
- VIDEO_DETECTION_FIX.md / VIDEO_DETECTION_DEBUG.md / VIDEO_DOWNLOAD_QUALITY_SELECTION.md
- TAB_SWITCHING_FIX.md / TAB_SWITCHING_SUCCESS.md / TAB_MEMORY_OPTIMIZATION.md
- RED_FLASH_FIX.md / RED_BORDER_FLASH_COMPLETE_FIX.md
- BLANK_PAGE_FIX.md / BLANK_SCREEN_FIX_V2.md / BLANK_SCREEN_NAVIGATION_FIX.md / DIRECT_LINK_FIX.md / DIRECT_LINK_BLACK_SCREEN_FIX.md
- MONETIZATION_IMPLEMENTATION.md / MONETIZATION_INTEGRATED.md / MONETIZATION_STATUS.md / MONETIZATION_AD_BLACK_SCREEN_FIX.md / MONETIZATION_FIX_FINAL.md / TEST_MONETIZATION_ADS.md
- GPU_MEMORY_FIX.md / MALI_GPU_ERRORS_FIXED.md / watch_gpu_memory.bat
- PULL_TO_REFRESH.md
- Accessibility: app/src/main/java/com/entertainmentbrowser/util/AccessibilityGuidelines.md

---

## Screens (HTML prototypes)

- screens/
  - features.html
  - home.html
  - permissions.html
  - settings.html
  - tabs.html
  - welcome.html

---

## Helper Scripts (Windows)

- watch_adblock_logs.bat / watch_adblock_detailed.bat / save_adblock_logs.bat
- test_direct_link_blocking.bat
- watch_monetization_ads.bat / save_monetization_logs.bat / test_monetization_working.bat
- watch_gpu_memory.bat / check_gpu_status.bat
- check_crash.bat

---

## Notes

- This index lists frequently-used and key files; the project contains additional implementation details across data, domain, and presentation layers following Clean Architecture and Hilt DI patterns.
