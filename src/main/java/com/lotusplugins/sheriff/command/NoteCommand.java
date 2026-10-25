package com.lotusplugins.sheriff.command;

import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static org.incendo.cloud.parser.standard.LongParser.longParser;
import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

import com.lotusplugins.sheriff.PluginConfig;
import com.lotusplugins.sheriff.note.Note;
import com.lotusplugins.sheriff.note.NoteService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Level;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.CommandManager;

public final class NoteCommand {
    private static final int MAX_BODY_LENGTH = 256;
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final Plugin plugin;
    private final NoteService notes;
    private final PluginConfig config;
    private final BukkitAudiences adventure;
    private final Executor mainThread;

    public NoteCommand(Plugin plugin, NoteService notes, PluginConfig config, BukkitAudiences adventure) {
        this.plugin = plugin;
        this.notes = notes;
        this.config = config;
        this.adventure = adventure;
        this.mainThread = task -> plugin.getServer().getScheduler().runTask(plugin, task);
    }

    public void register(CommandManager<CommandSender> manager) {
        manager.command(manager.commandBuilder("note")
                .permission("sheriff.notes")
                .senderType(Player.class)
                .required("player", stringParser())
                .required("text", greedyStringParser())
                .handler(context -> add(context.sender(), context.get("player"), context.get("text"))));
        var base = manager.commandBuilder("notes").senderType(Player.class);
        manager.command(base.permission("sheriff.notes")
                .required("player", stringParser())
                .optional("page", integerParser(1))
                .handler(context -> list(context.sender(), context.get("player"), context.getOrDefault("page", 1))));
        manager.command(base.permission("sheriff.notes.delete")
                .literal("delete")
                .required("id", longParser(1))
                .handler(context -> delete(context.sender(), context.get("id"))));
    }

    private void add(Player sender, String playerName, String text) {
        var body = text.trim();
        if (body.isEmpty() || body.length() > MAX_BODY_LENGTH) {
            reply(sender, "notes.invalid-length");
            return;
        }
        withTarget(sender, playerName, target -> notes.add(sender.getUniqueId(), target.getUniqueId(), body)
                .thenCompose(note -> reply(
                        sender,
                        "notes.added",
                        Placeholder.unparsed("id", String.valueOf(note.id())),
                        Placeholder.unparsed("player", nameOf(target)))));
    }

    private void list(Player sender, String playerName, int page) {
        withTarget(sender, playerName, target -> notes.list(target.getUniqueId(), page)
                .thenAcceptAsync(found -> show(sender, target, page, found), mainThread));
    }

    private void delete(Player sender, long id) {
        var idPlaceholder = Placeholder.unparsed("id", String.valueOf(id));
        notes.delete(sender.getUniqueId(), id)
                .thenCompose(deleted -> reply(
                        sender, deleted.isPresent() ? "notes.deleted" : "notes.not-found", idPlaceholder))
                .exceptionally(error -> failed(sender, error));
    }

    private void withTarget(Player sender, String name, Function<OfflinePlayer, CompletableFuture<Void>> action) {
        CompletableFuture.supplyAsync(() -> findPlayer(name), mainThread)
                .thenCompose(found -> found.map(action)
                        .orElseGet(() -> reply(sender, "notes.unknown-player", Placeholder.unparsed("player", name))))
                .exceptionally(error -> failed(sender, error));
    }

    private Optional<OfflinePlayer> findPlayer(String name) {
        return Optional.<OfflinePlayer>ofNullable(plugin.getServer().getPlayerExact(name))
                .or(() -> Arrays.stream(plugin.getServer().getOfflinePlayers())
                        .filter(player -> name.equalsIgnoreCase(player.getName()))
                        .findFirst());
    }

    private void show(Player sender, OfflinePlayer target, int page, List<Note> found) {
        var audience = adventure.player(sender);
        var player = Placeholder.unparsed("player", nameOf(target));
        var pageNumber = Placeholder.unparsed("page", String.valueOf(page));
        if (found.isEmpty()) {
            audience.sendMessage(config.message("notes.empty", player, pageNumber));
            return;
        }
        audience.sendMessage(config.message("notes.header", player, pageNumber));
        found.forEach(note -> audience.sendMessage(config.message(
                "notes.line",
                Placeholder.unparsed("id", String.valueOf(note.id())),
                Placeholder.unparsed("author", nameOf(plugin.getServer().getOfflinePlayer(note.author()))),
                Placeholder.unparsed("date", DATE.format(note.createdAt())),
                Placeholder.unparsed("body", note.body()))));
    }

    private String nameOf(OfflinePlayer player) {
        return Optional.ofNullable(player.getName()).orElseGet(() -> player.getUniqueId().toString());
    }

    private CompletableFuture<Void> reply(Player sender, String key, TagResolver... resolvers) {
        return CompletableFuture.runAsync(
                () -> adventure.player(sender).sendMessage(config.message(key, resolvers)), mainThread);
    }

    private Void failed(Player sender, Throwable error) {
        plugin.getLogger().log(Level.SEVERE, "notes command failed for " + sender.getUniqueId(), error);
        reply(sender, "notes.failed");
        return null;
    }
}
