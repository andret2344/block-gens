package eu.andret.blockgenerator;

import eu.andret.blockgenerator.entity.Generator;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

@AllArgsConstructor
public class BlockGeneratorListener implements Listener {
	private final BlockGenerator plugin;

	@EventHandler
	public void place(BlockPlaceEvent e) {
		plugin.getGenerators()
				.stream()
				.filter(g -> g.isGenerator(e.getItemInHand()))
				.findFirst()
				.ifPresent(g -> g.add(e.getBlock()));
	}

	@EventHandler
	public void destroy(BlockBreakEvent e) {
		Block brokenBlock = e.getBlock();
		for (Generator generator : plugin.getGenerators()) {
			for (Block block : generator.getPlacedBlocks()) {
				if (brokenBlock.getRelative(0, -1, 0).equals(block)) {
					Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
						synchronized (brokenBlock) {
							brokenBlock.setType(generator.getGeneratedItem().getType());
						}
					}, generator.getRegenDelay());
				}
				if (brokenBlock.equals(block)) {
					generator.remove(brokenBlock);
					e.setCancelled(true);
					brokenBlock.setType(Material.AIR);
					if (!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
						brokenBlock.getLocation().getWorld().dropItemNaturally(brokenBlock.getLocation(), generator.getGeneratorItem());
					}
					return;
				}
			}
		}
	}
}