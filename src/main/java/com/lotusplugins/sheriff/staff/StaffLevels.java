package com.lotusplugins.sheriff.staff;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import org.bukkit.plugin.Plugin;

public final class StaffLevels {
    static final int MAX_LEVEL = 10;

    private final Plugin plugin;

    public StaffLevels(Plugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Boolean> canAct(UUID actor, UUID target) {
        return CompletableFuture.supplyAsync(
                () -> onlineLevel(actor).map(level -> level > onlineLevel(target).orElse(0)).orElse(false),
                task -> plugin.getServer().getScheduler().runTask(plugin, task));
    }

    static int levelOf(Predicate<String> hasPermission) {
        for (var level = MAX_LEVEL; level > 0; level--) {
            if (hasPermission.test("sheriff.level." + level)) return level;
        }
        return 0;
    }

    private Optional<Integer> onlineLevel(UUID id) {
        return Optional.ofNullable(plugin.getServer().getPlayer(id)).map(player -> levelOf(player::hasPermission));
    }
}
