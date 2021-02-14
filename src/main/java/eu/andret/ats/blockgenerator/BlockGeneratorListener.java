package eu.andret.ats.blockgenerator;

import eu.andret.ats.blockgenerator.entity.Generator;
import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import lombok.Value;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Value
public class BlockGeneratorListener implements Listener {
	BlockGenerator plugin;
	Map<Generator, Integer> schedulers = new HashMap<>();

	@EventHandler
	public void place(final BlockPlaceEvent e) {
		plugin.getPatternList()
				.stream()
				.filter(p -> Objects.equals(p.getGeneratorItem().getItemStack(), e.getItemInHand()))
				.findFirst()
				.map(p -> new Generator(p, e.getBlock()))
				.ifPresent(g -> {
					e.getBlockPlaced().getRelative(0, 1, 0).setType(g.getPattern().getGeneratedItem().getItemStack().getType());
					plugin.getGeneratorList().add(g);
				});
	}

	@EventHandler
	public void destroy(final BlockBreakEvent e) {
		final Block brokenBlock = e.getBlock();
		plugin.getGeneratorList().stream()
				.filter(generator -> generator.getBlock().getRelative(0, 1, 0).equals(brokenBlock))
				.findFirst()
				.ifPresent(generator -> {
					final GeneratorPattern pattern = generator.getPattern();
					e.setCancelled(true);
					brokenBlock.setType(Material.AIR);
					if (Arrays.asList(GameMode.SURVIVAL, GameMode.ADVENTURE).contains(e.getPlayer().getGameMode()) && !pattern.getDropItems().isEmpty()) {
						Optional.of(brokenBlock)
								.map(Block::getLocation)
								.map(Location::getWorld)
								.ifPresent(world -> world.dropItemNaturally(brokenBlock.getLocation(), pattern.getDropItems().next().getItemStack()));
					}
					final int id = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> brokenBlock.setType(pattern.getGeneratedItem().getItemStack().getType()), pattern.getDelay());
					schedulers.put(generator, id);
				});

		plugin.getGeneratorList().stream()
				.filter(generator -> brokenBlock.equals(generator.getBlock()))
				.findFirst()
				.ifPresent(generator -> {
					e.setCancelled(true);
					brokenBlock.setType(Material.AIR);
					if (Arrays.asList(GameMode.SURVIVAL, GameMode.ADVENTURE).contains(e.getPlayer().getGameMode())) {
						Optional.of(brokenBlock)
								.map(Block::getLocation)
								.map(Location::getWorld)
								.ifPresent(world -> world.dropItemNaturally(brokenBlock.getLocation(), generator.getPattern().getGeneratorItem().getItemStack()));
					}
					if (schedulers.containsKey(generator)) {
						plugin.getServer().getScheduler().cancelTask(schedulers.get(generator));
						schedulers.remove(generator);
					}
					plugin.getGeneratorList().remove(generator);
				});
	}
}
