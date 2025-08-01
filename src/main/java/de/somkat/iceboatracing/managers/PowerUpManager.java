package de.somkat.iceboatracing.managers;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import de.somkat.iceboatracing.arena.PowerUpSpawn;
import de.somkat.iceboatracing.powerups.PowerUp;
import de.somkat.iceboatracing.powerups.PowerUpType;
import de.somkat.iceboatracing.race.Race;
import de.somkat.iceboatracing.race.RacePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PowerUpManager {

    private final IceBoatRacingPlugin plugin;
    private final Map<String, Set<ArmorStand>> racePowerUps; // Race ID -> PowerUp ArmorStands
    private final Map<UUID, List<PowerUp>> playerPowerUps; // Player UUID -> Active PowerUps

    public PowerUpManager(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.racePowerUps = new ConcurrentHashMap<>();
        this.playerPowerUps = new ConcurrentHashMap<>();

        startPowerUpTask();
    }

    private void startPowerUpTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updatePowerUps();
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second
    }

    public void spawnPowerUpsForRace(Race race) {
        if (!plugin.getConfig().getBoolean("powerups.enabled", true)) {
            return;
        }

        String raceId = race.getId();
        Set<ArmorStand> powerUpStands = new HashSet<>();

        for (PowerUpSpawn spawn : race.getArena().getPowerUpSpawns()) {
            if (spawn.canSpawn(System.currentTimeMillis(),
                    plugin.getConfig().getLong("powerups.respawn-time", 30) * 1000)) {

                ArmorStand stand = spawnPowerUpStand(spawn.getLocation(), spawn.getType());
                if (stand != null) {
                    powerUpStands.add(stand);
                    spawn.setSpawned(System.currentTimeMillis());
                }
            }
        }

        racePowerUps.put(raceId, powerUpStands);
    }

    private ArmorStand spawnPowerUpStand(Location location, PowerUpType type) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);

        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.setCustomName("§e" + type.getDisplayName());
        stand.setCustomNameVisible(true);
        stand.setSmall(true);

        // Create floating item
        ItemStack item = new ItemStack(type.getDisplayMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + type.getDisplayName());
        item.setItemMeta(meta);

        Item floatingItem = location.getWorld().dropItem(location.add(0, 1, 0), item);
        floatingItem.setVelocity(new Vector(0, 0, 0));
        floatingItem.setPickupDelay(Integer.MAX_VALUE);

        // Add floating effect
        new BukkitRunnable() {
            double y = 0;

            @Override
            public void run() {
                if (stand.isDead() || floatingItem.isDead()) {
                    cancel();
                    return;
                }

                y += 0.1;
                Location newLoc = stand.getLocation().clone().add(0, Math.sin(y) * 0.3, 0);
                floatingItem.teleport(newLoc);

                // Spawn particles
                stand.getWorld().spawnParticle(type.getParticle(), newLoc, 3, 0.2, 0.2, 0.2, 0.02);
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return stand;
    }

    public boolean collectPowerUp(Player player, ArmorStand powerUpStand) {
        Race race = plugin.getRaceManager().getRaceByPlayer(player);
        if (race == null) return false;

        RacePlayer racePlayer = race.getPlayers().get(player.getUniqueId());
        if (racePlayer == null) return false;

        // Determine power-up type from stand name
        String name = powerUpStand.getCustomName();
        PowerUpType type = null;

        for (PowerUpType powerUpType : PowerUpType.values()) {
            if (name.contains(powerUpType.getDisplayName())) {
                type = powerUpType;
                break;
            }
        }

        if (type == null) return false;

        // Create and activate power-up
        PowerUp powerUp = new PowerUp(type, racePlayer);
        powerUp.activate();

        // Add to player's active power-ups
        playerPowerUps.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(powerUp);
        racePlayer.addPowerUp(powerUp);

        // Remove the power-up stand
        powerUpStand.remove();

        // Remove from race power-ups
        Set<ArmorStand> stands = racePowerUps.get(race.getId());
        if (stands != null) {
            stands.remove(powerUpStand);
        }

        plugin.getMessageUtil().sendMessage(player, "powerup.collected", "type", type.getDisplayName());

        return true;
    }

    private void updatePowerUps() {
        // Update active power-ups
        for (Map.Entry<UUID, List<PowerUp>> entry : playerPowerUps.entrySet()) {
            List<PowerUp> powerUps = entry.getValue();
            Iterator<PowerUp> iterator = powerUps.iterator();

            while (iterator.hasNext()) {
                PowerUp powerUp = iterator.next();

                if (powerUp.isExpired()) {
                    powerUp.deactivate();
                    powerUp.getOwner().removePowerUp(powerUp);
                    iterator.remove();
                }
            }

            if (powerUps.isEmpty()) {
                playerPowerUps.remove(entry.getKey());
            }
        }

        // Respawn power-ups in active races
        for (Race race : plugin.getRaceManager().getActiveRaces()) {
            if (race.getState().isActive()) {
                respawnPowerUps(race);
            }
        }
    }

    private void respawnPowerUps(Race race) {
        String raceId = race.getId();
        Set<ArmorStand> currentStands = racePowerUps.computeIfAbsent(raceId, k -> new HashSet<>());

        // Remove dead stands
        currentStands.removeIf(ArmorStand::isDead);

        // Check if we need to spawn more power-ups
        long respawnTime = plugin.getConfig().getLong("powerups.respawn-time", 30) * 1000;

        for (PowerUpSpawn spawn : race.getArena().getPowerUpSpawns()) {
            if (spawn.isActive() && spawn.canSpawn(System.currentTimeMillis(), respawnTime)) {
                // Check if there's already a power-up at this location
                boolean hasStandNearby = currentStands.stream()
                        .anyMatch(stand -> stand.getLocation().distance(spawn.getLocation()) < 2.0);

                if (!hasStandNearby) {
                    ArmorStand stand = spawnPowerUpStand(spawn.getLocation(), spawn.getType());
                    if (stand != null) {
                        currentStands.add(stand);
                        spawn.setSpawned(System.currentTimeMillis());
                    }
                }
            }
        }
    }

    public void clearRacePowerUps(Race race) {
        String raceId = race.getId();
        Set<ArmorStand> stands = racePowerUps.remove(raceId);

        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (!stand.isDead()) {
                    stand.remove();
                }
            }
        }

        // Clear player power-ups for this race
        for (RacePlayer racePlayer : race.getPlayers().values()) {
            UUID playerId = racePlayer.getPlayer().getUniqueId();
            List<PowerUp> powerUps = playerPowerUps.get(playerId);

            if (powerUps != null) {
                for (PowerUp powerUp : powerUps) {
                    powerUp.deactivate();
                }
                powerUps.clear();
                playerPowerUps.remove(playerId);
            }
        }
    }

    public List<PowerUp> getPlayerPowerUps(Player player) {
        return playerPowerUps.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    public boolean hasActivePowerUp(Player player, PowerUpType type) {
        List<PowerUp> powerUps = getPlayerPowerUps(player);
        return powerUps.stream().anyMatch(p -> p.getType() == type && p.isActive());
    }
}