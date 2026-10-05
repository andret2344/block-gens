package eu.andret.blockgens;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import eu.andret.blockgens.config.ExplosionMode;
import eu.andret.blockgens.config.GeneratorPattern;
import eu.andret.blockgens.config.NamedItem;
import eu.andret.blockgens.helper.PluginTest;
import eu.andret.blockgens.util.RandomCollection;
import org.bukkit.ExplosionResult;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockGensListenerTest extends PluginTest {
	private PlayerMock player;

	@BeforeMethod
	public void setUpPlayer() {
		player = server.addPlayer();
		player.setGameMode(GameMode.SURVIVAL);
	}

	@Test
	void placeGenerator() {
		// given
		final GeneratorPattern sponge = pattern("sponge");
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.SPONGE);

		// when
		callPlace(block, sponge.generatorItem().itemStack().clone());

		// then
		assertThat(block.getRelative(0, 1, 0).getType()).isEqualTo(Material.STONE);
		assertThat(plugin.getGeneratorStore().find(block)).contains("sponge");
	}

	@Test
	void placeRegularBlockWhereGeneratorWasStored() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		plugin.getGeneratorStore().save(block, "sponge");
		block.setType(Material.SPONGE);

		// when
		callPlace(block, new ItemStack(Material.SPONGE));

		// then
		assertThat(block.getRelative(0, 1, 0).getType()).isEqualTo(Material.AIR);
		assertThat(plugin.getGeneratorStore().find(block)).isEmpty();
	}

	@Test
	void breakGeneratedInSurvival() {
		// given
		final Block generated = placeGenerator(pattern("sponge"), 0, 5, 0).getRelative(0, 1, 0);

		// when
		final BlockBreakEvent event = breakBlock(generated);

		// then
		assertThat(event.isCancelled()).isTrue();
		assertThat(generated.getType()).isEqualTo(Material.AIR);
		assertThat(droppedItems()).singleElement()
				.extracting(ItemStack::getType)
				.isIn(Material.BREAD, Material.APPLE);
		server.getScheduler().performTicks(1);
		assertThat(generated.getType()).isEqualTo(Material.STONE);
	}

	@Test
	void breakGeneratedInCreative() {
		// given
		final Block generated = placeGenerator(pattern("sponge"), 0, 5, 0).getRelative(0, 1, 0);
		player.setGameMode(GameMode.CREATIVE);

		// when
		breakBlock(generated);

		// then
		assertThat(generated.getType()).isEqualTo(Material.AIR);
		assertThat(droppedItems()).isEmpty();
		server.getScheduler().performTicks(1);
		assertThat(generated.getType()).isEqualTo(Material.STONE);
	}

	@Test
	void breakGeneratorInSurvival() {
		// given
		final GeneratorPattern sponge = pattern("sponge");
		final Block generator = placeGenerator(sponge, 0, 5, 0);

		// when
		final BlockBreakEvent event = breakBlock(generator);

		// then
		assertThat(event.isCancelled()).isTrue();
		assertThat(generator.getType()).isEqualTo(Material.AIR);
		assertThat(plugin.getGeneratorStore().find(generator)).isEmpty();
		assertThat(droppedItems()).singleElement()
				.matches(item -> item.isSimilar(sponge.generatorItem().itemStack()));
	}

	@Test
	void breakGeneratorInCreative() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		player.setGameMode(GameMode.CREATIVE);

		// when
		breakBlock(generator);

		// then
		assertThat(generator.getType()).isEqualTo(Material.AIR);
		assertThat(droppedItems()).isEmpty();
	}

	@Test
	void breakGeneratorCancelsRespawn() {
		// given
		final Block generator = placeGenerator(pattern("dirt"), 0, 5, 0);
		final Block generated = generator.getRelative(0, 1, 0);
		breakBlock(generated);

		// when
		breakBlock(generator);
		server.getScheduler().performTicks(10);

		// then
		assertThat(generated.getType()).isEqualTo(Material.AIR);
	}

	@Test
	void breakStoredButReplacedGenerator() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		generator.setType(Material.DIRT);

		// when
		final BlockBreakEvent event = breakBlock(generator);

		// then
		assertThat(event.isCancelled()).isFalse();
		assertThat(droppedItems()).noneMatch(item -> item.isSimilar(pattern("sponge").generatorItem().itemStack()));
	}

	@Test
	void breakBlockAboveRegularBlock() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.SPONGE);
		final Block above = block.getRelative(0, 1, 0);
		above.setType(Material.STONE);

		// when
		final BlockBreakEvent event = breakBlock(above);

		// then
		assertThat(event.isCancelled()).isFalse();
	}

	@Test
	void chunkLoadRestoresGeneratedBlock() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		final Block generated = generator.getRelative(0, 1, 0);
		generated.setType(Material.AIR);

		// when
		callEvent(new ChunkLoadEvent(generator.getChunk(), false));

		// then
		assertThat(generated.getType()).isEqualTo(Material.STONE);
	}

	@Test
	void chunkLoadKeepsPresentBlockAboveGenerator() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		final Block generated = generator.getRelative(0, 1, 0);
		generated.setType(Material.GLASS);

		// when
		callEvent(new ChunkLoadEvent(generator.getChunk(), false));

		// then
		assertThat(generated.getType()).isEqualTo(Material.GLASS);
	}

	@Test
	void chunkLoadForgetsReplacedGenerator() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		generator.setType(Material.AIR);

		// when
		callEvent(new ChunkLoadEvent(generator.getChunk(), false));

		// then
		assertThat(plugin.getGeneratorStore().find(generator)).isEmpty();
	}

	@Test
	void chunkLoadKeepsGeneratorOfUnknownPattern() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		plugin.getGeneratorStore().save(block, "removed-from-config");

		// when
		callEvent(new ChunkLoadEvent(block.getChunk(), false));

		// then
		assertThat(plugin.getGeneratorStore().find(block)).contains("removed-from-config");
	}

	@Test
	void pistonPushingGenerator() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		final BlockPistonExtendEvent event = new BlockPistonExtendEvent(world.getBlockAt(1, 5, 0),
				List.of(generator), BlockFace.WEST);

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void pistonPushingGeneratedBlock() {
		// given
		final Block generated = placeGenerator(pattern("sponge"), 0, 5, 0).getRelative(0, 1, 0);
		final BlockPistonExtendEvent event = new BlockPistonExtendEvent(world.getBlockAt(1, 6, 0),
				List.of(generated), BlockFace.WEST);

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void pistonPullingGenerator() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		final BlockPistonRetractEvent event = new BlockPistonRetractEvent(world.getBlockAt(2, 5, 0),
				List.of(generator), BlockFace.EAST);

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void pistonMovingRegularBlocks() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.SPONGE);
		final BlockPistonExtendEvent extend = new BlockPistonExtendEvent(world.getBlockAt(1, 5, 0),
				List.of(block), BlockFace.WEST);
		final BlockPistonRetractEvent retract = new BlockPistonRetractEvent(world.getBlockAt(2, 5, 0),
				List.of(block), BlockFace.EAST);

		// when
		callEvent(extend);
		callEvent(retract);

		// then
		assertThat(extend.isCancelled()).isFalse();
		assertThat(retract.isCancelled()).isFalse();
	}

	@Test
	void entityChangingGenerator() {
		// given
		final Block generator = placeGenerator(pattern("dirt"), 0, 5, 0);
		final EntityChangeBlockEvent event = new EntityChangeBlockEvent(player, generator,
				Material.AIR.createBlockData());

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void entityChangingGeneratedBlock() {
		// given
		final Block generated = placeGenerator(pattern("sponge"), 0, 5, 0).getRelative(0, 1, 0);
		final EntityChangeBlockEvent event = new EntityChangeBlockEvent(player, generated,
				Material.INFESTED_STONE.createBlockData());

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void entityChangingRegularBlock() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.DIRT);
		final EntityChangeBlockEvent event = new EntityChangeBlockEvent(player, block, Material.AIR.createBlockData());

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isFalse();
	}

	@Test
	void explosionDropsGenerator() {
		// given
		final GeneratorPattern sponge = pattern("sponge");
		final Block generator = placeGenerator(sponge, 0, 5, 0);
		final List<Block> blocks = new ArrayList<>(List.of(generator));

		// when
		callEvent(entityExplosion(blocks, 1));

		// then
		assertThat(blocks).isEmpty();
		assertThat(generator.getType()).isEqualTo(Material.AIR);
		assertThat(plugin.getGeneratorStore().find(generator)).isEmpty();
		assertThat(droppedItems()).singleElement()
				.matches(item -> item.isSimilar(sponge.generatorItem().itemStack()));
	}

	@Test
	void explosionWithoutYieldDestroysGenerator() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		final List<Block> blocks = new ArrayList<>(List.of(generator));

		// when
		callEvent(entityExplosion(blocks, 0));

		// then
		assertThat(generator.getType()).isEqualTo(Material.AIR);
		assertThat(droppedItems()).isEmpty();
	}

	@Test
	void explosionRemovesGeneratedBlock() {
		// given
		final Block generated = placeGenerator(pattern("sponge"), 0, 5, 0).getRelative(0, 1, 0);
		final List<Block> blocks = new ArrayList<>(List.of(generated));

		// when
		callEvent(entityExplosion(blocks, 1));

		// then
		assertThat(blocks).isEmpty();
		assertThat(generated.getType()).isEqualTo(Material.AIR);
		assertThat(droppedItems()).isEmpty();
		server.getScheduler().performTicks(1);
		assertThat(generated.getType()).isEqualTo(Material.STONE);
	}

	@Test
	void explosionDropsGeneratedBlock() {
		// given
		final GeneratorPattern pattern = addPattern(ExplosionMode.REMOVE, ExplosionMode.DROP);
		final Block generated = placeGenerator(pattern, 0, 5, 0).getRelative(0, 1, 0);
		final List<Block> blocks = new ArrayList<>(List.of(generated));

		// when
		callEvent(entityExplosion(blocks, 1));

		// then
		assertThat(generated.getType()).isEqualTo(Material.AIR);
		assertThat(droppedItems()).singleElement()
				.extracting(ItemStack::getType)
				.isEqualTo(Material.DIAMOND);
		server.getScheduler().performTicks(1);
		assertThat(generated.getType()).isEqualTo(Material.GOLD_BLOCK);
	}

	@Test
	void explosionRemovesGeneratorWithoutDrop() {
		// given
		final GeneratorPattern pattern = addPattern(ExplosionMode.REMOVE, ExplosionMode.REMOVE);
		final Block generator = placeGenerator(pattern, 0, 5, 0);
		final List<Block> blocks = new ArrayList<>(List.of(generator));

		// when
		callEvent(entityExplosion(blocks, 1));

		// then
		assertThat(generator.getType()).isEqualTo(Material.AIR);
		assertThat(plugin.getGeneratorStore().find(generator)).isEmpty();
		assertThat(droppedItems()).isEmpty();
	}

	@Test
	void explosionPrevented() {
		// given
		final GeneratorPattern pattern = addPattern(ExplosionMode.PREVENT, ExplosionMode.PREVENT);
		final Block generator = placeGenerator(pattern, 0, 5, 0);
		final Block generated = generator.getRelative(0, 1, 0);
		final List<Block> blocks = new ArrayList<>(List.of(generator, generated));

		// when
		callEvent(entityExplosion(blocks, 1));

		// then
		assertThat(blocks).isEmpty();
		assertThat(generator.getType()).isEqualTo(Material.IRON_BLOCK);
		assertThat(generated.getType()).isEqualTo(Material.GOLD_BLOCK);
		assertThat(plugin.getGeneratorStore().find(generator)).contains("custom");
		assertThat(droppedItems()).isEmpty();
	}

	@Test
	void explosionOfGeneratorAndGeneratedBlockCancelsRespawn() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		final Block generated = generator.getRelative(0, 1, 0);
		final List<Block> blocks = new ArrayList<>(List.of(generator, generated));

		// when
		callEvent(entityExplosion(blocks, 1));
		server.getScheduler().performTicks(1);

		// then
		assertThat(generator.getType()).isEqualTo(Material.AIR);
		assertThat(generated.getType()).isEqualTo(Material.AIR);
	}

	@Test
	void explosionKeepsRegularBlocks() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.STONE);
		final List<Block> blocks = new ArrayList<>(List.of(block));

		// when
		callEvent(entityExplosion(blocks, 1));

		// then
		assertThat(blocks).containsExactly(block);
	}

	@Test
	void blockExplosionDropsGenerator() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		final List<Block> blocks = new ArrayList<>(List.of(generator));
		final Block bed = world.getBlockAt(3, 5, 0);

		// when
		callEvent(new BlockExplodeEvent(bed, bed.getState(), blocks, 1, ExplosionResult.DESTROY));

		// then
		assertThat(blocks).isEmpty();
		assertThat(generator.getType()).isEqualTo(Material.AIR);
		assertThat(droppedItems()).hasSize(1);
	}

	@Test
	void placeGeneratorFromStack() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.SPONGE);

		// when
		callPlace(block, pattern("sponge").generatorItem().itemStack().asQuantity(5));

		// then
		assertThat(plugin.getGeneratorStore().find(block)).contains("sponge");
	}

	@Test
	void breakOtherBlockPlacedWhileRespawning() {
		// given
		final Block generated = placeGenerator(pattern("dirt"), 0, 5, 0).getRelative(0, 1, 0);
		breakBlock(generated);
		generated.setType(Material.COBBLESTONE);

		// when
		final BlockBreakEvent event = breakBlock(generated);

		// then
		assertThat(event.isCancelled()).isFalse();
		assertThat(droppedItems()).hasSize(1);
	}

	@Test
	void breakGeneratedMaterialPlacedWhileRespawning() {
		// given
		final Block generated = placeGenerator(pattern("dirt"), 0, 5, 0).getRelative(0, 1, 0);
		breakBlock(generated);
		generated.setType(Material.OBSIDIAN);

		// when
		final BlockBreakEvent event = breakBlock(generated);

		// then
		assertThat(event.isCancelled()).isFalse();
		assertThat(droppedItems()).hasSize(1);
	}

	@Test
	void respawnForgetsItsTask() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		breakBlock(generator.getRelative(0, 1, 0));

		// when
		server.getScheduler().performTicks(1);

		// then
		assertThat(plugin.isSchedulerPresent(generator)).isFalse();
	}

	@Test
	void chunkLoadLeavesPendingRespawn() {
		// given
		final Block generator = placeGenerator(pattern("dirt"), 0, 5, 0);
		final Block generated = generator.getRelative(0, 1, 0);
		breakBlock(generated);

		// when
		callEvent(new ChunkLoadEvent(generator.getChunk(), false));

		// then
		assertThat(generated.getType()).isEqualTo(Material.AIR);
		server.getScheduler().performTicks(10);
		assertThat(generated.getType()).isEqualTo(Material.OBSIDIAN);
	}

	@Test
	void placeBlockWhileRespawning() {
		// given
		final Block generated = placeGenerator(pattern("dirt"), 0, 5, 0).getRelative(0, 1, 0);
		breakBlock(generated);
		generated.setType(Material.CHEST);
		final BlockPlaceEvent event = new BlockPlaceEvent(generated, generated.getState(),
				generated.getRelative(0, 0, 1), new ItemStack(Material.CHEST), player, true, EquipmentSlot.HAND);

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void placeGeneratorOnGeneratorWhileRespawning() {
		// given
		final GeneratorPattern sponge = pattern("sponge");
		final Block generated = placeGenerator(sponge, 0, 5, 0).getRelative(0, 1, 0);
		breakBlock(generated);
		generated.setType(Material.SPONGE);
		final BlockPlaceEvent event = new BlockPlaceEvent(generated, generated.getState(),
				generated.getRelative(0, 0, 1), sponge.generatorItem().itemStack().clone(), player, true,
				EquipmentSlot.HAND);

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
		assertThat(plugin.getGeneratorStore().find(generated)).isEmpty();
	}

	@Test
	void emptyBucketWhileRespawning() {
		// given
		final Block generated = placeGenerator(pattern("dirt"), 0, 5, 0).getRelative(0, 1, 0);
		breakBlock(generated);
		final PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(player, generated,
				generated.getRelative(0, 0, 1), BlockFace.NORTH, Material.LAVA_BUCKET, new ItemStack(Material.LAVA_BUCKET),
				EquipmentSlot.HAND);

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void emptyBucketElsewhere() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		final PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(player, block, block.getRelative(0, -1, 0),
				BlockFace.UP, Material.WATER_BUCKET, new ItemStack(Material.WATER_BUCKET), EquipmentSlot.HAND);

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isFalse();
	}

	@Test
	void pistonPushingBlockOntoRespawningSpot() {
		// given
		final Block generated = placeGenerator(pattern("dirt"), 0, 5, 0).getRelative(0, 1, 0);
		breakBlock(generated);
		final Block pushed = world.getBlockAt(1, 6, 0);
		pushed.setType(Material.COBBLESTONE);
		final BlockPistonExtendEvent event = new BlockPistonExtendEvent(world.getBlockAt(2, 6, 0), List.of(pushed),
				BlockFace.WEST);

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void pistonHeadEnteringRespawningSpot() {
		// given
		final Block generated = placeGenerator(pattern("dirt"), 0, 5, 0).getRelative(0, 1, 0);
		breakBlock(generated);
		final BlockPistonExtendEvent event = new BlockPistonExtendEvent(world.getBlockAt(1, 6, 0), List.of(),
				BlockFace.WEST);

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void pistonPullingBlockOntoRespawningSpot() {
		// given
		final Block generated = placeGenerator(pattern("dirt"), 0, 5, 0).getRelative(0, 1, 0);
		breakBlock(generated);
		final Block pulled = world.getBlockAt(1, 6, 0);
		pulled.setType(Material.COBBLESTONE);
		// A sticky piston at x = -1 facing east pulls the block at x = 1 to x = 0
		final BlockPistonRetractEvent event = new BlockPistonRetractEvent(world.getBlockAt(-1, 6, 0), List.of(pulled),
				BlockFace.EAST);

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void blockLandingOnRespawningSpot() {
		// given
		final Block generated = placeGenerator(pattern("dirt"), 0, 5, 0).getRelative(0, 1, 0);
		breakBlock(generated);
		final EntityChangeBlockEvent event = new EntityChangeBlockEvent(player, generated,
				Material.SAND.createBlockData());

		// when
		callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void placeGeneratorUnderBlock() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.SPONGE);
		final Block above = block.getRelative(0, 1, 0);
		above.setType(Material.CHEST);

		// when
		callPlace(block, pattern("sponge").generatorItem().itemStack().clone());

		// then
		assertThat(above.getType()).isEqualTo(Material.CHEST);
		assertThat(plugin.getGeneratorStore().find(block)).contains("sponge");
	}

	@Test
	void placeGeneratorUnderCaveAir() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.SPONGE);
		final Block above = block.getRelative(0, 1, 0);
		above.setType(Material.CAVE_AIR);

		// when
		callPlace(block, pattern("sponge").generatorItem().itemStack().clone());

		// then
		assertThat(above.getType()).isEqualTo(Material.STONE);
	}

	@Test
	void breakBlockAboveWaitingGenerator() {
		// given
		final Block above = placeWaitingGenerator();

		// when
		final BlockBreakEvent event = breakBlock(above);
		server.getScheduler().performTicks(1);

		// then
		assertThat(event.isCancelled()).isFalse();
		assertThat(above.getType()).isEqualTo(Material.STONE);
	}

	@Test
	void explosionOfBlockAboveWaitingGenerator() {
		// given
		final Block above = placeWaitingGenerator();
		final List<Block> blocks = new ArrayList<>(List.of(above));

		// when
		callEvent(entityExplosion(blocks, 1));
		above.setType(Material.AIR);
		server.getScheduler().performTicks(1);

		// then
		assertThat(blocks).containsExactly(above);
		assertThat(above.getType()).isEqualTo(Material.STONE);
	}

	@Test
	void decayOfLeavesAboveWaitingGenerator() {
		// given
		final Block above = placeWaitingGenerator();
		above.setType(Material.OAK_LEAVES);

		// when
		callEvent(new LeavesDecayEvent(above));
		above.setType(Material.AIR);
		server.getScheduler().performTicks(1);

		// then
		assertThat(above.getType()).isEqualTo(Material.STONE);
	}

	@Test
	void destroyOfBlockAboveWaitingGenerator() {
		// given
		final Block above = placeWaitingGenerator();
		above.setType(Material.TORCH);

		// when
		callEvent(new BlockDestroyEvent(above, Material.AIR.createBlockData(), Material.TORCH.createBlockData(), 0,
				true));
		above.setType(Material.AIR);
		server.getScheduler().performTicks(1);

		// then
		assertThat(above.getType()).isEqualTo(Material.STONE);
	}

	@Test
	void cancelledBreakOfBlockAboveWaitingGenerator() {
		// given
		final Block above = placeWaitingGenerator();
		final BlockBreakEvent event = new BlockBreakEvent(above, player);
		event.setCancelled(true);

		// when
		callEvent(event);
		server.getScheduler().performTicks(1);

		// then
		assertThat(above.getType()).isEqualTo(Material.CHEST);
	}

	@Test
	void placeGeneratorCancelledByOtherPlugin() {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.SPONGE);
		final Block above = block.getRelative(0, 1, 0);
		above.setType(Material.CHEST);
		// Cancels at the highest priority a cancelling listener may use, after the plugin's own listeners
		server.getPluginManager().registerEvents(new Listener() {
			@EventHandler(priority = EventPriority.HIGHEST)
			public void protect(@NotNull final BlockPlaceEvent event) {
				event.setCancelled(true);
			}
		}, plugin);

		// when
		callPlace(block, pattern("sponge").generatorItem().itemStack().clone());

		// then
		assertThat(above.getType()).isEqualTo(Material.CHEST);
		assertThat(plugin.getGeneratorStore().find(block)).isEmpty();
	}

	@Test
	void breakGeneratedCancelledByOtherPlugin() {
		// given
		final Block generated = placeGenerator(pattern("sponge"), 0, 5, 0).getRelative(0, 1, 0);
		protectFromBreaking();

		// when
		breakBlock(generated);
		server.getScheduler().performTicks(1);

		// then
		assertThat(generated.getType()).isEqualTo(Material.STONE);
		assertThat(plugin.isSchedulerPresent(generated.getRelative(0, -1, 0))).isFalse();
		assertThat(droppedItems()).isEmpty();
	}

	@Test
	void breakGeneratorCancelledByOtherPlugin() {
		// given
		final Block generator = placeGenerator(pattern("sponge"), 0, 5, 0);
		protectFromBreaking();

		// when
		breakBlock(generator);

		// then
		assertThat(generator.getType()).isEqualTo(Material.SPONGE);
		assertThat(plugin.getGeneratorStore().find(generator)).contains("sponge");
		assertThat(droppedItems()).isEmpty();
	}

	// Cancels breaking at the default priority, as region protections do
	private void protectFromBreaking() {
		server.getPluginManager().registerEvents(new Listener() {
			@EventHandler
			public void protect(@NotNull final BlockBreakEvent event) {
				event.setCancelled(true);
			}
		}, plugin);
	}

	/**
	 * Places a sponge generator under a chest and returns the chest.
	 */
	@NotNull
	private Block placeWaitingGenerator() {
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.SPONGE);
		final Block above = block.getRelative(0, 1, 0);
		above.setType(Material.CHEST);
		callPlace(block, pattern("sponge").generatorItem().itemStack().clone());
		return above;
	}

	@NotNull
	private GeneratorPattern addPattern(@NotNull final ExplosionMode generator, @NotNull final ExplosionMode generated) {
		final RandomCollection<NamedItem> drops = new RandomCollection<>();
		drops.add(new NamedItem("diamond", new ItemStack(Material.DIAMOND)), 1);
		final GeneratorPattern pattern = new GeneratorPattern("custom", 1,
				new NamedItem("iron", new ItemStack(Material.IRON_BLOCK)),
				new NamedItem("gold", new ItemStack(Material.GOLD_BLOCK)),
				drops, generator, generated, null);
		plugin.getPatternList().add(pattern);
		return pattern;
	}

	@NotNull
	private EntityExplodeEvent entityExplosion(@NotNull final List<Block> blocks, final float yield) {
		return new EntityExplodeEvent(player, player.getLocation(), blocks, yield, ExplosionResult.DESTROY);
	}

	/**
	 * Breaks the block the way the server does: the event first, then air unless a listener cancelled it.
	 */
	@NotNull
	private BlockBreakEvent breakBlock(@NotNull final Block block) {
		final BlockBreakEvent event = new BlockBreakEvent(block, player);
		callEvent(event);
		if (!event.isCancelled()) {
			block.setType(Material.AIR);
		}
		return event;
	}

	private void callPlace(@NotNull final Block block, @NotNull final ItemStack itemInHand) {
		callEvent(new BlockPlaceEvent(block, block.getState(), block.getRelative(0, -1, 0), itemInHand, player, true,
				EquipmentSlot.HAND));
	}

	private void callEvent(@NotNull final Event event) {
		server.getPluginManager().callEvent(event);
	}
}
