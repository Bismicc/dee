package de.somkat.iceboatracing.managers;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import de.somkat.iceboatracing.race.Race;
import de.somkat.iceboatracing.race.RacePlayer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class GuiManager {

    private final IceBoatRacingPlugin plugin;
    private BukkitTask updateTask;

    public GuiManager(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    public void startUpdateTask() {
        if (!plugin.getConfig().getBoolean("gui.enabled", true)) {
            return;
        }

        int interval = plugin.getConfig().getInt("gui.update-interval", 20);

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllPlayerGuis();
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void updateAllPlayerGuis() {
        for (Race race : plugin.getRaceManager().getActiveRaces()) {
            for (RacePlayer racePlayer : race.getPlayers().values()) {
                updatePlayerGui(racePlayer.getPlayer(), race, racePlayer);
            }
        }
    }

    public void updatePlayerGui(Player player, Race race, RacePlayer racePlayer) {
        if (!plugin.getConfig().getBoolean("gui.enabled", true)) {
            return;
        }

        String gui = buildGuiString(race, racePlayer);
        sendActionBar(player, gui);
    }

    private String buildGuiString(Race race, RacePlayer racePlayer) {
        StringBuilder gui = new StringBuilder();

        gui.append("§b§l▬▬▬ IceBoatRacing ▬▬▬\n");
        gui.append("§fMap: §e").append(race.getArena().getName()).append("\n");
        gui.append("§fRound: §e").append(race.getRound()).append("\n");
        gui.append("§fPlace: §e").append(racePlayer.getPosition()).append("§f/§e").append(race.getPlayers().size()).append("\n");
        gui.append("§fCheckpoints: §e").append(racePlayer.getCheckpointsReached()).append("§f/§e").append(racePlayer.getTotalCheckpoints()).append("\n");
        gui.append("§fTime: §e").append(racePlayer.getFormattedTime()).append("\n");

        // Show leader info
        RacePlayer leader = getLeader(race);
        if (leader != null && !leader.equals(racePlayer)) {
            gui.append("§fLeader: §e").append(leader.getPlayer().getName()).append("\n");
        }

        // Show race state
        gui.append("§fState: §e").append(race.getState().getDisplayName()).append("\n");

        if (race.getState().name().equals("COUNTDOWN")) {
            gui.append("§fStarting in: §c").append(race.getCountdownTime()).append("s\n");
        }

        gui.append("§b▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        return gui.toString();
    }

    private RacePlayer getLeader(Race race) {
        return race.getPlayersRanked().stream().findFirst().orElse(null);
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public void sendSidebar(Player player, Race race, RacePlayer racePlayer) {
        // Alternative implementation using scoreboard
        // This would create a persistent sidebar instead of action bar
        // Implementation would depend on your preference
    }
}