package de.somkat.iceboatracing.listeners;

import de.somkat.iceboatracing.IceBoatRacingPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SetupListener implements Listener {

    private final IceBoatRacingPlugin plugin;

    public SetupListener(IceBoatRacingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.STICK) return;
        if (!player.hasPermission("iceboatracing.setup")) return;

        // Check if item is setup wand
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().contains("Setup Wand")) {

            event.setCancelled(true);

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                // Handle setup actions - this would be expanded for full setup functionality
                plugin.getMessageUtil().sendMessage(player, "setup.left-click",
                        "x", String.valueOf(event.getClickedBlock().getX()),
                        "y", String.valueOf(event.getClickedBlock().getY()),
                        "z", String.valueOf(event.getClickedBlock().getZ()));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                plugin.getMessageUtil().sendMessage(player, "setup.right-click",
                        "x", String.valueOf(event.getClickedBlock().getX()),
                        "y", String.valueOf(event.getClickedBlock().getY()),
                        "z", String.valueOf(event.getClickedBlock().getZ()));
            }
        }
    }
}