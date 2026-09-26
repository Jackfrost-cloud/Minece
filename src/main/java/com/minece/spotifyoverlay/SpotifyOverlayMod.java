package com.minece.spotifyoverlay;

import com.minece.spotifyoverlay.api.SpotifyApiClient;
import com.minece.spotifyoverlay.auth.SpotifyAuthManager;
import com.minece.spotifyoverlay.config.SpotifyModConfig;
import com.minece.spotifyoverlay.hud.SpotifyHudRenderer;
import com.minece.spotifyoverlay.keybind.SpotifyKeybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpotifyOverlayMod implements ClientModInitializer {
    public static final String MOD_ID = "spotifyoverlay";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static SpotifyModConfig config;
    public static SpotifyAuthManager authManager;
    public static SpotifyApiClient apiClient;
    public static SpotifyHudRenderer hudRenderer;

    @Override
    public void onInitializeClient() {
        config = SpotifyModConfig.load();
        authManager = new SpotifyAuthManager(config);
        apiClient = new SpotifyApiClient(authManager);
        hudRenderer = new SpotifyHudRenderer(config, authManager, apiClient);

        HudRenderCallback.EVENT.register(hudRenderer);
        SpotifyKeybinds.register();
        registerCommands();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("spotify")
                .then(ClientCommandManager.literal("login")
                    .executes(context -> {
                        SpotifyOverlayMod.authManager.startLoginFlow();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("logout")
                    .executes(context -> {
                        SpotifyOverlayMod.authManager.clearTokens();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("toggle")
                    .executes(context -> {
                        SpotifyOverlayMod.config.enabled = !SpotifyOverlayMod.config.enabled;
                        SpotifyOverlayMod.config.save();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("refresh")
                    .executes(context -> {
                        if (SpotifyOverlayMod.authManager.refreshAccessToken()) {
                            context.getSource().sendFeedback(Text.literal("Spotify token refreshed successfully."));
                        } else {
                            context.getSource().sendError(Text.literal("Spotify token refresh failed. Run /spotify login again."));
                        }
                        return 1;
                    })));
        });
    }
}
