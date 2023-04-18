/*
 * Copyright Andret Tools System (c) 2018-2023. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator;

import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import eu.andret.ats.blockgenerator.entity.NamedItem;
import eu.andret.ats.blockgenerator.utils.RandomCollection;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitScheduler;
import org.mockito.MockedStatic;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlockGeneratorListenerTest {
	@Test
	void placeWhenEmptyList() {
		// given
		final BlockPlaceEvent blockPlaceEvent = mock(BlockPlaceEvent.class);
		final Block block = mock(Block.class);
		final Block relative = mock(Block.class);
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		when(blockPlaceEvent.getBlock()).thenReturn(block);
		when(blockGeneratorPlugin.getPatternList()).thenReturn(Collections.emptyList());
		when(block.getRelative(0, 1, 0)).thenReturn(relative);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);

		// when
		listener.place(blockPlaceEvent);

		// then
		verify(relative, times(0)).setType(any(Material.class));
	}

	@Test
	void placeNonExisting() {
		// given
		final BlockPlaceEvent blockPlaceEvent = mock(BlockPlaceEvent.class);
		final Block block = mock(Block.class);
		final Block relative = mock(Block.class);
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final GeneratorPattern generatorPattern = new GeneratorPattern(
				"test",
				1,
				new NamedItem("generator", new ItemStack(Material.SPONGE)),
				new NamedItem("generated", new ItemStack(Material.OBSIDIAN)),
				new RandomCollection<>());
		when(blockPlaceEvent.getBlock()).thenReturn(block);
		when(blockPlaceEvent.getItemInHand()).thenReturn(new ItemStack(Material.DIRT));
		when(blockGeneratorPlugin.getPatternList()).thenReturn(List.of(generatorPattern));
		when(block.getRelative(0, 1, 0)).thenReturn(relative);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);

		// when
		listener.place(blockPlaceEvent);

		// then
		verify(relative, times(0)).setType(any(Material.class));
	}

	@Test
	void placeExisting() {
		// given
		final BlockPlaceEvent blockPlaceEvent = mock(BlockPlaceEvent.class);
		final Block block = mock(Block.class);
		final Block relative = mock(Block.class);
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final ItemStack generator = mock(ItemStack.class);
		final GeneratorPattern generatorPattern = new GeneratorPattern(
				"test",
				1,
				new NamedItem("generator", generator),
				new NamedItem("generated", new ItemStack(Material.OBSIDIAN)),
				new RandomCollection<>());
		when(blockPlaceEvent.getBlock()).thenReturn(block);
		when(blockPlaceEvent.getItemInHand()).thenReturn(generator);
		when(blockGeneratorPlugin.getPatternList()).thenReturn(List.of(generatorPattern));
		when(block.getRelative(0, 1, 0)).thenReturn(relative);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);

		// when
		listener.place(blockPlaceEvent);

		// then
		verify(relative).setType(Material.OBSIDIAN);
		verify(block).setMetadata(eq("generator"), argThat(x ->
				Objects.equals(x.getOwningPlugin(), blockGeneratorPlugin) && x.asString().equals("test")));
	}

	@Test
	void destroyGenerated() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final Block relative = mock(Block.class);
		final MetadataValue relativeMetadata = new FixedMetadataValue(blockGeneratorPlugin, "TEST");
		final Player player = mock(Player.class);
		final World world = mock(World.class);
		final Location location = new Location(world, 1, 1, 1);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		final Server server = mock(Server.class);
		final BukkitScheduler scheduler = mock(BukkitScheduler.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getRelative(0, -1, 0)).thenReturn(relative);
		when(relative.getMetadata(any())).thenReturn(List.of(relativeMetadata));
		when(event.getPlayer()).thenReturn(player);
		when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
		when(brokenBlock.getLocation()).thenReturn(location);
		when(blockGeneratorPlugin.getServer()).thenReturn(server);
		when(server.getScheduler()).thenReturn(scheduler);
		RandomCollection<NamedItem> randomCollection = new RandomCollection<>();
		final ItemFactory itemFactory = mock(ItemFactory.class);
		when(itemFactory.equals(any(), any())).thenReturn(true);
		try (final MockedStatic<Bukkit> utilities = mockStatic(Bukkit.class)) {
			utilities.when(Bukkit::getItemFactory).thenReturn(itemFactory);
			randomCollection.add(new NamedItem("1", new ItemStack(Material.DIRT)), 5);
			when(blockGeneratorPlugin.getPattern("TEST")).thenReturn(Optional.of(new GeneratorPattern(
					"NAME", 1L,
					new NamedItem("2", new ItemStack(Material.STONE)),
					new NamedItem("3", new ItemStack(Material.CRYING_OBSIDIAN)),
					randomCollection
			)));

			// when
			listener.destroyGenerated(event);

			// then
			verify(event).setCancelled(true);
			verify(brokenBlock).setType(Material.AIR);
			verify(world).dropItemNaturally(eq(location), any());
			verify(scheduler).scheduleSyncDelayedTask(eq(blockGeneratorPlugin), any(Runnable.class), eq(1L));
			verify(blockGeneratorPlugin).addScheduler(eq(relative), anyInt());
		}
	}

	@Test
	void destroyGeneratedNoWorld() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final Block relative = mock(Block.class);
		final MetadataValue relativeMetadata = new FixedMetadataValue(blockGeneratorPlugin, "TEST");
		final Player player = mock(Player.class);
		final World world = mock(World.class);
		final Location location = new Location(null, 1, 1, 1);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		final Server server = mock(Server.class);
		final BukkitScheduler scheduler = mock(BukkitScheduler.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getRelative(0, -1, 0)).thenReturn(relative);
		when(relative.getMetadata(any())).thenReturn(List.of(relativeMetadata));
		when(event.getPlayer()).thenReturn(player);
		when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
		when(brokenBlock.getLocation()).thenReturn(location);
		when(blockGeneratorPlugin.getServer()).thenReturn(server);
		when(server.getScheduler()).thenReturn(scheduler);
		RandomCollection<NamedItem> randomCollection = new RandomCollection<>();
		final ItemFactory itemFactory = mock(ItemFactory.class);
		when(itemFactory.equals(any(), any())).thenReturn(true);
		try (final MockedStatic<Bukkit> utilities = mockStatic(Bukkit.class)) {
			utilities.when(Bukkit::getItemFactory).thenReturn(itemFactory);
			randomCollection.add(new NamedItem("1", new ItemStack(Material.DIRT)), 5);
			when(blockGeneratorPlugin.getPattern("TEST")).thenReturn(Optional.of(new GeneratorPattern(
					"NAME", 1L,
					new NamedItem("2", new ItemStack(Material.STONE)),
					new NamedItem("3", new ItemStack(Material.CRYING_OBSIDIAN)),
					randomCollection
			)));

			// when
			listener.destroyGenerated(event);

			// then
			verify(event).setCancelled(true);
			verify(brokenBlock).setType(Material.AIR);
			verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
			verify(scheduler).scheduleSyncDelayedTask(eq(blockGeneratorPlugin), any(Runnable.class), eq(1L));
			verify(blockGeneratorPlugin).addScheduler(eq(relative), anyInt());
		}
	}

	@Test
	void destroyGeneratedNoDrop() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final Block relative = mock(Block.class);
		final MetadataValue relativeMetadata = new FixedMetadataValue(blockGeneratorPlugin, "TEST");
		final Player player = mock(Player.class);
		final World world = mock(World.class);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		final Server server = mock(Server.class);
		final BukkitScheduler scheduler = mock(BukkitScheduler.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getRelative(0, -1, 0)).thenReturn(relative);
		when(relative.getMetadata(any())).thenReturn(List.of(relativeMetadata));
		when(event.getPlayer()).thenReturn(player);
		when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
		when(blockGeneratorPlugin.getServer()).thenReturn(server);
		when(server.getScheduler()).thenReturn(scheduler);
		final ItemFactory itemFactory = mock(ItemFactory.class);
		when(itemFactory.equals(any(), any())).thenReturn(true);
		try (final MockedStatic<Bukkit> utilities = mockStatic(Bukkit.class)) {
			utilities.when(Bukkit::getItemFactory).thenReturn(itemFactory);
			when(blockGeneratorPlugin.getPattern("TEST")).thenReturn(Optional.of(new GeneratorPattern(
					"NAME", 1L,
					new NamedItem("2", new ItemStack(Material.STONE)),
					new NamedItem("3", new ItemStack(Material.CRYING_OBSIDIAN)),
					new RandomCollection<>()
			)));

			// when
			listener.destroyGenerated(event);

			// then
			verify(event).setCancelled(true);
			verify(brokenBlock).setType(Material.AIR);
			verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
			verify(scheduler).scheduleSyncDelayedTask(eq(blockGeneratorPlugin), any(Runnable.class), eq(1L));
			verify(blockGeneratorPlugin).addScheduler(eq(relative), anyInt());
		}
	}

	@Test
	void destroyGeneratedCreativeMode() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final Block relative = mock(Block.class);
		final MetadataValue relativeMetadata = new FixedMetadataValue(blockGeneratorPlugin, "TEST");
		final Player player = mock(Player.class);
		final World world = mock(World.class);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		final Server server = mock(Server.class);
		final BukkitScheduler scheduler = mock(BukkitScheduler.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getRelative(0, -1, 0)).thenReturn(relative);
		when(relative.getMetadata(any())).thenReturn(List.of(relativeMetadata));
		when(event.getPlayer()).thenReturn(player);
		when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
		when(blockGeneratorPlugin.getServer()).thenReturn(server);
		when(server.getScheduler()).thenReturn(scheduler);
		final ItemFactory itemFactory = mock(ItemFactory.class);
		when(itemFactory.equals(any(), any())).thenReturn(true);
		try (final MockedStatic<Bukkit> utilities = mockStatic(Bukkit.class)) {
			utilities.when(Bukkit::getItemFactory).thenReturn(itemFactory);
			when(blockGeneratorPlugin.getPattern("TEST")).thenReturn(Optional.of(new GeneratorPattern(
					"NAME", 1L,
					new NamedItem("2", new ItemStack(Material.STONE)),
					new NamedItem("3", new ItemStack(Material.CRYING_OBSIDIAN)),
					new RandomCollection<>()
			)));

			// when
			listener.destroyGenerated(event);

			// then
			verify(event).setCancelled(true);
			verify(brokenBlock).setType(Material.AIR);
			verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
			verify(scheduler).scheduleSyncDelayedTask(eq(blockGeneratorPlugin), any(Runnable.class), eq(1L));
			verify(blockGeneratorPlugin).addScheduler(eq(relative), anyInt());
		}
	}

	@Test
	void destroyGeneratedNoPattern() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final Block relative = mock(Block.class);
		final MetadataValue relativeMetadata = new FixedMetadataValue(blockGeneratorPlugin, "TEST");
		final Player player = mock(Player.class);
		final World world = mock(World.class);
		final Location location = new Location(world, 1, 1, 1);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		final Server server = mock(Server.class);
		final BukkitScheduler scheduler = mock(BukkitScheduler.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getRelative(0, -1, 0)).thenReturn(relative);
		when(relative.getMetadata(any())).thenReturn(List.of(relativeMetadata));
		when(event.getPlayer()).thenReturn(player);
		when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
		when(brokenBlock.getLocation()).thenReturn(location);
		when(blockGeneratorPlugin.getServer()).thenReturn(server);
		when(server.getScheduler()).thenReturn(scheduler);
		RandomCollection<NamedItem> randomCollection = new RandomCollection<>();
		final ItemFactory itemFactory = mock(ItemFactory.class);
		when(itemFactory.equals(any(), any())).thenReturn(true);
		try (final MockedStatic<Bukkit> utilities = mockStatic(Bukkit.class)) {
			utilities.when(Bukkit::getItemFactory).thenReturn(itemFactory);
			randomCollection.add(new NamedItem("1", new ItemStack(Material.DIRT)), 5);
			when(blockGeneratorPlugin.getPattern("TEST")).thenReturn(
					Optional.empty()
			);

			// when
			listener.destroyGenerated(event);

			// then
			verify(event, never()).setCancelled(true);
			verify(brokenBlock, never()).setType(any(Material.class));
			verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
			verify(scheduler, never()).scheduleSyncDelayedTask(any(BlockGeneratorPlugin.class), any(Runnable.class), eq(1L));
			verify(blockGeneratorPlugin, never()).addScheduler(any(Block.class), anyInt());
		}
	}

	@Test
	void destroyGeneratedNoMetadata() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final Block relative = mock(Block.class);
		final World world = mock(World.class);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		final Server server = mock(Server.class);
		final BukkitScheduler scheduler = mock(BukkitScheduler.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getRelative(0, -1, 0)).thenReturn(relative);
		when(relative.getMetadata(any())).thenReturn(Collections.emptyList());
		when(blockGeneratorPlugin.getServer()).thenReturn(server);
		when(server.getScheduler()).thenReturn(scheduler);

		// when
		listener.destroyGenerated(event);

		// then
		verify(event, never()).setCancelled(true);
		verify(brokenBlock, never()).setType(any(Material.class));
		verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
		verify(scheduler, never()).scheduleSyncDelayedTask(any(BlockGeneratorPlugin.class), any(Runnable.class), eq(1L));
		verify(blockGeneratorPlugin, never()).addScheduler(any(Block.class), anyInt());
	}

	@Test
	void destroyGenerator() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final Block relative = mock(Block.class);
		final MetadataValue relativeMetadata = new FixedMetadataValue(blockGeneratorPlugin, "TEST");
		final Player player = mock(Player.class);
		final World world = mock(World.class);
		final Location location = new Location(world, 1, 1, 1);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getRelative(0, 1, 0)).thenReturn(relative);
		when(brokenBlock.getMetadata("generator")).thenReturn(List.of(relativeMetadata));
		when(brokenBlock.getLocation()).thenReturn(location);
		when(blockGeneratorPlugin.isSchedulerPresent(relative)).thenReturn(true);
		when(event.getPlayer()).thenReturn(player);
		when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
		when(blockGeneratorPlugin.getPattern("TEST")).thenReturn(Optional.of(new GeneratorPattern(
				"NAME", 1L,
				new NamedItem("2", new ItemStack(Material.STONE)),
				new NamedItem("3", new ItemStack(Material.CRYING_OBSIDIAN)),
				new RandomCollection<>()
		)));

		// when
		listener.destroyGenerator(event);

		// then
		verify(event).setCancelled(true);
		verify(world).dropItemNaturally(eq(location), any());
		verify(brokenBlock).setType(Material.AIR);
		verify(brokenBlock).removeMetadata("generator", blockGeneratorPlugin);
		verify(blockGeneratorPlugin).cancelScheduler(relative);
	}

	@Test
	void destroyGeneratorNoSchedulerPresent() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final Block relative = mock(Block.class);
		final MetadataValue relativeMetadata = new FixedMetadataValue(blockGeneratorPlugin, "TEST");
		final Player player = mock(Player.class);
		final World world = mock(World.class);
		final Location location = new Location(world, 1, 1, 1);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getRelative(0, 1, 0)).thenReturn(relative);
		when(brokenBlock.getMetadata("generator")).thenReturn(List.of(relativeMetadata));
		when(brokenBlock.getLocation()).thenReturn(location);
		when(blockGeneratorPlugin.isSchedulerPresent(relative)).thenReturn(false);
		when(event.getPlayer()).thenReturn(player);
		when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
		when(blockGeneratorPlugin.getPattern("TEST")).thenReturn(Optional.of(new GeneratorPattern(
				"NAME", 1L,
				new NamedItem("2", new ItemStack(Material.STONE)),
				new NamedItem("3", new ItemStack(Material.CRYING_OBSIDIAN)),
				new RandomCollection<>()
		)));

		// when
		listener.destroyGenerator(event);

		// then
		verify(event).setCancelled(true);
		verify(world).dropItemNaturally(eq(location), any());
		verify(brokenBlock).setType(Material.AIR);
		verify(brokenBlock).removeMetadata("generator", blockGeneratorPlugin);
		verify(blockGeneratorPlugin, never()).cancelScheduler(any(Block.class));
	}

	@Test
	void destroyGeneratorNoWorld() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final Block relative = mock(Block.class);
		final MetadataValue relativeMetadata = new FixedMetadataValue(blockGeneratorPlugin, "TEST");
		final Player player = mock(Player.class);
		final World world = mock(World.class);
		final Location location = new Location(null, 1, 1, 1);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getRelative(0, 1, 0)).thenReturn(relative);
		when(brokenBlock.getMetadata("generator")).thenReturn(List.of(relativeMetadata));
		when(brokenBlock.getLocation()).thenReturn(location);
		when(blockGeneratorPlugin.isSchedulerPresent(relative)).thenReturn(true);
		when(event.getPlayer()).thenReturn(player);
		when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
		when(blockGeneratorPlugin.getPattern("TEST")).thenReturn(Optional.of(new GeneratorPattern(
				"NAME", 1L,
				new NamedItem("2", new ItemStack(Material.STONE)),
				new NamedItem("3", new ItemStack(Material.CRYING_OBSIDIAN)),
				new RandomCollection<>()
		)));

		// when
		listener.destroyGenerator(event);

		// then
		verify(event).setCancelled(true);
		verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
		verify(brokenBlock).setType(Material.AIR);
		verify(brokenBlock).removeMetadata("generator", blockGeneratorPlugin);
		verify(blockGeneratorPlugin).cancelScheduler(relative);
	}

	@Test
	void destroyGeneratorCreativeMode() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final Block relative = mock(Block.class);
		final MetadataValue relativeMetadata = new FixedMetadataValue(blockGeneratorPlugin, "TEST");
		final Player player = mock(Player.class);
		final World world = mock(World.class);
		final Location location = new Location(world, 1, 1, 1);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getRelative(0, 1, 0)).thenReturn(relative);
		when(brokenBlock.getMetadata("generator")).thenReturn(List.of(relativeMetadata));
		when(brokenBlock.getLocation()).thenReturn(location);
		when(blockGeneratorPlugin.isSchedulerPresent(relative)).thenReturn(true);
		when(event.getPlayer()).thenReturn(player);
		when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
		when(blockGeneratorPlugin.getPattern("TEST")).thenReturn(Optional.of(new GeneratorPattern(
				"NAME", 1L,
				new NamedItem("2", new ItemStack(Material.STONE)),
				new NamedItem("3", new ItemStack(Material.CRYING_OBSIDIAN)),
				new RandomCollection<>()
		)));

		// when
		listener.destroyGenerator(event);

		// then
		verify(event).setCancelled(true);
		verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
		verify(brokenBlock).setType(Material.AIR);
		verify(brokenBlock).removeMetadata("generator", blockGeneratorPlugin);
		verify(blockGeneratorPlugin).cancelScheduler(relative);
	}

	@Test
	void destroyGeneratorNoPattern() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final World world = mock(World.class);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getMetadata("generator")).thenReturn(Collections.emptyList());
		when(blockGeneratorPlugin.getPattern("TEST")).thenReturn(Optional.empty());

		// when
		listener.destroyGenerator(event);

		// then
		verify(event, never()).setCancelled(true);
		verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
		verify(brokenBlock, never()).setType(any(Material.class));
		verify(brokenBlock, never()).removeMetadata(anyString(), any(BlockGeneratorPlugin.class));
		verify(blockGeneratorPlugin, never()).cancelScheduler(any(Block.class));
	}

	@Test
	void destroyGeneratorNoMetadata() {
		// given
		final BlockGeneratorPlugin blockGeneratorPlugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorListener listener = new BlockGeneratorListener(blockGeneratorPlugin);
		final Block brokenBlock = mock(Block.class);
		final World world = mock(World.class);
		final BlockBreakEvent event = mock(BlockBreakEvent.class);
		when(event.getBlock()).thenReturn(brokenBlock);
		when(brokenBlock.getMetadata("generator")).thenReturn(Collections.emptyList());

		// when
		listener.destroyGenerator(event);

		// then
		verify(event, never()).setCancelled(true);
		verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
		verify(brokenBlock, never()).setType(any(Material.class));
		verify(brokenBlock, never()).removeMetadata(anyString(), any(BlockGeneratorPlugin.class));
		verify(blockGeneratorPlugin, never()).cancelScheduler(any(Block.class));
	}
}
