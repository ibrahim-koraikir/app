# Project Structure

## Architecture

The project follows **Clean Architecture** with **MVVM** pattern, organized into distinct layers:

```
app/src/main/java/com/entertainmentbrowser/
├── core/                    # Core utilities and shared components
│   ├── constants/          # App-wide constants
│   ├── error/              # Error handling
│   └── result/             # Result wrapper types
├── data/                    # Data layer
│   ├── local/              # Local data sources (Room, DataStore)
│   │   ├── dao/            # Room DAOs
│   │   ├── database/       # Database configuration
│   │   └── entity/         # Room entities
│   ├── repository/         # Repository implementations
│   └── worker/             # WorkManager workers
├── di/                      # Dependency injection modules
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   ├── DataStoreModule.kt
│   ├── DownloadModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
├── domain/                  # Domain layer
│   ├── model/              # Domain models
│   └── repository/         # Repository interfaces
├── presentation/            # Presentation layer
│   ├── common/             # Shared UI components
│   └── theme/              # Material Design 3 theme
├── util/                    # Utility functions
└── EntertainmentBrowserApp.kt  # Application class
```

## Layer Responsibilities

### Core Layer
- Shared constants, error types, and result wrappers
- No dependencies on other layers

### Data Layer
- **local/**: Room database entities, DAOs, and DataStore preferences
- **repository/**: Concrete implementations of domain repository interfaces
- **worker/**: Background tasks (e.g., cleanup, sync)
- Depends on: domain layer (repository interfaces)

### Domain Layer
- **model/**: Business logic models (pure Kotlin, no Android dependencies)
- **repository/**: Repository interfaces defining data contracts
- No dependencies on other layers (pure business logic)

### Presentation Layer
- Jetpack Compose UI components
- ViewModels (MVVM pattern)
- **common/**: Reusable composables
- **theme/**: Material Design 3 theming
- Depends on: domain layer

### DI Layer
- Hilt modules for dependency injection
- Provides instances of repositories, database, network clients, etc.

## Key Conventions

1. **Naming**:
   - Entities: `*Entity.kt` (e.g., `WebsiteEntity.kt`)
   - DAOs: `*Dao.kt` (e.g., `WebsiteDao.kt`)
   - Repositories: `*Repository.kt` (interface) and `*RepositoryImpl.kt` (implementation)
   - ViewModels: `*ViewModel.kt`
   - Screens: `*Screen.kt`

2. **Data Flow**:
   - UI → ViewModel → Repository → Data Source
   - Use Kotlin Flow for reactive data streams
   - Use sealed classes/interfaces for UI state

3. **Dependency Direction**:
   - Presentation → Domain ← Data
   - Domain layer has no dependencies on other layers
   - Data and Presentation depend on Domain

4. **Database**:
   - Room entities in `data/local/entity/`
   - DAOs in `data/local/dao/`
   - Database class in `data/local/database/`
   - Prepopulated data in `data/local/PrepopulateData.kt`

5. **Resources**:
   - `app/src/main/res/` for Android resources
   - Use Material Design 3 components
   - Dark theme with primary color #FD1D1D

## Configuration Files

- `app/build.gradle.kts`: App-level build configuration
- `build.gradle.kts`: Project-level build configuration
- `settings.gradle.kts`: Project settings and module inclusion
- `gradle/libs.versions.toml`: Centralized dependency version management
- `app/proguard-rules.pro`: ProGuard rules for release builds
