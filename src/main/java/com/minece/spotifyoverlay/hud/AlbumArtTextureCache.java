package com.minece.spotifyoverlay.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AlbumArtTextureCache {
    private static final Map<String, Identifier> CACHE = new HashMap<>();

    public static Identifier getTexture(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        if (CACHE.containsKey(url)) {
            return CACHE.get(url);
        }

        try {
            byte[] bytes = download(url);
            if (bytes.length == 0) {
                return null;
            }

            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                NativeImage nativeImage = NativeImage.read(in);
                Identifier identifier = new Identifier("spotifyoverlay", "album_art_" + Integer.toHexString(url.hashCode()));
                MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(nativeImage));
                CACHE.put(url, identifier);
                return identifier;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] download(String url) throws IOException {
        try (InputStream inputStream = new URL(url).openStream()) {
            return inputStream.readAllBytes();
        }
    }
}
