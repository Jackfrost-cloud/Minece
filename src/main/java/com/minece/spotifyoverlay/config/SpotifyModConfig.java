package com.minece.spotifyoverlay.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpotifyModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("spotifyoverlay.json");

    public String clientId = "";
    public String redirectUri = "http://127.0.0.1:8888/callback";
    public String accessToken = "";
    public String refreshToken = "";
    public long expiresAtEpochMs = 0L;
    public boolean enabled = true;
    public int hudX = 12;
    public int hudY = 12;
    public int pollIntervalMs = 1500;
    public int cardWidth = 320;
    public int cardHeight = 92;

    public static SpotifyModConfig load() {
        SpotifyModConfig config = new SpotifyModConfig();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                SpotifyModConfig loaded = GSON.fromJson(reader, SpotifyModConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (IOException e) {
                System.err.println("Unable to read Spotify overlay config: " + e.getMessage());
            }
        }
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Unable to save Spotify overlay config: " + e.getMessage());
        }
    }
}
