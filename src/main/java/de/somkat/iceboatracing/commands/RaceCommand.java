package de.somkat.iceboatracing.commands;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import de.somkat.iceboatracing.race.Race;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RaceCommand implements CommandExecutor, TabCompleter {

    private final IceBoatRacingPlugin plugin;

    public RaceCommand(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtil().sendMessage(sender, "commands.players-only");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("iceboatracing.play")) {
            plugin.getMessageUtil().sendMessage(player, "commands.no-permission");
            return true;
        }

        if (args.length == 0) {
            sendRaceHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "join":
                if (args.length < 2) {
                    plugin.getMessageUtil().sendMessage(player, "commands.race.usage.join");
                    return true;
                }
                joinRace(player, args[1]);
                break;

            case "leave":
            case "quit":
                leaveRace(player);
                break;

            case "list":
                listActiveRaces(player);
                break;

            case "status":
                showRaceStatus(player);
                break;

            default:
                sendRaceHelp(player);
                break;
        }

        return true;
    }

    private void sendRaceHelp(Player player) {
        plugin.getMessageUtil().sendMessage(player, "commands.race.help.header");
        plugin.getMessageUtil().sendMessage(player, "commands.race.help.join");
        plugin.getMessageUtil().sendMessage(player, "commands.race.help.leave");
        plugin.getMessageUtil().sendMessage(player, "commands.race.help.list");
        plugin.getMessageUtil().sendMessage(player, "commands.race.help.status");
    }

    private void joinRace(Player player, String arenaName) {
        // Check if player is already in a race
        if (plugin.getRaceManager().isPlayerInRace(player)) {
            plugin.getMessageUtil().sendMessage(player, "race.already-in-race");
            return;
        }

        // Check if arena exists
        if (plugin.getArenaManager().getArena(arenaName) == null) {
            plugin.getMessageUtil().sendMessage(player, "race.arena-not-found", "arena", arenaName);
            return;
        }

        // Attempt to join race
        boolean success = plugin.getRaceManager().joinRace(player, arenaName);

        if (!success) {
            plugin.getMessageUtil().sendMessage(player, "race.join-failed", "arena", arenaName);
        }
    }

    private void leaveRace(Player player) {
        if (!plugin.getRaceManager().isPlayerInRace(player)) {
            plugin.getMessageUtil().sendMessage(player, "race.not-in-race");
            return;
        }

        boolean success = plugin.getRaceManager().leaveRace(player);

        if (!success) {
            plugin.getMessageUtil().sendMessage(player, "race.leave-failed");
        }
    }

    private void listActiveRaces(Player player) {
        List<Race> activeRaces = new ArrayList<>(plugin.getRaceManager().getActiveRaces());

        if (activeRaces.isEmpty()) {
            plugin.getMessageUtil().sendMessage(player, "commands.race.list.no-races");
            return;
        }

        plugin.getMessageUtil().sendMessage(player, "commands.race.list.header");

        for (Race race : activeRaces) {
            plugin.getMessageUtil().sendMessage(player, "commands.race.list.entry",
                    "arena", race.getArena().getName(),
                    "players", String.valueOf(race.getPlayers().size()),
                    "max", String.valueOf(race.getArena().getMaxPlayers()),
                    "state", race.getState().getDisplayName());
        }
    }

    private void showRaceStatus(Player player) {
        Race race = plugin.getRaceManager().getRaceByPlayer(player);

        if (race == null) {
            plugin.getMessageUtil().sendMessage(player, "race.not-in-race");
            return;
        }

        plugin.getMessageUtil().sendMessage(player, "commands.race.status.header");
        plugin.getMessageUtil().sendMessage(player, "commands.race.status.arena",
                "arena", race.getArena().getName());
        plugin.getMessageUtil().sendMessage(player, "commands.race.status.state",
                "state", race.getState().getDisplayName());
        plugin.getMessageUtil().sendMessage(player, "commands.race.status.players",
                "current", String.valueOf(race.getPlayers().size()),
                "max", String.valueOf(race.getArena().getMaxPlayers()));

        if (race.getState().isActive()) {
            plugin.getMessageUtil().sendMessage(player, "commands.race.status.time",
                    "time", formatTime(race.getRaceTime()));

            int position = race.getPlayerPosition(player.getUniqueId());
            if (position > 0) {
                plugin.getMessageUtil().sendMessage(player, "commands.race.status.position",
                        "position", String.valueOf(position));
            }
        }
    }

    private String formatTime(long timeMillis) {
        long minutes = timeMillis / 60000;
        long seconds = (timeMillis % 60000) / 1000;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String[] subCommands = {"join", "leave", "quit", "list", "status"};
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            for (String arenaName : plugin.getArenaManager().getArenaNames()) {
                if (arenaName.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(arenaName);
                }
            }
        }

        return completions;
    }
}