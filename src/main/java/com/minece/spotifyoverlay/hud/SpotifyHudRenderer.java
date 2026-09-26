package com.minece.spotifyoverlay.hud;

import com.minece.spotifyoverlay.SpotifyOverlayMod;
import com.minece.spotifyoverlay.api.model.SpotifyPlayback;
import com.minece.spotifyoverlay.auth.SpotifyAuthManager;
import com.minece.spotifyoverlay.api.SpotifyApiClient;
import com.minece.spotifyoverlay.config.SpotifyModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SpotifyHudRenderer implements HudRenderCallback {
    private static final int COVER_SIZE = 64;
    private static final int CARD_HEIGHT = 88;
    private static final int PADDING_X = 12;
    private static final int PADDING_Y = 12;

    private final SpotifyModConfig config;
    private final SpotifyAuthManager authManager;
    private final SpotifyApiClient apiClient;
    private SpotifyPlayback currentPlayback;
    private long lastFetchMs;

    public SpotifyHudRenderer(SpotifyModConfig config, SpotifyAuthManager authManager, SpotifyApiClient apiClient) {
        this.config = config;
        this.authManager = authManager;
        this.apiClient = apiClient;
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        if (!config.enabled || !authManager.isAuthenticated()) {
            return;
        }

        refreshIfNeeded();
        if (currentPlayback == null || !currentPlayback.hasTrack()) {
            drawEmptyState(drawContext);
            return;
        }

        drawCard(drawContext);
    }

    public void togglePlayback() {
        if (currentPlayback != null && currentPlayback.isPlaying()) {
            apiClient.pausePlayback();
        } else {
            apiClient.resumePlayback();
        }
        lastFetchMs = 0L;
    }

    private void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        if (currentPlayback == null || now - lastFetchMs >= config.pollIntervalMs) {
            currentPlayback = apiClient.getCurrentPlayback();
            lastFetchMs = now;
        }
    }

    private void drawCard(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        int x = config.hudX;
        int y = config.hudY;
        int width = config.cardWidth;
        int height = config.cardHeight;

        drawContext.fill(x, y, x + width, y + height, 0xB0111111);
        drawContext.fill(x, y, x + width, y + 1, 0xFF2A2A2A);
        drawContext.fill(x, y + height - 1, x + width, y + height, 0xFF2A2A2A);

        int artX = x + 8;
        int artY = y + 8;
        Identifier albumArt = AlbumArtTextureCache.getTexture(currentPlayback.getAlbumArtUrl());
        if (albumArt != null) {
            drawContext.drawTexture(albumArt, artX, artY, 0, 0, COVER_SIZE, COVER_SIZE, COVER_SIZE, COVER_SIZE);
        } else {
            drawContext.fill(artX, artY, artX + COVER_SIZE, artY + COVER_SIZE, 0xFF1B1B1B);
            drawContext.drawCenteredTextWithShadow(client.textRenderer, Text.literal("♪"), artX + COVER_SIZE / 2, artY + 22, 0xFFFFFFFF);
        }

        int textX = artX + COVER_SIZE + 12;
        int textY = y + 18;
        String title = currentPlayback.getTrackName();
        String artist = currentPlayback.getArtistName();
        drawContext.drawTextWithShadow(client.textRenderer, Text.literal(title), textX, textY, 0xFFFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.literal(artist), textX, textY + 18, 0xFFB0B0B0);

        int barX = textX;
        int barY = y + 52;
        int barWidth = width - (textX - x) - 12;
        float progressRatio = currentPlayback.getDurationMs() > 0 ? (float) currentPlayback.getProgressMs() / currentPlayback.getDurationMs() : 0.0F;
        drawContext.fill(barX, barY, barX + barWidth, barY + 4, 0xFF2A2A2A);
        drawContext.fill(barX, barY, barX + Math.round(barWidth * progressRatio), barY + 4, 0xFF1DB954);

        long elapsed = Math.min(currentPlayback.getProgressMs(), currentPlayback.getDurationMs());
        long total = currentPlayback.getDurationMs();
        drawContext.drawTextWithShadow(client.textRenderer, Text.literal(formatDuration(elapsed)), barX, barY + 10, 0xFFB8B8B8);
        drawContext.drawTextWithShadow(client.textRenderer, Text.literal(formatDuration(total)), x + width - 16 - client.textRenderer.getWidth(formatDuration(total)), barY + 10, 0xFFB8B8B8);
    }

    private void drawEmptyState(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        int x = config.hudX;
        int y = config.hudY;
        int width = 220;
        int height = 52;

        drawContext.fill(x, y, x + width, y + height, 0xB0111111);
        drawContext.drawTextWithShadow(client.textRenderer, Text.literal("Nothing playing"), x + 12, y + 18, 0xFFFFFFFF);
        drawContext.drawTextWithShadow(client.textRenderer, Text.literal("Run /spotify login"), x + 12, y + 30, 0xFFB0B0B0);
    }

    private static String formatDuration(long ms) {
        long seconds = Math.max(0L, ms / 1000L);
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }
}
