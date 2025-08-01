package de.somkat.iceboatracing.race;

import de.somkat.iceboatracing.powerups.PowerUp;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RacePlayer {

    private Player player;
    private Boat boat;
    private int checkpointsReached;
    private int totalCheckpoints;
    private long startTime;
    private long finishTime;
    private boolean finished;
    private Location lastCheckpoint;
    private List<PowerUp> activePowerUps;
    private long lastResetTime;
    private int position;

    public RacePlayer(Player player, int totalCheckpoints) {
        this.player = player;
        this.totalCheckpoints = totalCheckpoints;
        this.checkpointsReached = 0;
        this.finished = false;
        this.activePowerUps = new ArrayList<>();
        this.lastResetTime = 0;
        this.position = 0;
    }

    public void reset() {
        checkpointsReached = 0;
        finished = false;
        finishTime = 0;
        activePowerUps.clear();
        startTime = System.currentTimeMillis();
        lastResetTime = System.currentTimeMillis();
    }

    public void reachCheckpoint(int checkpointIndex, Location checkpointLocation) {
        if (checkpointIndex == checkpointsReached) {
            checkpointsReached++;
            lastCheckpoint = checkpointLocation.clone();
        }
    }

    public void finish() {
        if (!finished) {
            finished = true;
            finishTime = System.currentTimeMillis();
        }
    }

    public long getCurrentTime() {
        if (startTime == 0) return 0;
        long endTime = finished ? finishTime : System.currentTimeMillis();
        return endTime - startTime;
    }

    public String getFormattedTime() {
        long time = getCurrentTime();
        long minutes = time / 60000;
        long seconds = (time % 60000) / 1000;
        long millis = (time % 1000) / 10;
        return String.format("%02d:%02d.%02d", minutes, seconds, millis);
    }

    public void addPowerUp(PowerUp powerUp) {
        activePowerUps.add(powerUp);
    }

    public void removePowerUp(PowerUp powerUp) {
        activePowerUps.remove(powerUp);
    }

    public boolean canReset() {
        return (System.currentTimeMillis() - lastResetTime) >= 3000; // 3 second cooldown
    }

    public void performReset() {
        if (canReset() && lastCheckpoint != null) {
            player.teleport(lastCheckpoint);
            lastResetTime = System.currentTimeMillis();
        }
    }

    // Getters and setters
    public Player getPlayer() {
        return player;
    }

    public Boat getBoat() {
        return boat;
    }

    public void setBoat(Boat boat) {
        this.boat = boat;
    }

    public int getCheckpointsReached() {
        return checkpointsReached;
    }

    public void setCheckpointsReached(int checkpointsReached) {
        this.checkpointsReached = checkpointsReached;
    }

    public int getTotalCheckpoints() {
        return totalCheckpoints;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public boolean isFinished() {
        return finished;
    }

    public Location getLastCheckpoint() {
        return lastCheckpoint;
    }

    public void setLastCheckpoint(Location lastCheckpoint) {
        this.lastCheckpoint = lastCheckpoint;
    }

    public List<PowerUp> getActivePowerUps() {
        return activePowerUps;
    }

    public long getLastResetTime() {
        return lastResetTime;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}