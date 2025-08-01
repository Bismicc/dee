package de.somkat.iceboatracing.managers;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import de.somkat.iceboatracing.arena.*;
import de.somkat.iceboatracing.powerups.PowerUpType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ArenaManager {

    private final IceBoatRacingPlugin plugin;
    private final Map<String, Arena> arenas;

    public ArenaManager(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
    }

    public void loadArenas() {
        FileConfiguration config = plugin.getConfigManager().getArenas();
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");

        if (arenasSection == null) {
            plugin.getLogger().info("No arenas found in configuration.");
            return;
        }

        for (String arenaName : arenasSection.getKeys(false)) {
            try {
                Arena arena = loadArena(arenasSection.getConfigurationSection(arenaName), arenaName);
                if (arena != null) {
                    arenas.put(arenaName, arena);
                    plugin.getLogger().info("Loaded arena: " + arenaName);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load arena " + arenaName + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + arenas.size() + " arenas.");
    }

    private Arena loadArena(ConfigurationSection section, String name) {
        if (section == null) return null;

        String worldName = section.getString("world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("World " + worldName + " not found for arena " + name);
            return null;
        }

        // Load spawn points
        List<Location> spawnPoints = loadLocations(section.getConfigurationSection("spawnpoints"), world);

        // Load checkpoints
        List<Location> checkpoints = loadLocations(section.getConfigurationSection("checkpoints"), world);

        // Load start and finish lines
        Location startLine = loadLocation(section.getConfigurationSection("start_line"), world);
        Location finishLine = loadLocation(section.getConfigurationSection("finish_line"), world);

        // Load settings
        int maxPlayers = section.getInt("max_players", 8);
        int minPlayers = section.getInt("min_players", 2);
        String boatModel = section.getString("boat_model", "default");

        // Load power-up spawns
        List<PowerUpSpawn> powerUpSpawns = loadPowerUpSpawns(section.getConfigurationSection("powerups"), world);

        // Load region
        ArenaRegion region = loadRegion(section.getConfigurationSection("region"), world);

        Arena arena = new Arena(name, worldName, spawnPoints, checkpoints, startLine, finishLine,
                maxPlayers, minPlayers, powerUpSpawns, boatModel, region);
        arena.setWorld(world);

        return arena;
    }

    private List<Location> loadLocations(ConfigurationSection section, World world) {
        List<Location> locations = new ArrayList<>();
        if (section == null) return locations;

        for (String key : section.getKeys(false)) {
            ConfigurationSection locationSection = section.getConfigurationSection(key);
            if (locationSection != null) {
                Location location = loadLocation(locationSection, world);
                if (location != null) {
                    locations.add(location);
                }
            }
        }

        return locations;
    }

    private Location loadLocation(ConfigurationSection section, World world) {
        if (section == null) return null;

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private List<PowerUpSpawn> loadPowerUpSpawns(ConfigurationSection section, World world) {
        List<PowerUpSpawn> spawns = new ArrayList<>();
        if (section == null) return spawns;

        ConfigurationSection spawnPointsSection = section.getConfigurationSection("spawn_points");
        if (spawnPointsSection != null) {
            for (String key : spawnPointsSection.getKeys(false)) {
                ConfigurationSection spawnSection = spawnPointsSection.getConfigurationSection(key);
                if (spawnSection != null) {
                    Location location = loadLocation(spawnSection, world);
                    String typeStr = spawnSection.getString("type", "BOOST");
                    int weight = spawnSection.getInt("weight", 100);

                    try {
                        PowerUpType type = PowerUpType.valueOf(typeStr.toUpperCase());
                        spawns.add(new PowerUpSpawn(location, type, weight));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid power-up type: " + typeStr);
                    }
                }
            }
        }

        return spawns;
    }

    private ArenaRegion loadRegion(ConfigurationSection section, World world) {
        if (section == null) return null;

        double minX = section.getDouble("min.x");
        double minY = section.getDouble("min.y");
        double minZ = section.getDouble("min.z");
        double maxX = section.getDouble("max.x");
        double maxY = section.getDouble("max.y");
        double maxZ = section.getDouble("max.z");

        return new ArenaRegion(minX, minY, minZ, maxX, maxY, maxZ, world.getName());
    }

    public void saveArena(Arena arena) {
        FileConfiguration config = plugin.getConfigManager().getArenas();
        String path = "arenas." + arena.getName();

        config.set(path + ".world", arena.getWorldName());
        config.set(path + ".max_players", arena.getMaxPlayers());
        config.set(path + ".min_players", arena.getMinPlayers());
        config.set(path + ".boat_model", arena.getBoatModel());

        // Save spawn points
        saveLocations(config, path + ".spawnpoints", arena.getSpawnPoints());

        // Save checkpoints
        saveLocations(config, path + ".checkpoints", arena.getCheckpoints());

        // Save start/finish lines
        if (arena.getStartLine() != null) {
            saveLocation(config, path + ".start_line", arena.getStartLine());
        }
        if (arena.getFinishLine() != null) {
            saveLocation(config, path + ".finish_line", arena.getFinishLine());
        }

        // Save power-up spawns
        savePowerUpSpawns(config, path + ".powerups.spawn_points", arena.getPowerUpSpawns());

        // Save region
        if (arena.getRegion() != null) {
            saveRegion(config, path + ".region", arena.getRegion());
        }

        plugin.getConfigManager().saveArenas();
        arenas.put(arena.getName(), arena);
    }

    private void saveLocations(FileConfiguration config, String path, List<Location> locations) {
        config.set(path, null); // Clear existing
        for (int i = 0; i < locations.size(); i++) {
            saveLocation(config, path + "." + i, locations.get(i));
        }
    }

    private void saveLocation(FileConfiguration config, String path, Location location) {
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    private void savePowerUpSpawns(FileConfiguration config, String path, List<PowerUpSpawn> spawns) {
        config.set(path, null); // Clear existing
        for (int i = 0; i < spawns.size(); i++) {
            PowerUpSpawn spawn = spawns.get(i);
            String spawnPath = path + "." + i;
            saveLocation(config, spawnPath, spawn.getLocation());
            config.set(spawnPath + ".type", spawn.getType().name());
            config.set(spawnPath + ".weight", spawn.getWeight());
        }
    }

    private void saveRegion(FileConfiguration config, String path, ArenaRegion region) {
        config.set(path + ".min.x", region.getMinX());
        config.set(path + ".min.y", region.getMinY());
        config.set(path + ".min.z", region.getMinZ());
        config.set(path + ".max.x", region.getMaxX());
        config.set(path + ".max.y", region.getMaxY());
        config.set(path + ".max.z", region.getMaxZ());
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public List<String> getArenaNames() {
        return new ArrayList<>(arenas.keySet());
    }

    public Arena getArenaByLocation(Location location) {
        for (Arena arena : arenas.values()) {
            if (arena.isLocationInArena(location)) {
                return arena;
            }
        }
        return null;
    }

    public void deleteArena(String name) {
        arenas.remove(name);
        FileConfiguration config = plugin.getConfigManager().getArenas();
        config.set("arenas." + name, null);
        plugin.getConfigManager().saveArenas();
    }
}