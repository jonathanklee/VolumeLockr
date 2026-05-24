<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" width="120" alt="VolumeLockr Icon"/>
</p>

<h1 align="center">VolumeLockr / 音量锁定器</h1>

<p align="center">
  <a href="https://www.gnu.org/licenses/gpl-3.0"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License: GPL v3"/></a>
  <a href="https://f-droid.org/packages/com.klee.volumelockr"><img src="https://img.shields.io/f-droid/v/com.klee.volumelockr?label=F-Droid" alt="F-Droid"/></a>
  <a href="https://github.com/jonathanklee/VolumeLockr/actions/workflows/android.yml"><img src="https://github.com/jonathanklee/VolumeLockr/actions/workflows/android.yml/badge.svg" alt="Android CI"/></a>
  <a href="https://github.com/jonathanklee/VolumeLockr/releases"><img src="https://img.shields.io/github/v/release/jonathanklee/VolumeLockr?label=Release" alt="Release"/></a>
  <img src="https://img.shields.io/badge/API-24%2B-brightgreen" alt="Min API 24"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-purple" alt="Language: Kotlin"/>
</p>

<p align="center">
  <b>English</b> | <a href="#简体中文">简体中文</a>
</p>

---

VolumeLockr allows you to control your Android device volume levels and set persistent locks for each audio stream. Once locked, the volume stays fixed — even when pressing hardware buttons or switching audio modes.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="200" alt="Screenshot 1"/>
  &nbsp;&nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="200" alt="Screenshot 2"/>
</p>

## Features

- Lock individual audio streams: **Media**, **Alarm**, **Ring / Notification**, **Call**
- **Do Not Disturb** integration — detect ringer mode changes (Silent, Vibrate, Normal)
- **Lower-only mode** — prevent accidental volume increases
- **Password protection** — require a password to change locks or disable protection
- **Hide from recents** — remove the app from the recent tasks list (powered by `AppTask.setExcludeFromRecents`)
- **Foreground service** — keep volume locks active when the app is in background
- **Auto-start on boot** — re-apply locks after device reboot
- **No ads, no tracking, no network permissions**
- **Available in 4 languages**: English, 简体中文, Français, Русский

## Installation

### F-Droid (Recommended)

<a href="https://f-droid.org/packages/com.klee.volumelockr">
  <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80" alt="Get it on F-Droid"/>
</a>

### Build from Source

**Prerequisites**
- JDK 17+
- Android SDK (API 35+)

```bash
# Clone
git clone https://github.com/jonathanklee/VolumeLockr.git
cd VolumeLockr

# Build debug APK
./gradlew assembleDebug

# Build release APK (unsigned)
./gradlew assembleRelease

# Install via ADB
adb install app/build/outputs/apk/release/app-release.apk
```

**Signing the release APK**

1. Create `release/keystore.properties` (git-ignored):
```properties
storeFile=../release.keystore
storePassword=your_password
keyAlias=your_alias
keyPassword=your_password
```
2. Place your keystore at `release/release.keystore`
3. Run `./gradlew assembleRelease` — the build will auto-detect and sign with your credentials.

## Usage

1. Open the app and set a lock for each volume stream
2. Grant "Do Not Disturb" access when prompted (required for ringer mode detection)
3. The foreground service keeps locks active in the background
4. **Optionally** enable password protection in Settings
5. **Optionally** hide from recents in Settings

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Material Design 3, Navigation Component |
| DI | Manual (no framework) |
| Persistence | SharedPreferences + EncryptedSharedPreferences |
| Build | Gradle (Kotlin DSL), Version Catalog |
| CI/CD | GitHub Actions (assemble + detekt + test) |
| Lint | Detekt |
| License | GPL-3.0 |

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on:

- Commit conventions (Conventional Commits)
- Pull Request process
- Code style
- Translation guide

## Translators

- 简体中文 (zh-CN): [@liket](https://github.com/liket)
- Français (fr-FR): Jonathan Klee
- Русский (ru-RU): Jonathan Klee

Want to add a new language? See [CONTRIBUTING.md#translations](CONTRIBUTING.md).

---

<h2 id="简体中文">简体中文</h2>

## 功能

- 独立锁定各个音频流：**媒体音量**、**闹钟音量**、**铃声/通知音量**、**通话音量**
- **勿扰模式**集成 — 检测铃声模式变化（静音、振动、正常）
- **仅允许降低**模式 — 防止意外调高音量
- **密码保护** — 修改锁定或关闭保护需要密码验证
- **最近任务隐藏** — 在多任务界面隐藏应用（基于 `AppTask.setExcludeFromRecents`，支持红米/小米）
- **前台服务** — 应用后台运行时保持音量锁定
- **开机自启** — 设备重启后自动恢复锁定
- **无广告、无追踪、无网络权限**

## 安装

推荐通过 [F-Droid](https://f-droid.org/packages/com.klee.volumelockr) 安装，或从源码编译。

## 参与贡献

欢迎提交 PR！详见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 许可证

[GNU General Public License v3.0](LICENSE)

---

<p align="center">
  <a href='https://ko-fi.com/Y8Y5191O6Z' target='_blank'>
    <img height='36' src='https://storage.ko-fi.com/cdn/kofi6.png?v=6' alt='Buy Me a Coffee at ko-fi.com' />
  </a>
</p>