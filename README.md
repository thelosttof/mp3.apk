# OldPlayer

A good-looking offline MP3 player for old Android phones (Android 4.4+, API 19).
No third-party libraries. Plays everything the phone's media scanner knows about.

## Get the APK (pick one)

**A) Android Studio**
1. Open this folder in Android Studio (File > Open).
2. Let Gradle sync (accept any "upgrade" prompts).
3. Build > Build APK(s). The file is `app/build/outputs/apk/debug/app-debug.apk`.
4. Copy it to the phone and install (allow "Unknown sources" in Settings > Security).

**B) GitHub, no PC tools needed**
1. Create a new repository and upload everything in this folder (keep the `.github` folder).
2. Open the Actions tab, wait for "Build APK" to finish.
3. Download `OldPlayer-apk` from the run page, unzip, install `app-debug.apk`.

## Features
- Opens on a Home screen with colourful tiles: Favorites, Top Played, Recently Added, Recently Played, All Songs, Albums, Artists, Shuffle All
- Bottom navigation (Home / Search / Library) and a mini player that opens the full player
- Library tab with Songs / Albums / Artists
- Big album art, background tinted from the cover
- Play/pause, next/previous, shuffle, repeat (off / all / one), seek bar
- Favourite (heart), sleep timer (moon: 15 / 30 / 60 min)
- Search in the library, background playback with notification, headphone-unplug pause
