# Calma

**Calma** is a free, open-source, offline-first Android app for focus, relaxation, and mindfulness. No accounts, no ads, no trackers, no network required.

<p align="center">
  <img src="docs/screenshots/home.png" alt="Home" width="200"/>
  <img src="docs/screenshots/focus.png" alt="Focus" width="200"/>
  <img src="docs/screenshots/breathe.png" alt="Breathe" width="200"/>
  <img src="docs/screenshots/sounds.png" alt="Sounds" width="200"/>
  <img src="docs/screenshots/stats.png" alt="Stats" width="200"/>
</p>

## Features

| Tab | Capability |
|-----|------------|
| **Home** | Quick summary, one-tap calm, daily check-in, shortcuts |
| **Focus** | Pomodoro (15/25/45/60 min) + custom timer, break support, precise elapsed-time math |
| **Breathe** | Guided patterns (4-6, box 4-4-4-4, 4-7-8, etc.), 1/3/5 min sessions, visual ring |
| **Sounds** | 9 ambient layers (rain, forest, birds, sea, stream, wind, white/pink/brown noise), per-layer volume, sleep timer, save mixes |
| **Calm** | Five short grounding exercises (breathing, 5 senses, surroundings, mindful, release) |
| **Mood** | Daily 1–5 scales (mood, stress, energy, focus) + optional note, fully local |
| **Stats** | Weekly/monthly focus totals, session counts, streak, mood/stress trends, all on-device |

## Privacy

- **Zero network access** – the app declares no `INTERNET` permission.
- **No accounts, no cloud, no analytics** – everything stays on your device.
- **Data deletion** – Settings → "Erase all local data" removes everything instantly.
- **Open source** – verify yourself at `https://github.com/Alvandcode/Calma`.

## Building

> **Note:** The local build environment used during development has a restrictive Maven mirror that breaks AGP resolution. The app builds cleanly on standard environments (GitHub Actions, Android Studio with direct Google Maven access).

### Prerequisites
- JDK 17
- Android SDK (compileSdk 35, minSdk 26)
- Gradle 8.11.1 (via wrapper)

### Debug APK
```bash
./gradlew assembleDebug
```

### Release APK (signed)
Configure `CALMA_STORE_FILE`, `CALMA_STORE_PASSWORD`, `CALMA_KEY_ALIAS`, `CALMA_KEY_PASSWORD` in `~/.gradle/gradle.properties` or CI secrets, then:
```bash
./gradlew assembleRelease
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

## Architecture

```
app/
├── core/               # Design system, time utils, DI
├── data/               # Room DB, DataStore, repositories
├── domain/             # Pure logic (timer, breathing, stats)
├── feature/            # Screens (home, focus, breathe, sounds, calm, mood, stats, settings)
├── audio/              # Media3 ExoPlayer mixer + foreground service
└── navigation/         # Compose Navigation bottom bar
```

- **MVVM** with manual DI (no Hilt)
- **Room** for structured data, **DataStore** for preferences
- **Media3 ExoPlayer** for gapless ambient audio mixing
- **Compose + Material 3** with custom calm palette
- **KAPT** for Room (KSP was unstable in this build env)

## Audio Assets

All ambient sounds are **procedurally generated** by `tools/generate_audio.py` using NumPy. They are licensed under the same terms as the source code (Apache-2.0). See `licenses/audio-licenses.md`.

## License

Apache-2.0 — see `LICENSE`.

## Contributing

See `CONTRIBUTING.md`. Issues and PRs welcome.

## Changelog

See `CHANGELOG.md`.

## Privacy Policy

See `PRIVACY.md`.