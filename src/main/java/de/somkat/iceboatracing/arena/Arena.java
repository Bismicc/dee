package de.somkat.iceboatracing.arena;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Map;

public class Arena {

    private String name;
    private String worldName;
    private World world;
    private List<Location> spawnPoints;
    private List<Location> checkpoints;
    private Location startLine;
    private Location finishLine;
    private int maxPlayers;
    private int minPlayers;
    private List<PowerUpSpawn> powerUpSpawns;
    private String boatModel;
    private ArenaRegion region;

    public Arena(String name, String worldName) {
        this.name = name;
        this.worldName = worldName;
    }

    // Constructor with all parameters
    public Arena(String name, String worldName, List<Location> spawnPoints,
                 List<Location> checkpoints, Location startLine, Location finishLine,
                 int maxPlayers, int minPlayers, List<PowerUpSpawn> powerUpSpawns,
                 String boatModel, ArenaRegion region) {
        this.name = name;
        this.worldName = worldName;
        this.spawnPoints = spawnPoints;
        this.checkpoints = checkpoints;
        this.startLine = startLine;
        this.finishLine = finishLine;
        this.maxPlayers = maxPlayers;
        this.minPlayers = minPlayers;
        this.powerUpSpawns = powerUpSpawns;
        this.boatModel = boatModel;
        this.region = region;
    }

    public boolean isLocationInArena(Location location) {
        if (region == null || !location.getWorld().equals(world)) {
            return false;
        }
        return region.contains(location);
    }

    public boolean isValidForRace() {
        return world != null && spawnPoints != null && !spawnPoints.isEmpty()
                && checkpoints != null && !checkpoints.isEmpty()
                && startLine != null && finishLine != null
                && minPlayers > 0 && maxPlayers >= minPlayers;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public List<Location> getSpawnPoints() {
        return spawnPoints;
    }

    public void setSpawnPoints(List<Location> spawnPoints) {
        this.spawnPoints = spawnPoints;
    }

    public List<Location> getCheckpoints() {
        return checkpoints;
    }

    public void setCheckpoints(List<Location> checkpoints) {
        this.checkpoints = checkpoints;
    }

    public Location getStartLine() {
        return startLine;
    }

    public void setStartLine(Location startLine) {
        this.startLine = startLine;
    }

    public Location getFinishLine() {
        return finishLine;
    }

    public void setFinishLine(Location finishLine) {
        this.finishLine = finishLine;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public List<PowerUpSpawn> getPowerUpSpawns() {
        return powerUpSpawns;
    }

    public void setPowerUpSpawns(List<PowerUpSpawn> powerUpSpawns) {
        this.powerUpSpawns = powerUpSpawns;
    }

    public String getBoatModel() {
        return boatModel;
    }

    public void setBoatModel(String boatModel) {
        this.boatModel = boatModel;
    }

    public ArenaRegion getRegion() {
        return region;
    }

    public void setRegion(ArenaRegion region) {
        this.region = region;
    }
}