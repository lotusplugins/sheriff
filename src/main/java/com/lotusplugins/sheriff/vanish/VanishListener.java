package com.lotusplugins.sheriff.vanish;

import com.lotusplugins.sheriff.PluginConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class VanishListener implements Listener {
    private final VanishService vanish;
    private final PluginConfig config;

    public VanishListener(VanishService vanish, PluginConfig config) {
        this.vanish = vanish;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        vanish.hideFrom(player);
        if (vanish.isVanished(player)) {
            vanish.reapply(player);
            if (config.vanishSuppressRealJoinLeaveMessages()) {
                event.setJoinMessage(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        if (config.vanishSuppressRealJoinLeaveMessages() && vanish.isVanished(event.getPlayer())) {
            event.setQuitMessage(null);
        }
    }
}
