package com.lotusplugins.sheriff.staff;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.plugin.Plugin;

public final class StaffLevels {
    public static final int MAX_LEVEL = 10;

    private final Plugin plugin;
    private final Function<UUID, CompletableFuture<Integer>> offlineLevel;

    public StaffLevels(Plugin plugin, Function<UUID, CompletableFuture<Integer>> offlineLevel) {
        this.plugin = plugin;
        this.offlineLevel = offlineLevel;
    }

    public CompletableFuture<Boolean> canAct(UUID actor, UUID target) {
        return CompletableFuture.supplyAsync(
                        () -> onlineLevel(actor)
                                .map(actorLevel -> level(target).thenApply(targetLevel -> actorLevel > targetLevel))
                                .orElseGet(() -> CompletableFuture.completedFuture(false)),
                        task -> plugin.getServer().getScheduler().runTask(plugin, task))
                .thenCompose(Function.identity());
    }

    static int levelOf(Predicate<String> hasPermission) {
        for (var level = MAX_LEVEL; level > 0; level--) {
            if (hasPermission.test("sheriff.level." + level)) return level;
        }
        return 0;
    }

    private CompletableFuture<Integer> level(UUID id) {
        return onlineLevel(id).map(CompletableFuture::completedFuture).orElseGet(() -> offlineLevel.apply(id));
    }

    private Optional<Integer> onlineLevel(UUID id) {
        return Optional.ofNullable(plugin.getServer().getPlayer(id)).map(player -> levelOf(player::hasPermission));
    }
}
