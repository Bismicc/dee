package de.somkat.iceboatracing.listeners;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import de.somkat.iceboatracing.race.Race;
import de.somkat.iceboatracing.race.RacePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {

    private final IceBoatRacingPlugin plugin;

    public PlayerListener(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove player from race if they're in one
        if (plugin.getRaceManager().isPlayerInRace(player)) {
            plugin.getRaceManager().leaveRace(player);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Race race = plugin.getRaceManager().getRaceByPlayer(player);

        if (race != null && race.getState().isActive()) {
            // Prevent teleportation during active races (except for reset)
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
                event.setCancelled(true);
                plugin.getMessageUtil().sendMessage(player, "race.no-teleport");
            }
        }
    }
}