# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-10-12

### Added
- Initial release of Calma
- Focus timer with Pomodoro presets and custom durations
- Guided breathing exercises (4-6, box, 4-7-8, custom patterns)
- Ambient sound mixer with 9 synthesized layers
- Five grounding exercises for quick calm
- Daily mood/energy/stress/focus check-in with optional note
- Weekly/monthly statistics with streak tracking
- Full Persian (RTL) and English support
- Offline-first, no accounts, no network permission
- Apache-2.0 license

### Technical
- MVVM architecture with manual DI
- Room + DataStore for local persistence
- Media3 ExoPlayer for ambient audio mixing
- Jetpack Compose + Material 3 with custom calm palette
- Full RTL support, Persian translations
- Unit tests for timer, breathing, stats logic

[1.0.0]: https://github.com/calma-app/calma/releases/tag/v1.0.0