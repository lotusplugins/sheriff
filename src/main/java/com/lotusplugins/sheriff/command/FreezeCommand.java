package com.lotusplugins.sheriff.command;

import static org.incendo.cloud.parser.standard.StringParser.stringParser;

import com.lotusplugins.sheriff.PluginConfig;
import com.lotusplugins.sheriff.freeze.FreezeService;
import com.lotusplugins.sheriff.vanish.VanishService;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.logging.Level;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.CommandManager;

public final class FreezeCommand {
    private final Plugin plugin;
    private final FreezeService freeze;
    private final VanishService vanish;
    private final PluginConfig config;
    private final BukkitAudiences adventure;
    private final Executor mainThread;

    public FreezeCommand(
            Plugin plugin, FreezeService freeze, VanishService vanish, PluginConfig config, BukkitAudiences adventure) {
        this.plugin = plugin;
        this.freeze = freeze;
        this.vanish = vanish;
        this.config = config;
        this.adventure = adventure;
        this.mainThread = task -> plugin.getServer().getScheduler().runTask(plugin, task);
    }

    public void register(CommandManager<CommandSender> manager) {
        manager.command(manager.commandBuilder("freeze")
                .permission("sheriff.freeze")
                .senderType(Player.class)
                .required("player", stringParser())
                .handler(context -> change(
                        context.sender(), context.get("player"), true, freeze::freeze, "freeze.frozen", "freeze.notice-frozen")));
        manager.command(manager.commandBuilder("unfreeze")
                .permission("sheriff.freeze")
                .senderType(Player.class)
                .required("player", stringParser())
                .handler(context -> change(
                        context.sender(),
                        context.get("player"),
                        false,
                        freeze::unfreeze,
                        "freeze.unfrozen",
                        "freeze.notice-unfrozen")));
    }

    private void change(
            Player sender,
            String name,
            boolean onlineOnly,
            BiFunction<UUID, UUID, CompletableFuture<FreezeService.Result>> action,
            String doneKey,
            String noticeKey) {
        var player = Placeholder.unparsed("player", name);
        CompletableFuture.supplyAsync(() -> find(sender, name, onlineOnly), mainThread)
                .thenCompose(found -> found.map(target -> action.apply(sender.getUniqueId(), target.getUniqueId())
                                .thenCompose(result -> switch (result) {
                                    case DONE -> reply(sender, doneKey, player)
                                            .thenCompose(done -> notice(target, noticeKey));
                                    case UNCHANGED -> reply(sender, "freeze.unchanged", player);
                                    case DENIED -> reply(sender, "freeze.denied", player);
                                }))
                        .orElseGet(() -> reply(sender, "freeze.unknown-player", player)))
                .exceptionally(error -> failed(sender, error));
    }

    private Optional<OfflinePlayer> find(Player sender, String name, boolean onlineOnly) {
        var online = Optional.ofNullable(plugin.getServer().getPlayerExact(name))
                .filter(player -> vanish.canSeeVanished(sender) || !vanish.isVanished(player))
                .<OfflinePlayer>map(player -> player);
        if (onlineOnly) {
            return online;
        }
        return online.or(() -> Arrays.stream(plugin.getServer().getOfflinePlayers())
                .filter(player -> !player.isOnline() && name.equalsIgnoreCase(player.getName()))
                .findFirst());
    }

    private CompletableFuture<Void> notice(OfflinePlayer target, String key) {
        return CompletableFuture.runAsync(
                () -> Optional.ofNullable(target.getPlayer())
                        .ifPresent(player -> adventure.player(player).sendMessage(config.message(key))),
                mainThread);
    }

    private CompletableFuture<Void> reply(Player sender, String key, TagResolver... resolvers) {
        return CompletableFuture.runAsync(
                () -> adventure.player(sender).sendMessage(config.message(key, resolvers)), mainThread);
    }

    private Void failed(Player sender, Throwable error) {
        plugin.getLogger().log(Level.SEVERE, "freeze command failed for " + sender.getUniqueId(), error);
        reply(sender, "freeze.failed");
        return null;
    }
}
