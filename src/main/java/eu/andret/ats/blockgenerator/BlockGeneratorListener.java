package eu.andret.ats.blockgenerator;

import eu.andret.ats.blockgenerator.entity.Generator;
import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import lombok.AllArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Arrays;
import java.util.Optional;

@AllArgsConstructor
public class BlockGeneratorListener implements Listener {
	private final BlockGenerator plugin;

	@EventHandler
	public void place(final BlockPlaceEvent e) {
		plugin.getPatterns()
				.stream()
				.filter(p -> p.matchGeneratorItem(e.getItemInHand()))
				.findFirst()
				.map(p -> new Generator(p, e.getBlock()))
				.ifPresent(g -> {
					System.out.println(g);
					e.getBlockPlaced().getRelative(0, 1, 0).setType(g.getPattern().getGeneratedItem().getType());
					plugin.getGenerators().add(g);
				});
	}

	@EventHandler
	public void destroy(final BlockBreakEvent e) {
		final Block brokenBlock = e.getBlock();
		plugin.getGenerators().stream()
				.filter(generator -> generator.getBlock().getRelative(0, 1, 0).equals(brokenBlock))
				.findFirst()
				.map(Generator::getPattern)
				.ifPresent(pattern -> {
					e.setCancelled(true);
					if (Arrays.asList(GameMode.SURVIVAL, GameMode.ADVENTURE).contains(e.getPlayer().getGameMode())) {
						Optional.of(brokenBlock)
								.map(Block::getLocation)
								.map(Location::getWorld)
								.ifPresent(world -> world.dropItemNaturally(brokenBlock.getLocation(), pattern.getGeneratedItem()));
					}
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> task(brokenBlock, pattern), pattern.getDelay());
				});

		plugin.getGenerators().stream()
				.filter(generator -> brokenBlock.equals(generator.getBlock()))
				.findFirst()
				.ifPresent(generator -> {
					e.setCancelled(true);
					brokenBlock.setType(Material.AIR);
					plugin.getGenerators().remove(generator);
					if (Arrays.asList(GameMode.SURVIVAL, GameMode.ADVENTURE).contains(e.getPlayer().getGameMode())) {
						final World world = brokenBlock.getLocation().getWorld();
						if (world != null) {
							world.dropItemNaturally(brokenBlock.getLocation(), generator.getPattern().getGeneratorItem());
						}
					}
				});
	}

	private void task(final Block brokenBlock, final GeneratorPattern pattern) {
		System.out.println("BEFORE");
		brokenBlock.setType(pattern.getGeneratedItem().getType());
		System.out.println("AFTER");
	}
}
