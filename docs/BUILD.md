# Build

Requirements: JDK 17, Android SDK (compileSdk 34, minSdk 26).

```bash
./gradlew :app:testDebugUnitTest      # unit tests
./gradlew :app:lintDebug              # lint
./gradlew :app:assembleDebug          # debug APK -> app/build/outputs/apk/debug/app-debug.apk
```

CI (`.github/workflows/android-build.yml`) runs all three and uploads the APK artifact.
