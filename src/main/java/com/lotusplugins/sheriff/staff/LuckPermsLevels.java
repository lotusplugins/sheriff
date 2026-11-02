package com.lotusplugins.sheriff.staff;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Server;

public final class LuckPermsLevels {
    private final LuckPerms luckPerms;

    public LuckPermsLevels(Server server) {
        this.luckPerms = server.getServicesManager().load(LuckPerms.class);
    }

    public CompletableFuture<Integer> levelOf(UUID id) {
        return luckPerms.getUserManager().loadUser(id).thenApply(user -> {
            var permissions = user.getCachedData().getPermissionData(QueryOptions.nonContextual());
            return StaffLevels.levelOf(node -> permissions.checkPermission(node).asBoolean());
        });
    }
}
