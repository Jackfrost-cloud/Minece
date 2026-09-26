package com.minece.spotifyoverlay.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.minece.spotifyoverlay.SpotifyOverlayMod;
import com.minece.spotifyoverlay.config.SpotifyModConfig;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

public class SpotifyAuthManager {
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
    private final SpotifyModConfig config;
    private final LocalCallbackServer callbackServer = new LocalCallbackServer();
    private String codeVerifier;

    public SpotifyAuthManager(SpotifyModConfig config) {
        this.config = config;
    }

    public boolean isAuthenticated() {
        return config.accessToken != null && !config.accessToken.isBlank();
    }

    public String getAccessToken() {
        return config.accessToken;
    }

    public String getRefreshToken() {
        return config.refreshToken;
    }

    public boolean ensureValidAccessToken() {
        if (!isAuthenticated()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (config.expiresAtEpochMs <= now + 30_000L) {
            return refreshAccessToken();
        }
        return true;
    }

    public void startLoginFlow() {
        if (config.clientId == null || config.clientId.isBlank()) {
            SpotifyOverlayMod.LOGGER.error("Spotify client ID is missing. Set it in {} before logging in.", config.getClass().getSimpleName());
            return;
        }

        codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        String url = AUTH_URL + "?response_type=code"
            + "&client_id=" + URLEncoder.encode(config.clientId, StandardCharsets.UTF_8)
            + "&scope=" + URLEncoder.encode(SCOPES, StandardCharsets.UTF_8)
            + "&redirect_uri=" + URLEncoder.encode(config.redirectUri, StandardCharsets.UTF_8)
            + "&code_challenge_method=S256"
            + "&code_challenge=" + codeChallenge;

        try {
            if (Desktop.getDesktop().isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
            callbackServer.start(config.redirectUri, this::handleAuthorizationCode, () -> {
                SpotifyOverlayMod.LOGGER.warn("Spotify login failed or was cancelled.");
            });
        } catch (IOException e) {
            SpotifyOverlayMod.LOGGER.error("Failed to open Spotify login browser", e);
        }
    }

    public boolean refreshAccessToken() {
        if (config.refreshToken == null || config.refreshToken.isBlank()) {
            return false;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "grant_type=refresh_token"
                        + "&refresh_token=" + URLEncoder.encode(config.refreshToken, StandardCharsets.UTF_8)
                        + "&client_id=" + URLEncoder.encode(config.clientId, StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }

            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            String accessToken = json.has("access_token") ? json.get("access_token").getAsString() : null;
            String refreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : config.refreshToken;
            long expiresInSeconds = json.has("expires_in") ? json.get("expires_in").getAsLong() : 0L;

            if (accessToken == null || accessToken.isBlank()) {
                return false;
            }

            config.accessToken = accessToken;
            config.refreshToken = refreshToken;
            config.expiresAtEpochMs = Instant.now().toEpochMilli() + (expiresInSeconds * 1000L) - 30_000L;
            config.save();
            return true;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            SpotifyOverlayMod.LOGGER.warn("Spotify token refresh failed", e);
            return false;
        }
    }

    public void clearTokens() {
        config.accessToken = "";
        config.refreshToken = "";
        config.expiresAtEpochMs = 0L;
        config.save();
    }

    private void handleAuthorizationCode(String code) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "grant_type=authorization_code"
                        + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(config.redirectUri, StandardCharsets.UTF_8)
                        + "&client_id=" + URLEncoder.encode(config.clientId, StandardCharsets.UTF_8)
                        + "&code_verifier=" + URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                SpotifyOverlayMod.LOGGER.error("Spotify token exchange failed: {}", response.body());
                return;
            }

            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            config.accessToken = json.get("access_token").getAsString();
            config.refreshToken = json.get("refresh_token").getAsString();
            config.expiresAtEpochMs = Instant.now().toEpochMilli() + (json.get("expires_in").getAsLong() * 1000L) - 30_000L;
            config.save();
            SpotifyOverlayMod.LOGGER.info("Spotify login completed successfully.");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            SpotifyOverlayMod.LOGGER.error("Spotify auth exchange error", e);
        }
    }

    private String generateCodeVerifier() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return urlEncoder.encodeToString(bytes);
    }

    private String generateCodeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] challengeBytes = digest.digest(verifier.getBytes(StandardCharsets.UTF_8));
            return urlEncoder.encodeToString(challengeBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create Spotify PKCE challenge", e);
        }
    }
}
