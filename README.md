# Speaker Keep-Alive

**English** | [Русский](README.ru.md)

An Android TV app that keeps your soundbar (or any other speaker) from going into standby.

Many speakers fall asleep when they don't receive an audio signal for a while, so the first seconds of the next video or sound get lost while the speaker wakes up. Speaker Keep-Alive plays a short, quiet sine tone every few minutes. The speaker's silence detector sees a non-zero digital signal and stays awake, while the tone itself is barely audible.

## Features

- A foreground service with a persistent notification that plays the tone periodically.
- Starts automatically after the TV reboots, if the service was enabled.
- Configurable from the remote, right on the TV screen. Each button cycles through its values:

  | Setting | Values | Default |
  |---|---|---|
  | Interval | 1, 2, 3, 5, 10, 15, 20, 30 min | 5 min |
  | Tone volume (amplitude) | 1, 2, 5, 10, 20, 40 % | 5 % |
  | Frequency | 20, 40, 60, 100, 1000, 15000, 19000 Hz | 40 Hz |
  | Duration | 1, 2, 3, 5 s | 2 s |

- A "Play test tone" button to check your settings.
- No network access, no analytics, no unnecessary permissions.

> **Note:** the app's UI is currently in Russian only.

## Installation

Download the APK from the [Releases](../../releases) page (or [build it yourself](#building)) and install it with `adb`:

1. On the TV, enable developer mode (Settings → About → tap "Build" several times) and turn on USB debugging or network debugging.
2. Connect and install the APK:

   ```sh
   adb connect <tv-ip-address>:5555
   adb install -r speaker-keep-alive-v1.0.0.apk
   ```

3. Launch the app from the Android TV app list and press the start button.

You need to launch the app manually once. Until then Android doesn't deliver `BOOT_COMPLETED` to it, so autostart after a reboot won't work.

## Building

You need JDK 17 and the Android SDK (compileSdk 35). If you don't use Android Studio, point Gradle to the SDK in `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

Then:

```sh
./gradlew assembleRelease
```

The APK will be at `app/build/outputs/apk/release/app-release.apk`.

A local release build is signed with the **debug key** (see `app/build.gradle.kts`). It's meant for installing via `adb` and isn't suitable for app stores.

## Releasing

Pushing a tag like `v1.0.0` triggers the [Release workflow](.github/workflows/release.yml): it builds the APK and publishes it as a GitHub release.

```sh
git tag v1.0.0
git push origin v1.0.0
```

The tag sets `versionName`, the workflow run number sets `versionCode`. To sign releases with your own key instead of the debug one, add these repository secrets: `RELEASE_KEYSTORE_BASE64` (`base64 -w0 release.jks`), `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Use the same key for every release, otherwise Android won't let a new version update the old one. The [CI workflow](.github/workflows/ci.yml) builds a debug APK and runs lint on every push to `main` and on pull requests.

## Tuning

The silence detector only cares that the signal is non-zero, not how loud it is.

- **The speaker still falls asleep.** Increase the tone volume or duration, or shorten the interval.
- **You can hear the tone.** Lower the volume or change the frequency. Soundbar drivers reproduce almost nothing at low frequencies (20–60 Hz). 15000 and 19000 Hz are at the edge of hearing, so not everyone can hear them.
- Different speakers detect silence differently, so the best values have to be found by experiment. The "Play test tone" button helps to check the result quickly.

## How it works

- `MainActivity` — the settings screen, built from plain `Button`s without XML layouts and controlled with the D-pad.
- `KeepAliveService` — a foreground service (`specialUse` type). Holds a `PARTIAL_WAKE_LOCK`, triggers the tone on a timer in a background thread and reschedules itself with the current interval.
- `Beeper` — generates a sine wave with a 100 ms fade-in and fade-out to avoid clicks, and plays it through `AudioTrack` (stereo, 48 kHz, 16-bit).
- `Prefs` — settings stored in `SharedPreferences`, each with a fixed set of values.
- `BootReceiver` — starts the service after boot if it was enabled.

Tone parameters are read on every beep, so changing them takes effect without restarting the service. Changing the interval restarts the service.

## Requirements

- Android 6.0+ (minSdk 23), targeting Android 14 (targetSdk 34).
- Designed for Android TV (Leanback), but also runs on regular Android devices.

## License

[MIT](LICENSE)
