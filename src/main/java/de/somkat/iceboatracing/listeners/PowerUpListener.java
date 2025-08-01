package de.somkat.iceboatracing.listeners;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import de.somkat.iceboatracing.race.Race;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class PowerUpListener implements Listener {

    private final IceBoatRacingPlugin plugin;

    public PowerUpListener(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand)) return;

        Player player = event.getPlayer();
        ArmorStand armorStand = (ArmorStand) event.getRightClicked();

        // Check if player is in a race
        Race race = plugin.getRaceManager().getRaceByPlayer(player);
        if (race == null || !race.getState().isActive()) return;

        // Check if this is a power-up stand
        if (armorStand.getCustomName() == null || !armorStand.getCustomName().contains("§e")) return;

        // Try to collect the power-up
        if (plugin.getPowerUpManager().collectPowerUp(player, armorStand)) {
            event.setCancelled(true);
        }
    }
}