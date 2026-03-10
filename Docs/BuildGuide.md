**Flexible Timer --- WearOS**

How to Build the APK

**1. Prerequisites**

Install the following tools before proceeding:

**Java Development Kit (JDK) 17**

-   Download from: https://adoptium.net/ (Eclipse Temurin 17
    recommended)

-   After install, verify: open a terminal and run:

> java -version

-   You should see: openjdk version \"17.x.x\"

**Android Studio (Hedgehog or newer)**

-   Download from: https://developer.android.com/studio

-   During setup, make sure the following SDK components are installed:

-   Android SDK Platform 34 (API 34)

-   Android SDK Build-Tools 34.x.x

-   Wear OS emulator image (optional, for testing)

**Android SDK Command-Line Tools**

From Android Studio: Tools → SDK Manager → SDK Tools tab → check
\"Android SDK Command-line Tools\".

**Set ANDROID_HOME environment variable**

macOS / Linux --- add to \~/.zshrc or \~/.bashrc:

> export ANDROID_HOME=\$HOME/Library/Android/sdk
>
> export PATH=\$PATH:\$ANDROID_HOME/platform-tools

Windows --- open System Properties → Environment Variables and add:

> ANDROID_HOME = C:\\Users\\\<you\>\\AppData\\Local\\Android\\Sdk

**2. Open the Project in Android Studio**

1.  Launch Android Studio.

2.  Click File → Open.

3.  Navigate to the FlexibleTimer folder and click OK.

4.  Wait for Gradle sync to complete (the progress bar at the bottom
    finishes).

> **ℹ️ Note:** *The first sync downloads all dependencies; it can take
> 5--10 minutes on a fresh machine.*

**3. Run Unit Tests**

From Android Studio:

5.  Open the Gradle panel (View → Tool Windows → Gradle).

6.  Navigate to app → Tasks → verification → test.

7.  Double-click test to run all unit tests.

Or from a terminal inside the project folder:

> ./gradlew test

(On Windows use: gradlew.bat test)

HTML reports are generated at:

> app/build/reports/tests/testDebugUnitTest/index.html

**4. Build the APK**

**4a. Debug APK (for direct installation / development)**

From the terminal inside the project folder, run:

> ./gradlew assembleDebug

The output APK will be at:

> app/build/outputs/apk/debug/app-debug.apk

**4b. Release APK (signed, for production)**

A signed release APK is required for Play Store distribution and
recommended for sideloading.

**Step 1 --- Create a keystore (one-time)**

> keytool -genkey -v -keystore flexible-timer.jks \\
>
> -alias flexibletimer \\
>
> -keyalg RSA -keysize 2048 \\
>
> -validity 10000

Keep this .jks file safe --- you will always need it to sign updates.

**Step 2 --- Add signing config to app/build.gradle**

Inside the android { } block add:

> signingConfigs {
>
> release {
>
> storeFile file(\'/path/to/flexible-timer.jks\')
>
> storePassword \'your_store_password\'
>
> keyAlias \'flexibletimer\'
>
> keyPassword \'your_key_password\'
>
> }
>
> }
>
> buildTypes {
>
> release {
>
> signingConfig signingConfigs.release
>
> minifyEnabled true
>
> proguardFiles
> getDefaultProguardFile(\'proguard-android-optimize.txt\'),
> \'proguard-rules.pro\'
>
> }
>
> }

**Step 3 --- Build the signed APK**

> ./gradlew assembleRelease

The signed APK will be at:

> app/build/outputs/apk/release/app-release.apk
>
> **ℹ️ Note:** *Never commit your keystore password into source control.
> Use a local.properties or environment variables in CI pipelines.*

**5. Build via Android Studio UI (alternative)**

8.  Click Build in the top menu.

9.  Choose Build Bundle(s) / APK(s) → Build APK(s).

10. When the build finishes, click the \"locate\" link in the
    notification to find the APK.

**6. Common Build Errors**

**Gradle sync failed / Could not resolve dependencies**

-   Ensure you have an internet connection on first sync.

-   Try: File → Invalidate Caches / Restart in Android Studio.

**SDK location not found**

-   Check that ANDROID_HOME is set correctly.

-   Or create a local.properties file in the project root with:

> sdk.dir=/Users/\<you\>/Library/Android/sdk

**JVM target mismatch**

-   Ensure your Android Studio uses JDK 17: File → Project Structure →
    SDK Location → Gradle JDK.

**Duplicate class errors**

-   Run: ./gradlew clean assembleDebug
