package com.lotusplugins.sheriff.vanish;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class VanishListener implements Listener {
    private final VanishService vanish;

    public VanishListener(VanishService vanish) {
        this.vanish = vanish;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        vanish.hideFrom(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        vanish.forget(event.getPlayer());
    }
}
