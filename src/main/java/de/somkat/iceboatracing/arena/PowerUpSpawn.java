package de.somkat.iceboatracing.arena;

import de.somkat.iceboatracing.powerups.PowerUpType;
import org.bukkit.Location;

public class PowerUpSpawn {

    private Location location;
    private PowerUpType type;
    private int weight; // Spawn probability weight
    private boolean active;
    private long lastSpawnTime;

    public PowerUpSpawn(Location location, PowerUpType type, int weight) {
        this.location = location;
        this.type = type;
        this.weight = weight;
        this.active = true;
        this.lastSpawnTime = 0;
    }

    public PowerUpSpawn(Location location, PowerUpType type) {
        this(location, type, 100);
    }

    public boolean canSpawn(long currentTime, long respawnCooldown) {
        return active && (currentTime - lastSpawnTime) >= respawnCooldown;
    }

    public void setSpawned(long time) {
        this.lastSpawnTime = time;
    }

    // Getters and setters
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public PowerUpType getType() {
        return type;
    }

    public void setType(PowerUpType type) {
        this.type = type;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getLastSpawnTime() {
        return lastSpawnTime;
    }

    public void setLastSpawnTime(long lastSpawnTime) {
        this.lastSpawnTime = lastSpawnTime;
    }
}