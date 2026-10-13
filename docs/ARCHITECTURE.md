# Architecture Overview

Calma follows a clean MVVM architecture with manual dependency injection, optimized for offline-first, privacy-first operation.

## High-Level Modules

```
app/
├── core/               # Shared infrastructure
│   ├── common/         # Time, enums, formatting
│   ├── di/             # CompositionLocal for AppContainer
│   ├── navigation/     # Destinations, CalmaApp NavHost
│   ├── notifications/  # Channels, builders
│   ├── ui/             # Design system (theme, shapes, components, charts)
│   └── ...
├── data/               # Persistence layer
│   ├── local/          # Room database, entities, DAOs
│   ├── prefs/          # DataStore settings
│   └── repository/     # Repository implementations
├── domain/             # Pure business logic (no Android deps)
│   ├── focus/          # FocusTimerEngine, models
│   ├── breathing/      # BreathingScheduler, patterns
│   ├── stats/          # StatisticsCalculator
│   ├── exercise/       # CalmExerciseCatalog
│   └── model/          # Domain models
├── feature/            # Compose screens (MVVM)
│   ├── home/
│   ├── focus/
│   ├── breathing/
│   ├── sounds/
│   ├── calm/
│   ├── mood/
│   ├── statistics/
│   ├── settings/
│   └── about/
├── audio/              # Media3 ExoPlayer mixer + foreground service
│   ├── AmbientSound.kt
│   ├── SoundMixerEngine.kt
│   └── AmbientPlaybackService.kt
└── navigation/         # CalmaApp NavHost + bottom bar
```

## Data Flow

```
UI (Compose) → ViewModel (StateFlow) → Repository → Room / DataStore
                    ↑
                    └── Domain logic (pure functions)
```

- ViewModels expose `StateFlow<UiState>` for Compose observation.
- Repositories mediate between domain logic and data sources.
- Domain logic is pure Kotlin (testable without Android).
- Room DAOs return `Flow` for reactive queries.
- DataStore preferences flow into ViewModels via `settingsStore.settings`.

## Key Design Decisions

| Concern | Decision |
|---------|----------|
| DI | Manual `AppContainer` in `Application`; provided via `LocalAppContainer` |
| Navigation | Compose Navigation with bottom bar (5 tabs) + push routes |
| Persistence | Room for structured data, DataStore Preferences for settings |
| Audio | Media3 ExoPlayer per layer, manual audio focus, foreground service |
| Theming | Material 3 `ColorScheme` + custom `CalmaAccentColors` |
| RTL | `LocalLayoutDirection` pinned by selected language |
| Testing | Pure domain logic unit-tested; UI tests omitted for speed |
| Privacy | No `INTERNET` permission; `allowBackup=false`; local-only |

## Threading

- **Main**: Compose UI, ViewModel observation
- **IO**: Room, DataStore (handled by their coroutines)
- **Default**: Domain logic (timer math, stats) via `Dispatchers.Default`
- **Audio**: Media3 internal threads + foreground service

## Version Compatibility

| Component | Version |
|-----------|---------|
| Android Gradle Plugin | 8.9.1 |
| Kotlin | 2.0.10 |
| Gradle | 8.11.1 |
| Compose BOM | 2025.01.00 (Compose 1.7.6) |
| Room | 2.6.1 |
| DataStore | 1.1.1 |
| Media3 | 1.5.1 |
| Min SDK | 26 |
| Compile/Target SDK | 35 |