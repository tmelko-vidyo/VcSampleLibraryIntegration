## VidyoClient library integration

VidyoWorks sample demonstaiting VidyoClient **join as guest** & **tiles rendering** capabilities.

### Integrate

1. Download the latest VidyoClient Android package:
https://support.vidyocloud.com/hc/en-us/articles/115003651834-VidyoWorks-API-Downloads-

2. Place the corresponding \*.so binaries into: jniLibs -> "arm64-v8a" & "armeabi-v7a" folders (libVidyoClientApp.so).

3. Build and run on the device.

> PORTA, ROOM KEY, DISPLAY NAME could be hardcoded within **JoinActivity**.

> No NDK pre-build required. Just the latest VidyoClient library, Android Studio 3.x version and the real device.
