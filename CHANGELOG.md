# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Dynamic hide-from-recents via `ActivityManager.AppTask.setExcludeFromRecents()` API (works on Xiaomi/MIUI)
- Simplified Chinese (`zh-CN`) translation by [@liket](https://github.com/liket)
- `VolumeLockr` log tag for structured boot-time debugging via `adb logcat -s VolumeLockr`
- Bilingual README (English + 简体中文) with badges, features, and screenshots
- `CONTRIBUTING.md` with Conventional Commits, branch prefixes, code style, and translation guide
- `CODE_OF_CONDUCT.md` (Contributor Covenant 2.1)
- `CHANGELOG.md` (Keep a Changelog format)
- GitHub issue templates (bug report + feature request) and PR template

### Changed
- Replaced `startSelf()` boot crash handling with `mForegroundFailed` flag + `START_NOT_STICKY` to prevent death loop
- Replaced 25ms `Timer` polling with `ScheduledExecutorService` (500ms interval, 100ms initially) for 80% CPU reduction
- Removed `ContentObserver` → `checkVolumes()` chain to eliminate boot-time audio HAL contention (12 premature calls)
- Increased boot start delay from 15s to 60s to allow MIUI/HyperOS audio HAL full initialization
- Replaced `notifyDataSetChanged()` with `notifyItemChanged(pos)` for single-item RecyclerView updates
- Replaced per-call `Gson()` instances with companion `gson` singleton (reflection cache reuse)
- Replaced O(n) `loadLockFromService()` loop with `HashMap.containsKey()` O(1) lookup
- Cached `isPasswordProtected()` value to avoid `SharedPreferences` read on every `onBindViewHolder()`
- Added `try-catch` in `checkVolumes()` to prevent `ScheduledExecutorService` silent shutdown on exceptions
- Moved release signing credentials from hardcoded `build.gradle` to external `release/keystore.properties`
- Improved `.gitignore` with comprehensive Android + signing + secrets patterns

### Fixed
- Boot auto-start crash loop: `ForegroundServiceStartNotAllowedException` → `stopSelf()` → `START_STICKY` → infinite restart
- Volume lock failure in background (service destroyed on unbind due to missing `startService()`)
- MIUI recents hiding not working via `Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS` → switched to `AppTask` API
- Boot lag caused by premature audio HAL access during first 60 seconds after reboot

### Removed
- Deprecated `buildToolsVersion "30.0.3"` (AGP 8.13 uses default 35.0.0)
- Unused `ext.kotlin_version` from root `build.gradle` (version catalog supersedes it)
- Redundant `VolumeService.start()` in `onVolumeLocked()` (service already bound)
- Memory leak: unregistered `Handler` callback in `VolumeSliderFragment`

---

## [1.7.1] - 2025-02-01

### Added
- Russian (`ru-rRU`) translation

### Fixed
- Minor UI layout issues on tablets (w600dp)

---

## [1.7.0] - 2025-01-15

### Added
- French (`fr-FR`) translation

### Changed
- Upgraded target SDK to 35
- Migrated to Gradle Version Catalog (libs.versions.toml)

---

## [1.6.4] - 2024-11-20

### Added
- Password protection with AES-256 encrypted storage
- Password change dialog

### Fixed
- Volume lock not persisting after process restart on some devices

---

## [1.6.0] - 2024-09-10

### Added
- "Allow lowering volume" option (lower-only mode)
- Boot completed receiver for auto-start
- Detekt static analysis integration

### Changed
- Refactored volume checking loop to use ScheduledExecutorService
- Improved foreground service notification wording

---

## [1.5.0] - 2024-06-01

### Added
- Do Not Disturb access integration for ringer mode detection
- Per-stream volume lock with individual sliders
- Material Design 3 theme
- GitHub Actions CI pipeline

---

[Unreleased]: https://github.com/jonathanklee/VolumeLockr/compare/v1.7.1...HEAD
[1.7.1]: https://github.com/jonathanklee/VolumeLockr/compare/v1.7.0...v1.7.1
[1.7.0]: https://github.com/jonathanklee/VolumeLockr/compare/v1.6.4...v1.7.0
[1.6.4]: https://github.com/jonathanklee/VolumeLockr/compare/v1.6.0...v1.6.4
[1.6.0]: https://github.com/jonathanklee/VolumeLockr/compare/v1.5.0...v1.6.0
[1.5.0]: https://github.com/jonathanklee/VolumeLockr/releases/tag/v1.5.0