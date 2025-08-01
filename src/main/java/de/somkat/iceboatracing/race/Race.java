package de.somkat.iceboatracing.race;

import de.somkat.iceboatracing.arena.Arena;
import org.bukkit.entity.Player;

import java.util.*;

public class Race {

    private String id;
    private Arena arena;
    private RaceState state;
    private Map<UUID, RacePlayer> players;
    private List<UUID> playerOrder; // For spawn positions
    private long startTime;
    private long endTime;
    private int countdownTime;
    private UUID winner;
    private int round;

    public Race(String id, Arena arena) {
        this.id = id;
        this.arena = arena;
        this.state = RaceState.WAITING;
        this.players = new HashMap<>();
        this.playerOrder = new ArrayList<>();
        this.startTime = 0;
        this.endTime = 0;
        this.countdownTime = 0;
        this.winner = null;
        this.round = 1;
    }

    public boolean addPlayer(Player player) {
        if (players.size() >= arena.getMaxPlayers()) {
            return false;
        }

        if (state != RaceState.WAITING) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        if (players.containsKey(playerId)) {
            return false;
        }

        RacePlayer racePlayer = new RacePlayer(player, arena.getCheckpoints().size());
        players.put(playerId, racePlayer);
        playerOrder.add(playerId);

        return true;
    }

    public boolean removePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        if (!players.containsKey(playerId)) {
            return false;
        }

        players.remove(playerId);
        playerOrder.remove(playerId);

        // If race is running and not enough players, end race
        if (state == RaceState.RACING && players.size() < arena.getMinPlayers()) {
            state = RaceState.ENDING;
        }

        return true;
    }

    public boolean canStart() {
        return state == RaceState.WAITING && players.size() >= arena.getMinPlayers();
    }

    public void startCountdown(int seconds) {
        if (canStart()) {
            state = RaceState.COUNTDOWN;
            countdownTime = seconds;
        }
    }

    public void start() {
        if (state == RaceState.COUNTDOWN) {
            state = RaceState.RACING;
            startTime = System.currentTimeMillis();

            // Reset all players
            for (RacePlayer racePlayer : players.values()) {
                racePlayer.reset();
            }
        }
    }

    public void end() {
        state = RaceState.ENDING;
        endTime = System.currentTimeMillis();
    }

    public void finish() {
        state = RaceState.FINISHED;
    }

    public List<RacePlayer> getPlayersRanked() {
        List<RacePlayer> ranked = new ArrayList<>(players.values());
        ranked.sort((p1, p2) -> {
            // First by finish status
            if (p1.isFinished() && !p2.isFinished()) return -1;
            if (!p1.isFinished() && p2.isFinished()) return 1;

            // If both finished, by finish time
            if (p1.isFinished() && p2.isFinished()) {
                return Long.compare(p1.getFinishTime(), p2.getFinishTime());
            }

            // If neither finished, by checkpoints then by current time
            int checkpointDiff = p2.getCheckpointsReached() - p1.getCheckpointsReached();
            if (checkpointDiff != 0) return checkpointDiff;

            return Long.compare(p1.getCurrentTime(), p2.getCurrentTime());
        });

        return ranked;
    }

    public int getPlayerPosition(UUID playerId) {
        List<RacePlayer> ranked = getPlayersRanked();
        for (int i = 0; i < ranked.size(); i++) {
            if (ranked.get(i).getPlayer().getUniqueId().equals(playerId)) {
                return i + 1;
            }
        }
        return -1;
    }

    public long getRaceTime() {
        if (startTime == 0) return 0;
        long endTimeToUse = endTime > 0 ? endTime : System.currentTimeMillis();
        return endTimeToUse - startTime;
    }

    // Getters and setters
    public String getId() { return id; }
    public Arena getArena() { return arena; }
    public RaceState getState() { return state; }
    public void setState(RaceState state) { this.state = state; }
    public Map<UUID, RacePlayer> getPlayers() { return players; }
    public List<UUID> getPlayerOrder() { return playerOrder; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public int getCountdownTime() { return countdownTime; }
    public void setCountdownTime(int countdownTime) { this.countdownTime = countdownTime; }
    public UUID getWinner() { return winner; }
    public void setWinner(UUID winner) { this.winner = winner; }
    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }
}