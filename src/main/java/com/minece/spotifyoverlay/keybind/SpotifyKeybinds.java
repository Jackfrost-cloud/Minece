package com.minece.spotifyoverlay.keybind;

import com.minece.spotifyoverlay.SpotifyOverlayMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SpotifyKeybinds {
    private static KeyBinding nextTrack;
    private static KeyBinding previousTrack;
    private static KeyBinding togglePlayback;
    private static KeyBinding openSpotify;
    private static KeyBinding toggleHud;

    public static void register() {
        nextTrack = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.spotifyoverlay.next",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "category.spotifyoverlay"
        ));

        previousTrack = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.spotifyoverlay.previous",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "category.spotifyoverlay"
        ));

        togglePlayback = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.spotifyoverlay.toggle_playback",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "category.spotifyoverlay"
        ));

        openSpotify = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.spotifyoverlay.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "category.spotifyoverlay"
        ));

        toggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.spotifyoverlay.toggle_hud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "category.spotifyoverlay"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (nextTrack.wasPressed()) {
                SpotifyOverlayMod.apiClient.nextTrack();
            }
            while (previousTrack.wasPressed()) {
                SpotifyOverlayMod.apiClient.previousTrack();
            }
            while (togglePlayback.wasPressed()) {
                if (SpotifyOverlayMod.hudRenderer != null) {
                    SpotifyOverlayMod.hudRenderer.togglePlayback();
                }
            }
            while (openSpotify.wasPressed()) {
                SpotifyOverlayMod.apiClient.openCurrentTrack();
            }
            while (toggleHud.wasPressed()) {
                SpotifyOverlayMod.config.enabled = !SpotifyOverlayMod.config.enabled;
                SpotifyOverlayMod.config.save();
            }
        });
    }
}
