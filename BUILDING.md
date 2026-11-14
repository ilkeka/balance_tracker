# Building Balance Tracker

Command-line instructions for building the Android app. The desktop, web and iOS targets are not
part of the Android APK and are not needed here.

## Requirements

| | |
|---|---|
| JDK | **24** (the Android module compiles to JVM target 24) |
| Android SDK | platform 37 (`compileSdk`/`targetSdk`) |
| Gradle | none to install — the wrapper pins 9.6.1 |
| NDK | not required (the only native library comes prebuilt from a Maven dependency) |

Linux and macOS are both fine. The first build needs network access to download Gradle and the
dependencies; afterwards it builds offline.

## Build the release APK

```sh
git clone https://github.com/ilkeka/balance_tracker.git
cd balance_tracker
git checkout v1.0.18

# Point the build at your SDK, either of these works:
export ANDROID_HOME="$HOME/Android/Sdk"
# or: echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew --no-daemon :android:assembleRelease
```

Output:

```
android/build/outputs/apk/release/android-release-unsigned.apk
```

Without a signing configuration the release APK is produced **unsigned**, which is what a build
verification needs. Release builds run R8 (`isMinifyEnabled`) and resource shrinking.

## Signing (optional)

Only needed if you want an installable APK signed with the release key. Add to `local.properties`
in the repository root (the file is git-ignored):

```properties
signing.storeFile=/absolute/path/to/release.jks
signing.storePassword=…
signing.keyAlias=…
signing.keyPassword=…
```

With those present the same command produces `android-release.apk` instead:

```sh
apksigner verify --print-certs android/build/outputs/apk/release/android-release.apk
```

## Android App Bundle (Play Store)

```sh
./gradlew --no-daemon :android:bundleRelease
# -> android/release/android-release.aab
```

## What the published builds run

Released APKs are built by [`.github/workflows/release.yaml`](.github/workflows/release.yaml) on
`ubuntu-latest`:

1. `actions/setup-java@v5`, distribution `zulu`, `java-version: 24`
2. `./gradlew :android:assembleRelease`
3. signed with the release keystore, then uploaded to the GitHub release of the tag

So `./gradlew :android:assembleRelease` with JDK 24 on Linux/macOS is the exact recipe.

## Notes

- `./gradlew build` (used by CI for pull requests) additionally compiles the desktop, web and iOS
  targets and runs the tests — it is not needed to produce the APK.
- `gradle.properties` sets `org.gradle.jvmargs=-Xmx8G` and `kotlin.daemon.jvmargs=-Xmx8G`. On a
  machine with less RAM override them on the command line:
  ```sh
  ./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx4G -Dkotlin.daemon.jvmargs=-Xmx2G \
      :android:assembleRelease
  ```
- Configuration cache, build cache and parallel execution are enabled in `gradle.properties`. If a
  build misbehaves, `./gradlew --no-daemon --no-configuration-cache :android:assembleRelease`
  rules that out.
