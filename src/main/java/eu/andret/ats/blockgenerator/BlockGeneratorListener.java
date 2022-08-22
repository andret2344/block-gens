/*
 * Copyright Andret Tools System (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator;

import eu.andret.ats.blockgenerator.entity.Generator;
import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import lombok.AllArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@AllArgsConstructor
public class BlockGeneratorListener implements Listener {
	private static final String GENERATOR = "generator";

	private final BlockGeneratorPlugin plugin;
	private final Map<Block, Integer> schedulers = new HashMap<>();

	@EventHandler
	public void place(@NotNull final BlockPlaceEvent event) {
		final Block placed = event.getBlock();
		plugin.getPatternList()
				.stream()
				.filter(pattern -> Objects.equals(pattern.getGeneratorItem().getItemStack(), event.getItemInHand()))
				.findFirst()
				.map(pattern -> new Generator(pattern, placed))
				.ifPresent(generator -> {
					final GeneratorPattern pattern = generator.getPattern();
					final Block relative = placed.getRelative(0, 1, 0);
					relative.setType(pattern.getGeneratedItem().getItemStack().getType());
					placed.setMetadata(GENERATOR, new FixedMetadataValue(plugin, pattern.getName()));
				});
	}

	@EventHandler
	public void destroyGenerated(@NotNull final BlockBreakEvent event) {
		final Block brokenBlock = event.getBlock();
		final Block relative = brokenBlock.getRelative(0, -1, 0);
		final List<MetadataValue> relativeMetadata = relative.getMetadata(GENERATOR);
		if (relativeMetadata.isEmpty()) {
			return;
		}
		plugin.getPattern(relativeMetadata.get(0).asString()).ifPresent(pattern -> {
			event.setCancelled(true);
			brokenBlock.setType(Material.AIR);
			if (Arrays.asList(GameMode.SURVIVAL, GameMode.ADVENTURE).contains(event.getPlayer().getGameMode())
					&& !pattern.getDropItems().isEmpty()) {
				final ItemStack dropItemStack = pattern.getDropItems().next().getItemStack();
				Optional.of(brokenBlock)
						.map(Block::getLocation)
						.map(Location::getWorld)
						.ifPresent(world -> world.dropItemNaturally(brokenBlock.getLocation(), dropItemStack));
			}
			final Material material = pattern.getGeneratedItem().getItemStack().getType();
			final int id = plugin.getServer().getScheduler()
					.scheduleSyncDelayedTask(plugin, () -> brokenBlock.setType(material), pattern.getDelay());
			schedulers.put(relative, id);
		});
	}

	@EventHandler
	public void destroyGenerator(@NotNull final BlockBreakEvent event) {
		final Block brokenBlock = event.getBlock();
		final List<MetadataValue> metadata = brokenBlock.getMetadata(GENERATOR);
		if (metadata.isEmpty()) {
			return;
		}

		plugin.getPattern(metadata.get(0).asString()).ifPresent(pattern -> {
			event.setCancelled(true);
			if (Arrays.asList(GameMode.SURVIVAL, GameMode.ADVENTURE).contains(event.getPlayer().getGameMode())) {
				Optional.of(brokenBlock)
						.map(Block::getLocation)
						.map(Location::getWorld)
						.ifPresent(world -> world.dropItemNaturally(brokenBlock.getLocation(),
								pattern.getGeneratorItem().getItemStack()));
			}
			brokenBlock.setType(Material.AIR);
			brokenBlock.removeMetadata(GENERATOR, plugin);
			final Block relative = brokenBlock.getRelative(0, 1, 0);
			if (schedulers.containsKey(relative)) {
				plugin.getServer().getScheduler().cancelTask(schedulers.get(relative));
				schedulers.remove(relative);
			}
		});
	}
}
