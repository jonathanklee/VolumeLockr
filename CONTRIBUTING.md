# Contributing to VolumeLockr

Thank you for your interest in contributing! This document outlines the process and conventions for contributing to this project.

---

## Code of Conduct

Please review our [Code of Conduct](CODE_OF_CONDUCT.md) before contributing. All participants are expected to uphold these standards.

## How to Contribute

### 1. Pick an Issue

- Browse [open issues](https://github.com/xunnv/VolumeLockr/issues)
- Comment on the issue to let others know you're working on it
- If you have a new idea, open a [feature request](https://github.com/xunnv/VolumeLockr/issues/new?template=feature_request.yml) first

### 2. Set Up Your Environment

```bash
# Fork & clone
git clone https://github.com/YOUR_USERNAME/VolumeLockr.git
cd VolumeLockr

# Open in Android Studio or build via CLI
./gradlew assembleDebug
```

**Requirements**: JDK 17+, Android SDK API 35+

### 3. Create a Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/your-fix-name
```

Use one of these branch prefixes:
| Prefix | Purpose |
|--------|---------|
| `feature/` | New features |
| `fix/` | Bug fixes |
| `docs/` | Documentation |
| `refactor/` | Code refactoring |
| `translation/` | Translation updates |
| `chore/` | Maintenance tasks |

### 4. Make Your Changes

- Follow the project's code style (Kotlin conventions, Detekt rules)
- Write meaningful commit messages (see [Commit Conventions](#commit-conventions))
- Add or update tests as appropriate
- Ensure Detekt lint checks pass: `./gradlew detekt`

### 5. Submit a Pull Request

1. Push your branch
2. Open a [Pull Request](https://github.com/xunnv/VolumeLockr/compare) targeting `main`
3. Fill out the PR template completely
4. Ensure CI checks pass (build, detekt, test)
5. Wait for review

---

## Commit Conventions

This project follows [Conventional Commits](https://www.conventionalcommits.org/).

### Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

| Type | Usage |
|------|-------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Formatting, missing semicolons, etc. |
| `refactor` | Code restructuring without behavior change |
| `perf` | Performance improvement |
| `test` | Adding or updating tests |
| `chore` | Build process, dependencies, tooling |
| `ci` | CI/CD configuration |
| `i18n` | Internationalization / translations |

### Examples

```
feat(schedule): add time-triggered volume lock with weekday support
fix(volume): correct media stream detection on Android 34+
docs(readme): add simplified Chinese section
i18n(zh): add Chinese translations for settings screen
```

---

## Code Style

- **Language**: Kotlin
- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters
- **Naming**: Kotlin standard conventions (camelCase for variables/functions, PascalCase for classes)
- **Comments**: Write comments in English; inline comments explaining "why" are encouraged
- **Null safety**: Prefer `?.` and `?:` over `!!`
- **String resources**: All user-facing strings go in `res/values/strings.xml`

Run Detekt before pushing:
```bash
./gradlew detekt
```

---

## Translations

To add or update a translation:

1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-<locale>/strings.xml`
2. Translate each `<string>` value (do NOT translate the `name` attribute)
3. Add your translation credit to the [Translators](README.md#translators) section of README
4. Submit as `i18n(<locale>): add/update <language> translations`

**Locale codes**: Use [BCP 47](https://tools.ietf.org/html/bcp47) format (e.g., `zh`, `fr`, `ru-rRU`).

---

## Project Structure

```
VolumeLockr/
├── .github/
│   ├── workflows/android.yml    # CI pipeline
│   ├── ISSUE_TEMPLATE/           # Issue templates
│   └── pull_request_template.md  # PR template
├── app/
│   ├── src/main/
│   │   ├── java/com/klee/volumelockr/
│   │   │   ├── schedule/         # Time-triggered volume schedules
│   │   │   ├── service/          # Foreground service, volume provider
│   │   │   ├── ui/               # Activities, fragments, adapters
│   │   │   └── BootReceiver.kt   # Auto-start on boot
│   │   └── res/                  # Resources, layouts, strings
│   └── build.gradle
├── fastlane/metadata/            # F-Droid store listing
├── gradle/                       # Gradle wrapper & version catalog
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── SECURITY.md
├── CHANGELOG.md
├── LICENSE                       # GPL-3.0
└── README.md
```

---

## Questions?

Open a [discussion](https://github.com/xunnv/VolumeLockr/discussions) or ask in an issue.

Thank you for contributing!