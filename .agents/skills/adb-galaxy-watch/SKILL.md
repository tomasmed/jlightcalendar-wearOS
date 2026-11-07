---
name: adb-galaxy-watch
description: Connects to a physical Samsung Galaxy Watch or Wear OS emulator via Wireless ADB, deploys the debug APK, inspects logs, and triggers Tile/Complication updates.
---

# Samsung Galaxy Watch Wireless ADB & Surface Debugging Skill

Use this skill when deploying `JlightCalendar-wearOS` to a physical Samsung Galaxy Watch (Galaxy Watch 4/5/6/7/Ultra) or round Wear OS 4/5 emulator.

## 1. Pairing & Connecting to a Physical Galaxy Watch over Wi-Fi

1. On the Galaxy Watch, open **Settings > About watch > Software information** and tap **Software version** 7 times to enable **Developer options**.
2. Go to **Settings > Developer options**, enable **ADB debugging**, and open **Wireless debugging**.
3. Tap **Pair new device** on the watch to reveal the pairing IP, port, and 6-digit code:
   ```powershell
   adb pair <WATCH_IP>:<PAIRING_PORT> <6_DIGIT_CODE>
   ```
4. Connect to the watch's Wireless Debugging service port:
   ```powershell
   adb connect <WATCH_IP>:<CONNECT_PORT>
   adb devices -l
   ```

## 2. Building & Sideloading the Debug APK

```powershell
.\gradlew.bat installDebug
# Or install the built APK directly:
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 3. Launching the App & Streaming Filtered Logcat

```powershell
# Launch MainActivity on the watch
adb shell am start -n dev.tomasmed.jlight.wear/.ui.MainActivity

# Stream JlightWear application logs (never log PATs or auth headers!)
adb logcat -v time -s JlightWear:V AndroidRuntime:E
```

## 4. Inspecting Wear OS Tiles & Complications

```powershell
# Broadcast a manual Tile refresh request
adb shell am broadcast -a dev.tomasmed.jlight.wear.ACTION_REFRESH_TILE

# Use Android CLI (if installed) to launch or inspect surfaces directly
android run --type=TILE
android layout --pretty
android screen capture --output=watch-screen.png
```
