package com.lotusplugins.sheriff.freeze;

import com.lotusplugins.sheriff.PluginConfig;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class FreezeListener implements Listener {
    private final FreezeService freeze;
    private final PluginConfig config;
    private final Logger logger;

    public FreezeListener(FreezeService freeze, PluginConfig config, Logger logger) {
        this.freeze = freeze;
        this.config = config;
        this.logger = logger;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            freeze.load(event.getUniqueId()).join();
        } catch (CompletionException e) {
            logger.log(Level.SEVERE, "load freeze state for " + event.getUniqueId(), e);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, config.raw("freeze.load-failed"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        freeze.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.getFrom().toVector().equals(event.getTo().toVector())) {
            cancelIfFrozen(event.getPlayer(), event);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        cancelIfFrozen(event.getPlayer(), event);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        cancelIfFrozen(event.getPlayer(), event);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        cancelIfFrozen(event.getPlayer(), event);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        cancelIfFrozen(event.getPlayer(), event);
    }

    @EventHandler
    public void onDamaged(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            cancelIfFrozen(player, event);
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            cancelIfFrozen(player, event);
        }
    }

    private void cancelIfFrozen(Player player, Cancellable event) {
        if (freeze.isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
