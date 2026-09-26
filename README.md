# Spotify Overlay for Fabric

This project is a client-side Fabric mod for Minecraft 1.20.1 that shows the current Spotify track in a HUD overlay and exposes simple playback controls.

## Required setup before using it

- Create a Spotify app in the Spotify Developer Dashboard.
- Set the Redirect URI to `http://127.0.0.1:8888/callback`.
- Copy the generated Spotify Client ID into the config file created at `.minecraft/config/spotifyoverlay.json`.
- Run `/spotify login` in-game to authorize the app.

## Build

```bash
./gradlew build
```

## Notes

- The mod stores tokens in the config directory, not in the repo.
- For production usage, consider encrypting the stored tokens.
