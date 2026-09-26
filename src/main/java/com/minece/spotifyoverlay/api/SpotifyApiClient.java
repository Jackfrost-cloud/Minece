package com.minece.spotifyoverlay.api;

import com.google.gson.Gson;
import com.minece.spotifyoverlay.SpotifyOverlayMod;
import com.minece.spotifyoverlay.api.model.SpotifyPlayback;
import com.minece.spotifyoverlay.auth.SpotifyAuthManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class SpotifyApiClient {
    private static final String API_BASE = "https://api.spotify.com/v1";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final SpotifyAuthManager authManager;
    private volatile long lastRequestTimeMs = 0L;

    public SpotifyApiClient(SpotifyAuthManager authManager) {
        this.authManager = authManager;
    }

    public SpotifyPlayback getCurrentPlayback() {
        if (!authManager.ensureValidAccessToken()) {
            return null;
        }

        throttle();

        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + "/me/player/currently-playing"))
            .header("Authorization", "Bearer " + authManager.getAccessToken())
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204) {
                return null;
            }
            if (response.statusCode() != 200) {
                SpotifyOverlayMod.LOGGER.warn("Spotify currently-playing request failed: {} {}", response.statusCode(), response.body());
                return null;
            }
            return gson.fromJson(response.body(), SpotifyPlayback.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public boolean nextTrack() {
        return postAction("/me/player/next");
    }

    public boolean previousTrack() {
        return postAction("/me/player/previous");
    }

    public boolean pausePlayback() {
        return putAction("/me/player/pause");
    }

    public boolean resumePlayback() {
        return putAction("/me/player/play");
    }

    public boolean openCurrentTrack() {
        SpotifyPlayback playback = getCurrentPlayback();
        if (playback == null || !playback.hasTrack()) {
            return false;
        }

        String spotifyUrl = playback.getSpotifyUrl();
        if (spotifyUrl == null || spotifyUrl.isBlank()) {
            return false;
        }

        try {
            java.awt.Desktop.getDesktop().browse(URI.create(spotifyUrl));
            return true;
        } catch (Exception e) {
            SpotifyOverlayMod.LOGGER.warn("Unable to open Spotify URL", e);
            return false;
        }
    }

    private boolean postAction(String endpoint) {
        if (!authManager.ensureValidAccessToken()) {
            return false;
        }
        throttle();
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + endpoint))
            .header("Authorization", "Bearer " + authManager.getAccessToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean putAction(String endpoint) {
        if (!authManager.ensureValidAccessToken()) {
            return false;
        }
        throttle();
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + endpoint))
            .header("Authorization", "Bearer " + authManager.getAccessToken())
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void throttle() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTimeMs;
        if (elapsed < 1200L) {
            try {
                TimeUnit.MILLISECONDS.sleep(1200L - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTimeMs = System.currentTimeMillis();
    }
}
