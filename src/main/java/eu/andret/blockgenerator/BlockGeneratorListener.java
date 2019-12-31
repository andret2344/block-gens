package eu.andret.blockgenerator;

import eu.andret.blockgenerator.entity.Generator;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Arrays;

@AllArgsConstructor
public class BlockGeneratorListener implements Listener {
	private final BlockGenerator plugin;

	@EventHandler
	public void place(BlockPlaceEvent e) {
		plugin.getPatterns()
				.stream()
				.filter(p -> p.matchGeneratorItem(e.getItemInHand()))
				.findFirst()
				.map(p -> new Generator(p, e.getBlock()))
				.ifPresent(g -> {
					e.getBlockPlaced().getRelative(0, 1, 0).setType(g.getPattern().getGeneratedItem().getType());
					plugin.getGenerators().add(g);
				});
	}

	@EventHandler
	public void destroy(BlockBreakEvent e) {
		Block brokenBlock = e.getBlock();
		plugin.getGenerators().stream()
				.filter(generator -> generator.getBlock().getRelative(0, 1, 0).equals(brokenBlock))
				.findFirst()
				.map(Generator::getPattern)
				.ifPresent(pattern -> {
					e.setCancelled(true);
					if (Arrays.asList(GameMode.SURVIVAL, GameMode.ADVENTURE).contains(e.getPlayer().getGameMode())) {
						World world = brokenBlock.getLocation().getWorld();
						if (world != null) {
							world.dropItemNaturally(brokenBlock.getLocation(), pattern.getGeneratedItem());
						}
					}
					Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> brokenBlock.setType(pattern.getGeneratedItem().getType()), pattern.getRegenDelay());
				});

		plugin.getGenerators().stream()
				.filter(generator -> brokenBlock.equals(generator.getBlock()))
				.findFirst()
				.ifPresent(generator -> {
					e.setCancelled(true);
					brokenBlock.setType(Material.AIR);
					plugin.getGenerators().remove(generator);
					if (Arrays.asList(GameMode.SURVIVAL, GameMode.ADVENTURE).contains(e.getPlayer().getGameMode())) {
						World world = brokenBlock.getLocation().getWorld();
						if (world != null) {
							world.dropItemNaturally(brokenBlock.getLocation(), generator.getPattern().getGeneratorItem());
						}
					}
				});
	}
}
