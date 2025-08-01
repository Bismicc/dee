package de.somkat.iceboatracing.listeners;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import de.somkat.iceboatracing.race.Race;
import de.somkat.iceboatracing.race.RacePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

public class BoatListener implements Listener {

    private final IceBoatRacingPlugin plugin;

    public BoatListener(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat)) return;
        if (event.getVehicle().getPassengers().isEmpty()) return;
        if (!(event.getVehicle().getPassengers().get(0) instanceof Player)) return;

        Boat boat = (Boat) event.getVehicle();
        Player player = (Player) boat.getPassengers().get(0);

        Race race = plugin.getRaceManager().getRaceByPlayer(player);
        if (race == null || !race.getState().isActive()) return;

        RacePlayer racePlayer = race.getPlayers().get(player.getUniqueId());
        if (racePlayer == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        // Check for speed modifications based on ground material
        Material groundMaterial = to.clone().subtract(0, 1, 0).getBlock().getType();
        applySpeedModification(boat, groundMaterial);

        // Check if player left the arena
        if (!race.getArena().isLocationInArena(to)) {
            resetPlayerToLastCheckpoint(player, racePlayer);
            return;
        }

        // Check checkpoint proximity
        checkCheckpoints(player, racePlayer, race, to);
    }

    private void applySpeedModification(Boat boat, Material groundMaterial) {
        Vector velocity = boat.getVelocity();
        double speedMultiplier = 1.0;

        switch (groundMaterial) {
            case ICE:
            case PACKED_ICE:
            case BLUE_ICE:
                speedMultiplier = plugin.getConfig().getDouble("boat.speed-multiplier", 1.5);
                break;
            case GOLD_BLOCK:
                // Boost pad
                speedMultiplier = plugin.getConfig().getDouble("boat.boost-multiplier", 2.5);
                break;
            case SLIME_BLOCK:
                // Brake pad
                speedMultiplier = plugin.getConfig().getDouble("boat.brake-multiplier", 0.3);
                break;
            default:
                speedMultiplier = 0.8; // Slower on other surfaces
                break;
        }

        if (speedMultiplier != 1.0) {
            velocity.multiply(speedMultiplier);
            boat.setVelocity(velocity);
        }
    }

    private void resetPlayerToLastCheckpoint(Player player, RacePlayer racePlayer) {
        if (!racePlayer.canReset()) {
            plugin.getMessageUtil().sendMessage(player, "race.reset-cooldown");
            return;
        }

        Location resetLocation = racePlayer.getLastCheckpoint();
        if (resetLocation == null) {
            // Reset to start line if no checkpoint reached
            Race race = plugin.getRaceManager().getRaceByPlayer(player);
            if (race != null && race.getArena().getStartLine() != null) {
                resetLocation = race.getArena().getStartLine();
            }
        }

        if (resetLocation != null) {
            player.teleport(resetLocation);
            racePlayer.performReset();
            plugin.getMessageUtil().sendMessage(player, "race.reset");
        }
    }

    private void checkCheckpoints(Player player, RacePlayer racePlayer, Race race, Location location) {
        int nextCheckpoint = racePlayer.getCheckpointsReached();

        if (nextCheckpoint >= race.getArena().getCheckpoints().size()) {
            // Check finish line
            if (isNearLocation(location, race.getArena().getFinishLine(), 3.0)) {
                if (!racePlayer.isFinished()) {
                    racePlayer.finish();
                    plugin.getMessageUtil().sendMessage(player, "race.finished-position",
                            "position", String.valueOf(race.getPlayerPosition(player.getUniqueId())));
                }
            }
            return;
        }

        Location checkpointLocation = race.getArena().getCheckpoints().get(nextCheckpoint);

        if (isNearLocation(location, checkpointLocation, 3.0)) {
            racePlayer.reachCheckpoint(nextCheckpoint, checkpointLocation);
            plugin.getMessageUtil().sendMessage(player, "race.checkpoint-reached",
                    "checkpoint", String.valueOf(nextCheckpoint + 1),
                    "total", String.valueOf(race.getArena().getCheckpoints().size()));
        }
    }

    private boolean isNearLocation(Location loc1, Location loc2, double distance) {
        if (!loc1.getWorld().equals(loc2.getWorld())) return false;
        return loc1.distance(loc2) <= distance;
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof Boat)) return;
        if (!(event.getExited() instanceof Player)) return;

        Player player = (Player) event.getExited();
        Race race = plugin.getRaceManager().getRaceByPlayer(player);

        if (race != null && race.getState().isActive()) {
            // Prevent players from exiting boats during races
            event.setCancelled(true);
            plugin.getMessageUtil().sendMessage(player, "race.cannot-exit-boat");
        }
    }
}