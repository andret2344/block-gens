/*
 * Copyright Andret Tools System (c) 2018-2023. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator;

import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import eu.andret.ats.blockgenerator.entity.NamedItem;
import eu.andret.ats.blockgenerator.utils.RandomCollection;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
		verify(relative, times(1)).setType(Material.OBSIDIAN);
		verify(block, times(1)).setMetadata(eq("generator"), argThat(x ->
				Objects.equals(x.getOwningPlugin(), blockGeneratorPlugin) && x.asString().equals("test")));
	}
}
