package de.somkat.iceboatracing.commands;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import de.somkat.iceboatracing.arena.Arena;
import de.somkat.iceboatracing.managers.LeaderboardManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class IceBoatRacingCommand implements CommandExecutor, TabCompleter {

    private final IceBoatRacingPlugin plugin;

    public IceBoatRacingCommand(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelp(sender);
                break;

            case "list":
                listArenas(sender);
                break;

            case "info":
                if (args.length < 2) {
                    plugin.getMessageUtil().sendMessage(sender, "commands.usage.info");
                    return true;
                }
                showArenaInfo(sender, args[1]);
                break;

            case "stats":
                if (!(sender instanceof Player)) {
                    plugin.getMessageUtil().sendMessage(sender, "commands.players-only");
                    return true;
                }
                showPlayerStats((Player) sender, args.length > 1 ? args[1] : null);
                break;

            case "leaderboard":
            case "top":
                showLeaderboard(sender, args.length > 1 ? args[1] : null,
                        args.length > 2 ? args[2] : "wins");
                break;

            case "setup":
                if (!sender.hasPermission("iceboatracing.setup")) {
                    plugin.getMessageUtil().sendMessage(sender, "commands.no-permission");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    plugin.getMessageUtil().sendMessage(sender, "commands.players-only");
                    return true;
                }
                handleSetup((Player) sender, Arrays.copyOfRange(args, 1, args.length));
                break;

            case "reload":
                if (!sender.hasPermission("iceboatracing.admin")) {
                    plugin.getMessageUtil().sendMessage(sender, "commands.no-permission");
                    return true;
                }
                reloadPlugin(sender);
                break;

            default:
                plugin.getMessageUtil().sendMessage(sender, "commands.unknown");
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        plugin.getMessageUtil().sendMessage(sender, "commands.help.header");
        plugin.getMessageUtil().sendMessage(sender, "commands.help.list");
        plugin.getMessageUtil().sendMessage(sender, "commands.help.info");
        plugin.getMessageUtil().sendMessage(sender, "commands.help.stats");
        plugin.getMessageUtil().sendMessage(sender, "commands.help.leaderboard");

        if (sender.hasPermission("iceboatracing.setup")) {
            plugin.getMessageUtil().sendMessage(sender, "commands.help.setup");
        }

        if (sender.hasPermission("iceboatracing.admin")) {
            plugin.getMessageUtil().sendMessage(sender, "commands.help.reload");
        }
    }

    private void listArenas(CommandSender sender) {
        List<String> arenaNames = plugin.getArenaManager().getArenaNames();

        if (arenaNames.isEmpty()) {
            plugin.getMessageUtil().sendMessage(sender, "commands.list.no-arenas");
            return;
        }

        plugin.getMessageUtil().sendMessage(sender, "commands.list.header");
        for (String arenaName : arenaNames) {
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            AtomicInteger activePlayers = new AtomicInteger(0);

            plugin.getRaceManager().getActiveRaces().stream()
                    .filter(race -> race.getArena().getName().equals(arenaName))
                    .forEach(race -> activePlayers.addAndGet(race.getPlayers().size()));

            plugin.getMessageUtil().sendMessage(sender, "commands.list.arena",
                    "name", arenaName,
                    "players", String.valueOf(activePlayers.get()),
                    "max", String.valueOf(arena.getMaxPlayers()));
        }
    }

    private void showArenaInfo(CommandSender sender, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            plugin.getMessageUtil().sendMessage(sender, "commands.info.not-found", "arena", arenaName);
            return;
        }

        plugin.getMessageUtil().sendMessage(sender, "commands.info.header", "arena", arenaName);
        plugin.getMessageUtil().sendMessage(sender, "commands.info.world", "world", arena.getWorldName());
        plugin.getMessageUtil().sendMessage(sender, "commands.info.players",
                "min", String.valueOf(arena.getMinPlayers()),
                "max", String.valueOf(arena.getMaxPlayers()));
        plugin.getMessageUtil().sendMessage(sender, "commands.info.checkpoints",
                "count", String.valueOf(arena.getCheckpoints().size()));
        plugin.getMessageUtil().sendMessage(sender, "commands.info.spawns",
                "count", String.valueOf(arena.getSpawnPoints().size()));
        plugin.getMessageUtil().sendMessage(sender, "commands.info.powerups",
                "count", String.valueOf(arena.getPowerUpSpawns().size()));
    }

    private void showPlayerStats(Player player, String arenaName) {
        if (arenaName == null) {
            plugin.getMessageUtil().sendMessage(player, "commands.stats.select-arena");
            listArenas(player);
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            plugin.getMessageUtil().sendMessage(player, "commands.stats.arena-not-found", "arena", arenaName);
            return;
        }

        LeaderboardManager.PlayerStats stats = plugin.getLeaderboardManager()
                .getPlayerStats(player.getUniqueId(), arenaName);

        if (stats == null) {
            plugin.getMessageUtil().sendMessage(player, "commands.stats.no-data", "arena", arenaName);
            return;
        }

        plugin.getMessageUtil().sendMessage(player, "commands.stats.header",
                "player", player.getName(), "arena", arenaName);
        plugin.getMessageUtil().sendMessage(player, "commands.stats.races",
                "count", String.valueOf(stats.racesPlayed));
        plugin.getMessageUtil().sendMessage(player, "commands.stats.wins",
                "count", String.valueOf(stats.wins));
        plugin.getMessageUtil().sendMessage(player, "commands.stats.best-time",
                "time", formatTime(stats.bestTime));
        plugin.getMessageUtil().sendMessage(player, "commands.stats.avg-position",
                "position", String.format("%.1f", stats.averagePosition));
    }

    private void showLeaderboard(CommandSender sender, String arenaName, String sortBy) {
        if (arenaName == null) {
            plugin.getMessageUtil().sendMessage(sender, "commands.leaderboard.select-arena");
            listArenas(sender);
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            plugin.getMessageUtil().sendMessage(sender, "commands.leaderboard.arena-not-found", "arena", arenaName);
            return;
        }

        List<LeaderboardManager.LeaderboardEntry> entries = plugin.getLeaderboardManager()
                .getTopPlayers(arenaName, sortBy, 10);

        if (entries.isEmpty()) {
            plugin.getMessageUtil().sendMessage(sender, "commands.leaderboard.no-data", "arena", arenaName);
            return;
        }

        plugin.getMessageUtil().sendMessage(sender, "commands.leaderboard.header",
                "arena", arenaName, "sort", sortBy);

        for (int i = 0; i < entries.size(); i++) {
            LeaderboardManager.LeaderboardEntry entry = entries.get(i);
            plugin.getMessageUtil().sendMessage(sender, "commands.leaderboard.entry",
                    "rank", String.valueOf(i + 1),
                    "player", entry.playerName,
                    "wins", String.valueOf(entry.wins),
                    "races", String.valueOf(entry.racesPlayed),
                    "time", entry.getFormattedTime());
        }
    }

    private void handleSetup(Player player, String[] args) {
        plugin.getMessageUtil().sendMessage(player, "commands.setup.info");
    }

    private void reloadPlugin(CommandSender sender) {
        plugin.getConfigManager().reloadConfig();
        plugin.getMessageUtil().reloadMessages();
        plugin.getArenaManager().loadArenas();

        plugin.getMessageUtil().sendMessage(sender, "commands.reload.success");
    }

    private String formatTime(long timeMillis) {
        if (timeMillis == 0) return "N/A";

        long minutes = timeMillis / 60000;
        long seconds = (timeMillis % 60000) / 1000;
        long millis = (timeMillis % 1000) / 10;

        return String.format("%02d:%02d.%02d", minutes, seconds, millis);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String[] subCommands = {"help", "list", "info", "stats", "leaderboard", "top"};
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }

            if (sender.hasPermission("iceboatracing.setup")) {
                if ("setup".startsWith(args[0].toLowerCase())) {
                    completions.add("setup");
                }
            }

            if (sender.hasPermission("iceboatracing.admin")) {
                if ("reload".startsWith(args[0].toLowerCase())) {
                    completions.add("reload");
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("info") || subCommand.equals("stats") ||
                    subCommand.equals("leaderboard") || subCommand.equals("top")) {

                for (String arenaName : plugin.getArenaManager().getArenaNames()) {
                    if (arenaName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(arenaName);
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("leaderboard") || subCommand.equals("top")) {
                String[] sortOptions = {"wins", "time", "races"};
                for (String option : sortOptions) {
                    if (option.startsWith(args[2].toLowerCase())) {
                        completions.add(option);
                    }
                }
            }
        }

        return completions;
    }
}
