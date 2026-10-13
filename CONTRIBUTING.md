# Contributing to Calma

Thank you for your interest in improving Calma! This document explains how to contribute.

## Code of Conduct

Be respectful, inclusive, and constructive. This project follows the [Contributor Covenant](https://www.contributor-covenant.org/version/2/1/code_of_conduct/).

## How to Contribute

### Reporting Bugs

1. Check existing issues first.
2. Open a new issue with:
   - Device / Android version
   - Steps to reproduce
   - Expected vs actual behavior
   - Screenshots if applicable

### Suggesting Features

Open an issue with the "enhancement" label. Describe the problem it solves and any UI/UX ideas.

### Code Contributions

1. Fork the repo and create a feature branch.
2. Write code following the existing style (Kotlin, Compose, MVVM).
3. Add/update unit tests for new logic.
4. Run `./gradlew testDebugUnitTest` and `./gradlew lint` locally.
5. Open a PR with a clear description.

### Code Style

- Kotlin official style (`kotlin.code.style=official` in gradle.properties)
- Compose previews for new screens
- No external image loading libraries (app is offline)
- No analytics, crash reporters, or network calls

### Commit Messages

Use conventional commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.

Example: `feat(focus): add 90-minute preset`

## Development Setup

```bash
# Requires JDK 17, Android SDK (compileSdk 35)
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

See [README.md](README.md#building) for full build instructions.

## Pull Request Process

1. Ensure CI passes (GitHub Actions).
2. Request review from a maintainer.
3. Address feedback.
4. Squash and merge (maintainers will handle).

## License

By contributing, you agree that your contributions will be licensed under the Apache-2.0 License.