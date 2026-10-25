package com.lotusplugins.sheriff.vanish;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class VanishListener implements Listener {
    private final VanishService vanish;

    public VanishListener(VanishService vanish) {
        this.vanish = vanish;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        vanish.hideFrom(player);
        if (vanish.isVanished(player)) {
            vanish.reapply(player);
            event.setJoinMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        if (vanish.isVanished(event.getPlayer())) {
            event.setQuitMessage(null);
        }
    }
}
