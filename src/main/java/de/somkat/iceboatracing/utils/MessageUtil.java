package de.somkat.iceboatracing.utils;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class MessageUtil {

    private final IceBoatRacingPlugin plugin;
    private FileConfiguration messages;

    public MessageUtil(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getConfigManager().getMessages();
    }

    public void sendMessage(CommandSender sender, String key, String... replacements) {
        String message = getMessage(key, replacements);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public String getMessage(String key, String... replacements) {
        String message = messages.getString(key);
        if (message == null) {
            message = "§cMessage not found: " + key;
            plugin.getLogger().warning("Missing message key: " + key);
        }

        // Apply replacements
        if (replacements.length > 0) {
            Map<String, String> replacementMap = new HashMap<>();
            for (int i = 0; i < replacements.length - 1; i += 2) {
                replacementMap.put(replacements[i], replacements[i + 1]);
            }

            for (Map.Entry<String, String> entry : replacementMap.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void broadcast(String key, String... replacements) {
        String message = getMessage(key, replacements);
        if (message != null && !message.isEmpty()) {
            plugin.getServer().broadcastMessage(message);
        }
    }

    public void reloadMessages() {
        this.messages = plugin.getConfigManager().getMessages();
    }
}