package eu.andret.blockgenerator;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockGeneratorListener implements Listener {
    private final atsBlockGenerator plugin;

    public BlockGeneratorListener(atsBlockGenerator plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void place(BlockPlaceEvent e) {
        if (plugin.getGenerator().equals(e.getItemInHand())) {
            plugin.getGenerators().add(e.getBlock());
            e.getBlockPlaced().getRelative(0, 1, 0).setType(plugin.getGenerated());
        }
    }

    @EventHandler
    public void destroy(BlockBreakEvent e) {
        Block brokenBlock = e.getBlock();
        for (Block generator : plugin.getGenerators()) {
            if (brokenBlock.getRelative(0, -1, 0).equals(generator)) {
                int time = plugin.getConfig().getInt("regen-delay");
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    synchronized (brokenBlock) {
                        brokenBlock.setType(plugin.getGenerated());
                    }
                }, time);
            } else if (brokenBlock.equals(generator)) {
                plugin.getGenerators().remove(brokenBlock);
                e.setCancelled(true);
                brokenBlock.setType(Material.AIR);
                if (!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                    brokenBlock.getLocation().getWorld().dropItemNaturally(brokenBlock.getLocation(), plugin.getGenerator());
                }
                return;
            }
        }
    }
}