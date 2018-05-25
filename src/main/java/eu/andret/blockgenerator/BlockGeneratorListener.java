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
    }

    @EventHandler
    public void place(BlockPlaceEvent e) {
        for (BlockGenerator generator : plugin.getGenerators()) {
            if (generator
                    .getGenerator()
                    .getItemMeta()
                    .equals(e
                            .getItemInHand().getItemMeta())) {
                generator.add(e.getBlock());
                e.getBlockPlaced().getRelative(0, 1, 0).setType(generator.getGenerated().getType());
                return;
            }
        }
    }

    @EventHandler
    public void destroy(BlockBreakEvent e) {
        Block brokenBlock = e.getBlock();
        for (BlockGenerator generator : plugin.getGenerators()) {
            for (Block block : generator.getPlaced()) {
                if (brokenBlock.getRelative(0, -1, 0).equals(block)) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        synchronized (brokenBlock) {
                            brokenBlock.setType(generator.getGenerated().getType());
                        }
                    }, generator.getRegenDelay());
                }
                if (brokenBlock.equals(block)) {
                    generator.remove(brokenBlock);
                    e.setCancelled(true);
                    brokenBlock.setType(Material.AIR);
                    if (!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                        brokenBlock.getLocation().getWorld().dropItemNaturally(brokenBlock.getLocation(), generator.getGenerator());
                    }
                    return;
                }
            }
        }
    }
}