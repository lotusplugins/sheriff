package com.lotusplugins.sheriff.vanish;

import com.lotusplugins.sheriff.PluginConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.server.TabCompleteEvent;

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

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        var players = event.iterator();
        while (players.hasNext()) {
            if (vanish.isVanished(players.next())) {
                players.remove();
            }
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (vanish.canSeeVanished(event.getSender())) return;
        event.setCompletions(
                event.getCompletions().stream().filter(name -> !vanish.isVanishedName(name)).toList());
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player && vanish.isVanished(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && vanish.isVanished(player)) {
            event.setCancelled(true);
        }
    }
}
