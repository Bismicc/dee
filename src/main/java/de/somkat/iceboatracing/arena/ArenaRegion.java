package de.somkat.iceboatracing.arena;

import org.bukkit.Location;

public class ArenaRegion {

    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private String worldName;

    public ArenaRegion(double minX, double minY, double minZ,
                       double maxX, double maxY, double maxZ, String worldName) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.worldName = worldName;
    }

    public ArenaRegion(Location pos1, Location pos2) {
        this(pos1.getX(), pos1.getY(), pos1.getZ(),
                pos2.getX(), pos2.getY(), pos2.getZ(),
                pos1.getWorld().getName());
    }

    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    public Location getCenter() {
        return new Location(
                org.bukkit.Bukkit.getWorld(worldName),
                (minX + maxX) / 2,
                (minY + maxY) / 2,
                (minZ + maxZ) / 2
        );
    }

    // Getters
    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    public double getMaxZ() { return maxZ; }
    public String getWorldName() { return worldName; }

    @Override
    public String toString() {
        return String.format("Region[%s: (%.1f,%.1f,%.1f) to (%.1f,%.1f,%.1f)]",
                worldName, minX, minY, minZ, maxX, maxY, maxZ);
    }
}