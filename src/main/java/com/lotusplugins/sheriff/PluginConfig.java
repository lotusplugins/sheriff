package com.lotusplugins.sheriff;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfig {

    private static final String DEFAULT_LANGUAGE_PATH = "lang/en_US.yml";

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private final YamlConfiguration messages;

    public PluginConfig(JavaPlugin plugin, MiniMessage miniMessage) {
        this.plugin = plugin;
        this.miniMessage = miniMessage;
        plugin.saveDefaultConfig();
        this.messages = load("lang/" + plugin.getConfig().getString("language", "en_US") + ".yml");
        this.messages.setDefaults(YamlConfiguration.loadConfiguration(
                new InputStreamReader(plugin.getResource(DEFAULT_LANGUAGE_PATH), StandardCharsets.UTF_8)));
    }

    private YamlConfiguration load(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists() && plugin.getResource(path) != null) {
            plugin.saveResource(path, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public boolean vanishFakeJoinLeaveMessages() {
        return plugin.getConfig().getBoolean("vanish.fake-join-leave-messages", true);
    }

    public boolean vanishSuppressRealJoinLeaveMessages() {
        return plugin.getConfig().getBoolean("vanish.suppress-real-join-leave-messages", true);
    }

    public String raw(String key) {
        return messages.getString(key, key);
    }

    public Component message(String key, TagResolver... resolvers) {
        return miniMessage.deserialize(raw(key), resolvers);
    }
}
