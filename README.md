# DriveDeck

A car-style home screen for an Android tablet. Every app on the tablet, laid out in big, high-contrast tiles that are easy to hit while mounted in a dash.

- **Pinned row** at the top for the apps you use while driving (maps, music, phone). Long-press any app to pin, unpin, hide it, or open its app info.
- **All apps** grid, A-Z, with a letter rail on the right so one tap jumps anywhere.
- **Settings:** extra-large tiles, keep screen on, hide system bars, unhide apps, set as default home.
- **Self-updating:** checks this repo's latest GitHub Release and installs it from inside the app.

## First-time setup

1. Create a **public** GitHub repo named `drivedeck-` under `Jondude1` (the updater reads the latest release without a login, so the repo must be public).
2. Push this folder to its `main` branch. GitHub Actions builds the APK and publishes it as a Release (`v1.0.<run number>`).
3. On the tablet, open the repo's latest Release in a browser and install the `.apk`. Allow "install unknown apps" for your browser when asked.
4. Open DriveDeck, tap **Settings**, then **Set DriveDeck as default home**.

After that, every push to `main` produces a new release, and the app offers to install it by itself.

## Notes

- The signing key in `keystore/` is intentionally committed. This is a sideloaded app, and a permanent key is what lets updates install over the previous version.
- If a different build with another key is already installed, uninstall it once first. That clears its data.
- Local build: `./gradlew assembleDebug` (needs the Android SDK).
