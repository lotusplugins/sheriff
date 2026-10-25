package com.lotusplugins.sheriff;

import com.lotusplugins.sheriff.audit.AuditLog;
import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public class SheriffPlugin extends JavaPlugin {

    private BukkitAudiences adventure;
    private AuditLog auditLog;

    @Override
    public void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        var config = new PluginConfig(this, MiniMessage.miniMessage());
        try {
            this.auditLog = new AuditLog(
                    "jdbc:h2:" + new File(getDataFolder(), "sheriff").getAbsolutePath(),
                    task -> getServer().getScheduler().runTaskAsynchronously(this, task));
        } catch (SQLException e) {
            throw new IllegalStateException("open audit log", e);
        }
        CommandManager<CommandSender> commandManager =
                LegacyPaperCommandManager.createNative(this, ExecutionCoordinator.asyncCoordinator());
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        if (this.auditLog != null) {
            try {
                this.auditLog.close();
            } catch (SQLException e) {
                getLogger().log(Level.WARNING, "close audit log", e);
            }
            this.auditLog = null;
        }
    }
}
