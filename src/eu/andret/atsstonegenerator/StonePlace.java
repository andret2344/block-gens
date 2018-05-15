package eu.andret.atsstonegenerator;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class StonePlace implements Listener {
    private List<Block> list = atsStoneGenerator.getInstance().getBlocks();

    @EventHandler
    public void place(BlockPlaceEvent e) {
        if (e.getPlayer().getInventory().getItemInMainHand().getItemMeta() == null) {
            return;
        }
        if (e.getPlayer().getInventory().getItemInMainHand().getItemMeta().getDisplayName() == null) {
            return;
        }
        if (e.getPlayer().getInventory().getItemInMainHand().getItemMeta().getDisplayName().equals(atsStoneGenerator.getInstance().getStoneGenerator().getItemMeta().getDisplayName())) {
            list.add(e.getBlock());
            e.getBlock().getLocation().add(0, 1, 0).getBlock().setType(Material.STONE);
        }
    }

    @EventHandler
    public void destroy(BlockBreakEvent e) {
        Block brokenBlock = e.getBlock();
        for (Block block : list) {
            if (brokenBlock.getRelative(0, -1, 0).equals(block)) {
                int time = atsStoneGenerator.getInstance().getConfig().getInt("regen-delay");
                Bukkit.getScheduler().scheduleSyncDelayedTask(atsStoneGenerator.getInstance(), new Runnable() {
                    @Override
                    public synchronized void run() {
                        brokenBlock.setType(Material.STONE);
                    }
                }, time);
            } else if (brokenBlock.equals(block)) {
                list.remove(brokenBlock);
                e.setCancelled(true);
                brokenBlock.setType(Material.AIR);
                if (!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                    brokenBlock.getLocation().getWorld().dropItemNaturally(brokenBlock.getLocation(), new ItemStack(atsStoneGenerator.getInstance().getStoneGenerator()));
                }
                return;
            }
        }
    }
}