package de.somkat.iceboatracing.managers;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import de.somkat.iceboatracing.arena.Arena;
import de.somkat.iceboatracing.race.Race;
import de.somkat.iceboatracing.race.RacePlayer;
import de.somkat.iceboatracing.race.RaceState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RaceManager {

    private final IceBoatRacingPlugin plugin;
    private final Map<String, Race> activeRaces;
    private final Map<UUID, String> playerRaces; // Player UUID -> Race ID
    private final Map<String, BukkitTask> raceTasks; // Race ID -> Task

    public RaceManager(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.activeRaces = new ConcurrentHashMap<>();
        this.playerRaces = new ConcurrentHashMap<>();
        this.raceTasks = new ConcurrentHashMap<>();
    }

    public Race createRace(Arena arena) {
        String raceId = generateRaceId(arena);
        Race race = new Race(raceId, arena);
        activeRaces.put(raceId, race);

        plugin.getLogger().info("Created race " + raceId + " for arena " + arena.getName());
        return race;
    }

    public boolean joinRace(Player player, String arenaName) {
        // Check if player is already in a race
        if (playerRaces.containsKey(player.getUniqueId())) {
            return false;
        }

        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null || !arena.isValidForRace()) {
            return false;
        }

        // Find existing race or create new one
        Race race = findAvailableRace(arena);
        if (race == null) {
            race = createRace(arena);
        }

        if (race.addPlayer(player)) {
            playerRaces.put(player.getUniqueId(), race.getId());

            // Teleport player to spawn point
            teleportToSpawn(player, race);

            // Send join message
            plugin.getMessageUtil().sendMessage(player, "race.joined",
                    "arena", arena.getName(),
                    "players", String.valueOf(race.getPlayers().size()),
                    "max", String.valueOf(arena.getMaxPlayers()));

            // Check if race can start
            if (race.canStart()) {
                startRaceCountdown(race);
            }

            return true;
        }

        return false;
    }

    public boolean leaveRace(Player player) {
        String raceId = playerRaces.get(player.getUniqueId());
        if (raceId == null) {
            return false;
        }

        Race race = activeRaces.get(raceId);
        if (race == null) {
            playerRaces.remove(player.getUniqueId());
            return false;
        }

        if (race.removePlayer(player)) {
            playerRaces.remove(player.getUniqueId());

            // Remove boat if exists
            RacePlayer racePlayer = race.getPlayers().get(player.getUniqueId());
            if (racePlayer != null && racePlayer.getBoat() != null) {
                racePlayer.getBoat().remove();
            }

            plugin.getMessageUtil().sendMessage(player, "race.left");

            // End race if not enough players
            if (race.getState() == RaceState.RACING && race.getPlayers().size() < race.getArena().getMinPlayers()) {
                endRace(race, "Not enough players");
            }

            // Remove race if empty
            if (race.getPlayers().isEmpty()) {
                removeRace(race);
            }

            return true;
        }

        return false;
    }

    private void teleportToSpawn(Player player, Race race) {
        List<Location> spawnPoints = race.getArena().getSpawnPoints();
        if (spawnPoints.isEmpty()) return;

        int playerIndex = race.getPlayerOrder().indexOf(player.getUniqueId());
        Location spawnPoint = spawnPoints.get(playerIndex % spawnPoints.size());

        player.teleport(spawnPoint);

        // Spawn boat
        Boat boat = spawnPoint.getWorld().spawn(spawnPoint, Boat.class);
        boat.addPassenger(player);

        RacePlayer racePlayer = race.getPlayers().get(player.getUniqueId());
        if (racePlayer != null) {
            racePlayer.setBoat(boat);
        }
    }

    private void startRaceCountdown(Race race) {
        int countdownTime = plugin.getConfig().getInt("race.countdown-time", 10);
        race.startCountdown(countdownTime);

        BukkitTask task = new BukkitRunnable() {
            int timeLeft = countdownTime;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    startRace(race);
                    cancel();
                    return;
                }

                // Send countdown message
                for (RacePlayer racePlayer : race.getPlayers().values()) {
                    Player player = racePlayer.getPlayer();
                    plugin.getMessageUtil().sendMessage(player, "race.countdown", "time", String.valueOf(timeLeft));
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        raceTasks.put(race.getId(), task);
    }

    private void startRace(Race race) {
        race.start();

        // Send start message
        for (RacePlayer racePlayer : race.getPlayers().values()) {
            Player player = racePlayer.getPlayer();
            plugin.getMessageUtil().sendMessage(player, "race.started");
        }

        // Start race monitoring task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                updateRace(race);

                if (race.getState() == RaceState.FINISHED) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Update every 0.5 seconds

        raceTasks.put(race.getId() + "_monitor", task);

        plugin.getLogger().info("Started race " + race.getId());
    }

    private void updateRace(Race race) {
        if (race.getState() != RaceState.RACING) return;

        // Update player positions
        List<RacePlayer> ranked = race.getPlayersRanked();
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).setPosition(i + 1);
        }

        // Check for race completion
        boolean hasWinner = false;
        for (RacePlayer racePlayer : race.getPlayers().values()) {
            if (racePlayer.isFinished()) {
                if (race.getWinner() == null) {
                    race.setWinner(racePlayer.getPlayer().getUniqueId());
                }
                hasWinner = true;
            }
        }

        if (hasWinner) {
            endRace(race, "Race completed");
        }

        // Check for timeout
        long maxRaceTime = plugin.getConfig().getLong("race.max-race-time", 600) * 1000;
        if (race.getRaceTime() > maxRaceTime) {
            endRace(race, "Time limit reached");
        }
    }

    public void endRace(Race race, String reason) {
        race.end();

        // Cancel tasks
        BukkitTask task = raceTasks.remove(race.getId());
        if (task != null) task.cancel();

        task = raceTasks.remove(race.getId() + "_monitor");
        if (task != null) task.cancel();

        // Show results
        showRaceResults(race);

        // Save statistics
        plugin.getLeaderboardManager().saveRaceResults(race);

        // Schedule race cleanup
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupRace(race);
            }
        }.runTaskLater(plugin, 200L); // 10 seconds delay

        plugin.getLogger().info("Ended race " + race.getId() + ": " + reason);
    }

    private void showRaceResults(Race race) {
        List<RacePlayer> ranked = race.getPlayersRanked();

        for (RacePlayer racePlayer : race.getPlayers().values()) {
            Player player = racePlayer.getPlayer();

            plugin.getMessageUtil().sendMessage(player, "race.finished");
            plugin.getMessageUtil().sendMessage(player, "race.results.header");

            for (int i = 0; i < Math.min(ranked.size(), 5); i++) {
                RacePlayer rankedPlayer = ranked.get(i);
                plugin.getMessageUtil().sendMessage(player, "race.results.position",
                        "position", String.valueOf(i + 1),
                        "player", rankedPlayer.getPlayer().getName(),
                        "time", rankedPlayer.getFormattedTime(),
                        "checkpoints", String.valueOf(rankedPlayer.getCheckpointsReached()));
            }
        }
    }

    private void cleanupRace(Race race) {
        race.finish();

        // Remove players from race
        for (UUID playerId : new ArrayList<>(race.getPlayers().keySet())) {
            playerRaces.remove(playerId);

            RacePlayer racePlayer = race.getPlayers().get(playerId);
            if (racePlayer != null && racePlayer.getBoat() != null) {
                racePlayer.getBoat().remove();
            }
        }

        removeRace(race);
    }

    private void removeRace(Race race) {
        activeRaces.remove(race.getId());

        // Cancel any remaining tasks
        BukkitTask task = raceTasks.remove(race.getId());
        if (task != null) task.cancel();

        task = raceTasks.remove(race.getId() + "_monitor");
        if (task != null) task.cancel();
    }

    private Race findAvailableRace(Arena arena) {
        for (Race race : activeRaces.values()) {
            if (race.getArena().equals(arena) && race.getState().canJoin()
                    && race.getPlayers().size() < arena.getMaxPlayers()) {
                return race;
            }
        }
        return null;
    }

    private String generateRaceId(Arena arena) {
        return arena.getName() + "_" + System.currentTimeMillis();
    }

    public void stopAllRaces() {
        for (Race race : new ArrayList<>(activeRaces.values())) {
            endRace(race, "Server shutdown");
        }
    }

    // Getters
    public Race getRaceByPlayer(Player player) {
        String raceId = playerRaces.get(player.getUniqueId());
        return raceId != null ? activeRaces.get(raceId) : null;
    }

    public Collection<Race> getActiveRaces() {
        return activeRaces.values();
    }

    public Race getRace(String raceId) {
        return activeRaces.get(raceId);
    }

    public boolean isPlayerInRace(Player player) {
        return playerRaces.containsKey(player.getUniqueId());
    }
}