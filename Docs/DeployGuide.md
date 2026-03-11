**Flexible Timer --- WearOS**

How to Install on Your Watch

There are two main methods to install Flexible Timer on your Wear OS
watch: using ADB over Wi-Fi (no cable needed for most modern watches) or
using a USB cable via the Android Debug Bridge (ADB). Both are described
below.

**1. Enable Developer Options on Your Watch**

This is a one-time setup step.

1.  On your watch, open the Settings app.

2.  Scroll to System → About.

3.  Tap Build number 7 times in quick succession.

4.  You will see the message \"You are now a developer!\"

5.  Go back to Settings → Developer options.

6.  Enable ADB debugging.

7.  Enable Debug over Wi-Fi (if available on your watch model).

> **ℹ️ Note:** *The exact menu path may differ slightly across watch
> brands (Samsung Galaxy Watch, Pixel Watch, etc.) but the steps are the
> same.*

**2. Method A --- Install Over Wi-Fi (Recommended)**

No USB cable required. Your watch and computer must be on the same Wi-Fi
network.

**Step 1 --- Find the watch IP address**

8.  On your watch: Settings → Developer options → Debug over Wi-Fi.

9.  Note the IP address shown (e.g. 192.168.1.42:5555).

**Step 2 --- Connect from your computer**

Open a terminal on your computer and run:

> adb connect 192.168.1.42:5555 

or

> adb pair 192.168.1.42:5555 or


Replace the IP with the one shown on your watch. You should see:

> connected to 192.168.1.42:5555

On the watch, accept the RSA key fingerprint prompt if it appears.

**Step 3 --- Install the APK**

> adb install -r path/to/app-debug.apk

The -r flag allows re-installation (useful for updates). A successful
install shows:

> Performing Streamed Install
>
> Success

**3. Method B --- Install via USB Cable**

Use this method if Wi-Fi pairing is unavailable.

10. Plug your watch into your computer using its charging/data cable.

11. On the watch, accept the \"Allow USB debugging\" prompt.

12. Verify the watch is detected:

> adb devices

You should see your device listed, e.g.:

> XXXXXXXXXXXXXX device

13. Install the APK:

> adb install -r path/to/app-debug.apk

**4. Verify the Installation**

14. On your watch, swipe up or press the crown to open the app list.

15. Look for \"Flexible Timer\" in the app list.

16. Tap it to launch.

Alternatively, launch it directly from ADB:

> adb shell am start -n com.flexibletimer/.MainActivity

**5. View Logs (Optional --- for debugging)**

To stream logs from the watch to your terminal in real time:

> adb logcat -s FlexibleTimer

Or for broader logs filtered by your app package:

> adb logcat \| grep com.flexibletimer

**6. Updating the App**

When you rebuild the APK and want to push an update to the watch:

> ./gradlew assembleDebug && adb install -r
> app/build/outputs/apk/debug/app-debug.apk

The -r flag ensures the app is replaced in-place, preserving any saved
sequences/groups stored in the watch\'s Room database.

**7. Troubleshooting**

**adb: command not found**

-   Add the Android SDK platform-tools directory to your PATH:

> export PATH=\$PATH:\$ANDROID_HOME/platform-tools

**Device not found / offline**

-   Disconnect and reconnect the watch.

-   Restart the ADB server: adb kill-server then adb start-server.

-   On the watch: toggle ADB debugging off and on in Developer options.

**INSTALL_FAILED_UPDATE_INCOMPATIBLE**

-   The existing installed APK was signed with a different key.

-   Uninstall the old version first:

> adb uninstall com.flexibletimer

-   Then install the new APK.

**Connection refused on Wi-Fi**

-   Make sure both devices are on the same Wi-Fi network (not guest vs
    main).

-   Some corporate or mesh networks block device-to-device traffic ---
    try a personal hotspot.

> **⚠️** Developer options and ADB debugging are disabled automatically
> when you turn on the watch after a factory reset. You will need to
> enable them again.

**8. ADB Quick Reference**

Useful ADB commands for daily development:

> adb devices \# list connected devices
>
> adb connect \<ip\>:\<port\> \# connect over Wi-Fi
>
> adb install -r \<apk\> \# install / update APK
>
> adb uninstall com.flexibletimer \# remove the app
>
> adb logcat \| grep flexibletimer \# filter logs
>
> adb shell am start -n com.flexibletimer/.MainActivity \# launch app
>
> adb kill-server \# reset ADB daemon
