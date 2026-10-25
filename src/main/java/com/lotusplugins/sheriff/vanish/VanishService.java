package com.lotusplugins.sheriff.vanish;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
        for (var viewer : plugin.getServer().getOnlinePlayers()) {
            if (viewer.equals(player) || viewer.hasPermission(SEE_VANISHED)) continue;
            if (nowVanished) {
                viewer.hidePlayer(plugin, player);
            } else {
                viewer.showPlayer(plugin, player);
            }
        }
        return nowVanished;
    }

    public void hideFrom(Player viewer) {
        if (viewer.hasPermission(SEE_VANISHED)) return;
        for (var player : plugin.getServer().getOnlinePlayers()) {
            if (vanished.contains(player.getUniqueId())) {
                viewer.hidePlayer(plugin, player);
            }
        }
    }

    public void forget(Player player) {
        vanished.remove(player.getUniqueId());
    }
}
