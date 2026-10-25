package com.lotusplugins.sheriff.command;

import com.lotusplugins.sheriff.PluginConfig;
import com.lotusplugins.sheriff.audit.AuditAction;
import com.lotusplugins.sheriff.audit.AuditEntry;
import com.lotusplugins.sheriff.audit.AuditLog;
import com.lotusplugins.sheriff.vanish.VanishService;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Level;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.CommandManager;

public final class VanishCommand {
    private final Plugin plugin;
    private final VanishService vanish;
    private final AuditLog auditLog;
    private final PluginConfig config;
    private final BukkitAudiences adventure;

    public VanishCommand(
            Plugin plugin, VanishService vanish, AuditLog auditLog, PluginConfig config, BukkitAudiences adventure) {
        this.plugin = plugin;
        this.vanish = vanish;
        this.auditLog = auditLog;
        this.config = config;
        this.adventure = adventure;
    }

    public void register(CommandManager<CommandSender> manager) {
        manager.command(manager.commandBuilder("vanish")
                .permission("sheriff.vanish")
                .senderType(Player.class)
                .handler(context -> plugin.getServer().getScheduler().runTask(plugin, () -> toggle(context.sender()))));
    }

    private void toggle(Player player) {
        var nowVanished = vanish.toggle(player);
        var action = nowVanished ? AuditAction.VANISH : AuditAction.UNVANISH;
        adventure.player(player).sendMessage(config.message(nowVanished ? "vanish.enabled" : "vanish.disabled"));
        if (config.vanishFakeJoinLeaveMessages()) {
            adventure
                    .all()
                    .sendMessage(config.message(
                            nowVanished ? "vanish.fake-leave" : "vanish.fake-join",
                            Placeholder.unparsed("player", player.getName())));
        }
        auditLog.record(new AuditEntry(Instant.now(), player.getUniqueId(), action, Optional.empty(), ""))
                .exceptionally(error -> {
                    plugin.getLogger().log(Level.WARNING, "record audit entry", error);
                    return null;
                });
    }
}
