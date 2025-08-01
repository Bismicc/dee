package de.somkat.iceboatracing.listeners;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import de.somkat.iceboatracing.race.Race;
import de.somkat.iceboatracing.race.RacePlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class RaceListener implements Listener {

    private final IceBoatRacingPlugin plugin;

    public RaceListener(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.COMPASS) return;

        Race race = plugin.getRaceManager().getRaceByPlayer(player);
        if (race == null || !race.getState().isActive()) return;

        RacePlayer racePlayer = race.getPlayers().get(player.getUniqueId());
        if (racePlayer == null) return;

        // Check if item is reset compass
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().contains("Reset")) {

            event.setCancelled(true);

            if (racePlayer.canReset()) {
                racePlayer.performReset();
                plugin.getMessageUtil().sendMessage(player, "race.manual-reset");
            } else {
                plugin.getMessageUtil().sendMessage(player, "race.reset-cooldown");
            }
        }
    }
}