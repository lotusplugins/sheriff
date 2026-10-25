package com.lotusplugins.sheriff.vanish;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class VanishService {
    private static final String SEE_VANISHED = "sheriff.vanish.see-vanished";

    private final Plugin plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishService(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean toggle(Player player) {
        var nowVanished = vanished.add(player.getUniqueId());
        if (!nowVanished) {
            vanished.remove(player.getUniqueId());
        }
        applyToViewers(player, nowVanished);
        return nowVanished;
    }

    public boolean isVanishedName(String name) {
        var player = plugin.getServer().getPlayerExact(name);
        return player != null && isVanished(player);
    }

    public boolean canSeeVanished(CommandSender viewer) {
        return viewer.hasPermission(SEE_VANISHED);
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public void reapply(Player player) {
        applyToViewers(player, true);
    }

    private void applyToViewers(Player player, boolean hidden) {
        for (var viewer : plugin.getServer().getOnlinePlayers()) {
            if (viewer.equals(player) || canSeeVanished(viewer)) continue;
            if (hidden) {
                viewer.hidePlayer(plugin, player);
            } else {
                viewer.showPlayer(plugin, player);
            }
        }
    }

    public void hideFrom(Player viewer) {
        if (canSeeVanished(viewer)) return;
        for (var player : plugin.getServer().getOnlinePlayers()) {
            if (vanished.contains(player.getUniqueId())) {
                viewer.hidePlayer(plugin, player);
            }
        }
    }
}
