package de.somkat.iceboatracing.config;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ConfigManager {

    private final IceBoatRacingPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration arenas;

    public ConfigManager(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        // Save default config
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        // Load messages
        loadMessages();

        // Load arenas
        loadArenasConfig();

        // Set default values if not present
        setDefaults();
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void loadArenasConfig() {
        File arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenasFile.exists()) {
            try {
                arenasFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create arenas.yml file!");
            }
        }
        arenas = YamlConfiguration.loadConfiguration(arenasFile);
    }

    private void setDefaults() {
        // Database settings
        config.addDefault("database.type", "sqlite");
        config.addDefault("database.host", "localhost");
        config.addDefault("database.port", 3306);
        config.addDefault("database.database", "iceboatracing");
        config.addDefault("database.username", "root");
        config.addDefault("database.password", "");

        // Race settings
        config.addDefault("race.countdown-time", 10);
        config.addDefault("race.max-race-time", 600); // 10 minutes
        config.addDefault("race.reset-cooldown", 3);
        config.addDefault("race.auto-reset-distance", 20);

        // Power-up settings
        config.addDefault("powerups.enabled", true);
        config.addDefault("powerups.respawn-time", 30);
        config.addDefault("powerups.boost-duration", 5);
        config.addDefault("powerups.shield-duration", 10);
        config.addDefault("powerups.blocker-duration", 15);

        // GUI settings
        config.addDefault("gui.enabled", true);
        config.addDefault("gui.update-interval", 20); // 1 second

        // Boat settings
        config.addDefault("boat.speed-multiplier", 1.5);
        config.addDefault("boat.boost-multiplier", 2.5);
        config.addDefault("boat.brake-multiplier", 0.3);

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public void saveArenas() {
        try {
            arenas.save(new File(plugin.getDataFolder(), "arenas.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save arenas.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getArenas() {
        return arenas;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadMessages();
        loadArenasConfig();
    }
}