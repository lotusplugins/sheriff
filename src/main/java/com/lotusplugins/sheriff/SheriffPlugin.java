package com.lotusplugins.sheriff;

import com.lotusplugins.sheriff.audit.AuditLog;
import com.lotusplugins.sheriff.command.NoteCommand;
import com.lotusplugins.sheriff.command.VanishCommand;
import com.lotusplugins.sheriff.note.NoteService;
import com.lotusplugins.sheriff.note.NoteStore;
import com.lotusplugins.sheriff.staff.LuckPermsLevels;
import com.lotusplugins.sheriff.staff.StaffLevels;
import com.lotusplugins.sheriff.vanish.VanishListener;
import com.lotusplugins.sheriff.vanish.VanishService;
import java.io.File;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
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
    private NoteStore noteStore;

    @Override
    public void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        var config = new PluginConfig(this, MiniMessage.miniMessage());
        var jdbcUrl = "jdbc:h2:" + new File(getDataFolder(), "sheriff").getAbsolutePath();
        Executor async = task -> getServer().getScheduler().runTaskAsynchronously(this, task);
        try {
            this.auditLog = new AuditLog(jdbcUrl, async);
            this.noteStore = new NoteStore(jdbcUrl, async);
        } catch (SQLException e) {
            throw new IllegalStateException("open database", e);
        }
        CommandManager<CommandSender> commandManager =
                LegacyPaperCommandManager.createNative(this, ExecutionCoordinator.asyncCoordinator());
        var vanish = new VanishService(this);
        getServer().getPluginManager().registerEvents(new VanishListener(vanish, config), this);
        new VanishCommand(this, vanish, auditLog, config, adventure).register(commandManager);
        var notes = new NoteService(noteStore, auditLog, getLogger());
        new NoteCommand(this, notes, config, adventure).register(commandManager);
        var staffLevels = new StaffLevels(this, offlineLevel());
    }

    private Function<UUID, CompletableFuture<Integer>> offlineLevel() {
        if (!getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            return id -> CompletableFuture.completedFuture(
                    getServer().getOfflinePlayer(id).isOp() ? StaffLevels.MAX_LEVEL : 0);
        }
        return new LuckPermsLevels(getServer())::levelOf;
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        if (this.noteStore != null) {
            try {
                this.noteStore.close();
            } catch (SQLException e) {
                getLogger().log(Level.WARNING, "close note store", e);
            }
            this.noteStore = null;
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
