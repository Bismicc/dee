package de.somkat.iceboatracing.managers;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import de.somkat.iceboatracing.race.Race;
import de.somkat.iceboatracing.race.RacePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class LeaderboardManager {

    private final IceBoatRacingPlugin plugin;

    public LeaderboardManager(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    public void saveRaceResults(Race race) {
        List<RacePlayer> ranked = race.getPlayersRanked();

        for (int i = 0; i < ranked.size(); i++) {
            RacePlayer racePlayer = ranked.get(i);
            int position = i + 1;

            savePlayerStats(racePlayer, race, position);
        }
    }

    private void savePlayerStats(RacePlayer racePlayer, Race race, int position) {
        String sql = "INSERT INTO player_race_stats (player_uuid, player_name, arena_name, position, " +
                "finish_time, checkpoints_reached, race_date) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, racePlayer.getPlayer().getUniqueId().toString());
            stmt.setString(2, racePlayer.getPlayer().getName());
            stmt.setString(3, race.getArena().getName());
            stmt.setInt(4, position);
            stmt.setLong(5, racePlayer.getCurrentTime());
            stmt.setInt(6, racePlayer.getCheckpointsReached());
            stmt.setLong(7, System.currentTimeMillis());

            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save race stats: " + e.getMessage());
        }

        // Update overall player stats
        updatePlayerOverallStats(racePlayer, position, race.getArena().getName());
    }

    private void updatePlayerOverallStats(RacePlayer racePlayer, int position, String arenaName) {
        String selectSql = "SELECT * FROM player_stats WHERE player_uuid = ? AND arena_name = ?";
        String insertSql = "INSERT INTO player_stats (player_uuid, player_name, arena_name, races_played, " +
                "wins, best_time, total_time, average_position) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String updateSql = "UPDATE player_stats SET player_name = ?, races_played = ?, wins = ?, " +
                "best_time = ?, total_time = ?, average_position = ? WHERE player_uuid = ? AND arena_name = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {

            // Check if stats exist
            boolean exists = false;
            int racesPlayed = 0, wins = 0;
            long bestTime = 0, totalTime = 0;
            double avgPosition = 0;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, racePlayer.getPlayer().getUniqueId().toString());
                selectStmt.setString(2, arenaName);

                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    exists = true;
                    racesPlayed = rs.getInt("races_played");
                    wins = rs.getInt("wins");
                    bestTime = rs.getLong("best_time");
                    totalTime = rs.getLong("total_time");
                    avgPosition = rs.getDouble("average_position");
                }
            }

            // Update stats
            racesPlayed++;
            if (position == 1) wins++;

            long currentTime = racePlayer.getCurrentTime();
            if (racePlayer.isFinished() && (bestTime == 0 || currentTime < bestTime)) {
                bestTime = currentTime;
            }

            totalTime += currentTime;
            avgPosition = ((avgPosition * (racesPlayed - 1)) + position) / racesPlayed;

            // Insert or update
            if (exists) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, racePlayer.getPlayer().getName());
                    updateStmt.setInt(2, racesPlayed);
                    updateStmt.setInt(3, wins);
                    updateStmt.setLong(4, bestTime);
                    updateStmt.setLong(5, totalTime);
                    updateStmt.setDouble(6, avgPosition);
                    updateStmt.setString(7, racePlayer.getPlayer().getUniqueId().toString());
                    updateStmt.setString(8, arenaName);

                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, racePlayer.getPlayer().getUniqueId().toString());
                    insertStmt.setString(2, racePlayer.getPlayer().getName());
                    insertStmt.setString(3, arenaName);
                    insertStmt.setInt(4, racesPlayed);
                    insertStmt.setInt(5, wins);
                    insertStmt.setLong(6, bestTime);
                    insertStmt.setLong(7, totalTime);
                    insertStmt.setDouble(8, avgPosition);

                    insertStmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update player stats: " + e.getMessage());
        }
    }

    public List<LeaderboardEntry> getTopPlayers(String arenaName, String sortBy, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        String orderBy;
        switch (sortBy.toLowerCase()) {
            case "wins":
                orderBy = "wins DESC, best_time ASC";
                break;
            case "time":
            case "best_time":
                orderBy = "best_time ASC";
                break;
            case "races":
                orderBy = "races_played DESC, wins DESC";
                break;
            default:
                orderBy = "wins DESC, best_time ASC";
        }

        String sql = "SELECT * FROM player_stats WHERE arena_name = ? AND best_time > 0 ORDER BY " + orderBy + " LIMIT ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, arenaName);
            stmt.setInt(2, limit);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                entries.add(new LeaderboardEntry(
                        rs.getString("player_name"),
                        rs.getInt("races_played"),
                        rs.getInt("wins"),
                        rs.getLong("best_time"),
                        rs.getDouble("average_position")
                ));
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get leaderboard: " + e.getMessage());
        }

        return entries;
    }

    public PlayerStats getPlayerStats(UUID playerId, String arenaName) {
        String sql = "SELECT * FROM player_stats WHERE player_uuid = ? AND arena_name = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerId.toString());
            stmt.setString(2, arenaName);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new PlayerStats(
                        rs.getString("player_name"),
                        rs.getString("arena_name"),
                        rs.getInt("races_played"),
                        rs.getInt("wins"),
                        rs.getLong("best_time"),
                        rs.getLong("total_time"),
                        rs.getDouble("average_position")
                );
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player stats: " + e.getMessage());
        }

        return null;
    }

    public List<RecentRace> getRecentRaces(String arenaName, int limit) {
        List<RecentRace> races = new ArrayList<>();

        String sql = "SELECT * FROM player_race_stats WHERE arena_name = ? ORDER BY race_date DESC LIMIT ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, arenaName);
            stmt.setInt(2, limit);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                races.add(new RecentRace(
                        rs.getString("player_name"),
                        rs.getInt("position"),
                        rs.getLong("finish_time"),
                        rs.getInt("checkpoints_reached"),
                        rs.getLong("race_date")
                ));
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get recent races: " + e.getMessage());
        }

        return races;
    }

    // Data classes
    public static class LeaderboardEntry {
        public final String playerName;
        public final int racesPlayed;
        public final int wins;
        public final long bestTime;
        public final double averagePosition;

        public LeaderboardEntry(String playerName, int racesPlayed, int wins, long bestTime, double averagePosition) {
            this.playerName = playerName;
            this.racesPlayed = racesPlayed;
            this.wins = wins;
            this.bestTime = bestTime;
            this.averagePosition = averagePosition;
        }

        public String getFormattedTime() {
            long minutes = bestTime / 60000;
            long seconds = (bestTime % 60000) / 1000;
            long millis = (bestTime % 1000) / 10;
            return String.format("%02d:%02d.%02d", minutes, seconds, millis);
        }
    }

    public static class PlayerStats {
        public final String playerName;
        public final String arenaName;
        public final int racesPlayed;
        public final int wins;
        public final long bestTime;
        public final long totalTime;
        public final double averagePosition;

        public PlayerStats(String playerName, String arenaName, int racesPlayed, int wins,
                           long bestTime, long totalTime, double averagePosition) {
            this.playerName = playerName;
            this.arenaName = arenaName;
            this.racesPlayed = racesPlayed;
            this.wins = wins;
            this.bestTime = bestTime;
            this.totalTime = totalTime;
            this.averagePosition = averagePosition;
        }
    }

    public static class RecentRace {
        public final String playerName;
        public final int position;
        public final long finishTime;
        public final int checkpointsReached;
        public final long raceDate;

        public RecentRace(String playerName, int position, long finishTime, int checkpointsReached, long raceDate) {
            this.playerName = playerName;
            this.position = position;
            this.finishTime = finishTime;
            this.checkpointsReached = checkpointsReached;
            this.raceDate = raceDate;
        }
    }
}