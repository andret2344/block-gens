package eu.andret.blockgens;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import eu.andret.blockgens.config.GeneratorPattern;
import eu.andret.blockgens.config.NamedItem;
import eu.andret.blockgens.storage.GeneratorStore;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class BlockGensListener implements Listener {
	@NotNull
	private final BlockGensPlugin plugin;

	public BlockGensListener(@NotNull final BlockGensPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void placeOnReserved(@NotNull final BlockPlaceEvent event) {
		if (isReserved(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	// MONITOR, so a placement cancelled by any other plugin (e.g. a region protection) never creates a generator
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void place(@NotNull final BlockPlaceEvent event) {
		final Block placed = event.getBlock();
		plugin.getPatternList()
				.stream()
				// isSimilar, not equals - the amount in hand does not matter
				.filter(pattern -> pattern.generatorItem().itemStack().isSimilar(event.getItemInHand()))
				.findFirst()
				.ifPresentOrElse(pattern -> {
					plugin.getGeneratorStore().save(placed, pattern.name());
					fill(placed, pattern);
				}, () -> plugin.getGeneratorStore().remove(placed));
	}

	// HIGH and ignoring cancelled events, so region protections (cancelling at NORMAL or lower) keep players out
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void destroyGenerated(@NotNull final BlockBreakEvent event) {
		final Block brokenBlock = event.getBlock();
		findGeneratedPattern(brokenBlock).ifPresent(pattern -> {
			event.setCancelled(true);
			clearGenerated(brokenBlock, pattern);
			if (Arrays.asList(GameMode.SURVIVAL, GameMode.ADVENTURE).contains(event.getPlayer().getGameMode())) {
				final NamedItem next = pattern.dropItems().next();
				if (next == null) {
					return;
				}
				final ItemStack dropItemStack = next.itemStack();
				Optional.of(brokenBlock)
						.map(Block::getLocation)
						.map(Location::getWorld)
						.ifPresent(world -> world.dropItemNaturally(brokenBlock.getLocation(), dropItemStack));
			}
		});
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void destroyGenerator(@NotNull final BlockBreakEvent event) {
		final Block brokenBlock = event.getBlock();
		findPattern(brokenBlock).ifPresent(pattern -> {
			event.setCancelled(true);
			if (Arrays.asList(GameMode.SURVIVAL, GameMode.ADVENTURE).contains(event.getPlayer().getGameMode())) {
				Optional.of(brokenBlock)
						.map(Block::getLocation)
						.map(Location::getWorld)
						.ifPresent(world -> world.dropItemNaturally(brokenBlock.getLocation(),
								pattern.generatorItem().itemStack()));
			}
			removeGenerator(brokenBlock);
		});
	}

	/**
	 * A generator placed under another block waits until that block is gone. These are the ways a block above a
	 * generator can disappear; anything else (e.g. WorldEdit) is caught when the chunk loads again.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void breakAbove(@NotNull final BlockBreakEvent event) {
		fillLater(event.getBlock());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void burnAbove(@NotNull final BlockBurnEvent event) {
		fillLater(event.getBlock());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void decayAbove(@NotNull final LeavesDecayEvent event) {
		fillLater(event.getBlock());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void fadeAbove(@NotNull final BlockFadeEvent event) {
		fillLater(event.getBlock());
	}

	// Paper's event for blocks destroyed by physics, e.g. a torch that lost its support
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyAbove(@NotNull final BlockDestroyEvent event) {
		fillLater(event.getBlock());
	}

	@EventHandler(ignoreCancelled = true)
	public void bucketEmpty(@NotNull final PlayerBucketEmptyEvent event) {
		if (isReserved(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	// The blocks and the piston head move the way the piston faces
	@EventHandler(ignoreCancelled = true)
	public void pistonExtend(@NotNull final BlockPistonExtendEvent event) {
		final BlockFace direction = event.getDirection();
		if (event.getBlocks().stream().anyMatch(this::isProtected)
				|| isReserved(event.getBlock().getRelative(direction))
				|| event.getBlocks().stream().map(block -> block.getRelative(direction)).anyMatch(this::isReserved)) {
			event.setCancelled(true);
		}
	}

	// The direction is still the one the piston faces, the pulled blocks move the other way
	@EventHandler(ignoreCancelled = true)
	public void pistonRetract(@NotNull final BlockPistonRetractEvent event) {
		final BlockFace direction = event.getDirection().getOppositeFace();
		if (event.getBlocks().stream().anyMatch(this::isProtected)
				|| event.getBlocks().stream().map(block -> block.getRelative(direction)).anyMatch(this::isReserved)) {
			event.setCancelled(true);
		}
	}

	// Endermen picking blocks up or putting them down, withers breaking them, silverfish hiding in them, sand landing
	@EventHandler(ignoreCancelled = true)
	public void entityChangeBlock(@NotNull final EntityChangeBlockEvent event) {
		if (isProtected(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void entityExplode(@NotNull final EntityExplodeEvent event) {
		explode(event.blockList(), event.getYield());
	}

	@EventHandler(ignoreCancelled = true)
	public void blockExplode(@NotNull final BlockExplodeEvent event) {
		explode(event.blockList(), event.getYield());
	}

	/**
	 * Takes the generators and the generated blocks out of the exploded blocks, so the server does not drop them as
	 * regular blocks, and applies the explosion mode of their pattern instead.
	 */
	private void explode(@NotNull final List<Block> blocks, final float yield) {
		final List<Block> generators = blocks.stream().filter(block -> findPattern(block).isPresent()).toList();
		final List<Block> generated = blocks.stream().filter(block -> findGeneratedPattern(block).isPresent()).toList();
		blocks.removeAll(generators);
		blocks.removeAll(generated);
		// Regular blocks above generators are destroyed by the explosion, the generators start afterwards
		blocks.forEach(this::fillLater);
		// The generated blocks go first, as they are found through the generator below them
		generated.forEach(block -> findGeneratedPattern(block).ifPresent(pattern -> {
			switch (pattern.generatedExplosion()) {
				case PREVENT -> {
				}
				case DROP -> {
					clearGenerated(block, pattern);
					if (isDropped(yield)) {
						Optional.ofNullable(pattern.dropItems().next())
								.ifPresent(item -> drop(block, item.itemStack()));
					}
				}
				case REMOVE -> clearGenerated(block, pattern);
			}
		}));
		generators.forEach(block -> findPattern(block).ifPresent(pattern -> {
			switch (pattern.generatorExplosion()) {
				case PREVENT -> {
				}
				case DROP -> {
					removeGenerator(block);
					if (isDropped(yield)) {
						drop(block, pattern.generatorItem().itemStack());
					}
				}
				case REMOVE -> removeGenerator(block);
			}
		}));
	}

	@EventHandler
	public void load(@NotNull final ChunkLoadEvent event) {
		restore(event.getChunk());
	}

	/**
	 * Brings back the generated blocks whose re-spawn did not happen, e.g. because the server stopped in the meantime,
	 * starts the generators whose block above has gone, and forgets the generators that were removed without being
	 * broken by a player.
	 */
	public void restore(@NotNull final Chunk chunk) {
		final GeneratorStore store = plugin.getGeneratorStore();
		store.findAll(chunk).forEach((block, patternName) -> plugin.getPattern(patternName).ifPresent(pattern -> {
			if (!isGenerator(block, pattern)) {
				store.remove(block);
				return;
			}
			fill(block, pattern);
		}));
	}

	/**
	 * Puts the generated block above the generator if nothing is there. A pending re-spawn puts it back itself; doing
	 * it earlier would let it be broken as a regular block.
	 */
	private void fill(@NotNull final Block generator, @NotNull final GeneratorPattern pattern) {
		final Block relative = generator.getRelative(0, 1, 0);
		if (relative.getType().isAir() && !plugin.isSchedulerPresent(generator)) {
			relative.setType(pattern.generatedItem().itemStack().getType());
		}
	}

	// The events come before the block is removed, so the check waits a tick
	private void fillLater(@NotNull final Block block) {
		final Block generator = block.getRelative(0, -1, 0);
		if (findPattern(generator).isPresent()) {
			plugin.getServer().getScheduler().runTask(plugin, () ->
					findPattern(generator).ifPresent(pattern -> fill(generator, pattern)));
		}
	}

	private void clearGenerated(@NotNull final Block block, @NotNull final GeneratorPattern pattern) {
		block.setType(Material.AIR);
		final Block generator = block.getRelative(0, -1, 0);
		final Material material = pattern.generatedItem().itemStack().getType();
		final int id = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
			block.setType(material);
			plugin.removeScheduler(generator);
		}, pattern.delay());
		plugin.addScheduler(generator, id);
	}

	private void removeGenerator(@NotNull final Block block) {
		block.setType(Material.AIR);
		plugin.getGeneratorStore().remove(block);
		// The re-spawn of the generated block is kept under the generator
		if (plugin.isSchedulerPresent(block)) {
			plugin.cancelScheduler(block);
		}
	}

	private void drop(@NotNull final Block block, @NotNull final ItemStack itemStack) {
		block.getWorld().dropItemNaturally(block.getLocation(), itemStack);
	}

	private boolean isDropped(final float yield) {
		return ThreadLocalRandom.current().nextFloat() < yield;
	}

	private boolean isProtected(@NotNull final Block block) {
		return findPattern(block).isPresent() || isReserved(block);
	}

	/**
	 * The spot above a generator belongs to its generated block, also while it is re-spawning - anything put there
	 * would be overwritten by the re-spawn. A block that was already there when the generator was placed stays until
	 * it is removed.
	 */
	private boolean isReserved(@NotNull final Block block) {
		return findPattern(block.getRelative(0, -1, 0)).isPresent();
	}

	/**
	 * While the generated block is re-spawning, whatever sits above the generator was put there by someone else, so it
	 * is a regular block - otherwise any block placed there could be broken for the drops.
	 */
	@NotNull
	private Optional<GeneratorPattern> findGeneratedPattern(@NotNull final Block block) {
		final Block generator = block.getRelative(0, -1, 0);
		return findPattern(generator)
				.filter(pattern -> block.getType() == pattern.generatedItem().itemStack().getType())
				.filter(pattern -> !plugin.isSchedulerPresent(generator));
	}

	@NotNull
	private Optional<GeneratorPattern> findPattern(@NotNull final Block block) {
		return plugin.getGeneratorStore()
				.find(block)
				.flatMap(plugin::getPattern)
				.filter(pattern -> isGenerator(block, pattern));
	}

	private boolean isGenerator(@NotNull final Block block, @NotNull final GeneratorPattern pattern) {
		return block.getType() == pattern.generatorItem().itemStack().getType();
	}
}
