/*
 * Copyright Andret Tools System (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator;

import eu.andret.ats.blockgenerator.entity.NamedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlockGeneratorCommandTest {
	@Test
	void correctGenerator() {
		// given
		final Player sender = mock(Player.class);
		final BlockGeneratorPlugin plugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorCommand blockGeneratorCommand = new BlockGeneratorCommand(sender, plugin);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(sender.getInventory()).thenReturn(inventory);

		// when
		final ItemStack itemStack = new ItemStack(Material.SPONGE);
		final String result = blockGeneratorCommand.generator(new NamedItem("test", itemStack));

		// then
		assertEquals("Generator block: \"test\"", result);
		verify(inventory, times(1)).addItem(itemStack);
	}

	@Test
	void incorrectGenerator() {
		// given
		final Player sender = mock(Player.class);
		final BlockGeneratorPlugin plugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorCommand blockGeneratorCommand = new BlockGeneratorCommand(sender, plugin);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(sender.getInventory()).thenReturn(inventory);

		// when
		final String result = blockGeneratorCommand.generator("test");

		// then
		assertEquals("No generator block found: \"test\"", result);
		verify(inventory, times(0)).addItem(any(ItemStack.class));
	}

	@Test
	void correctGenerated() {
		// given
		final Player sender = mock(Player.class);
		final BlockGeneratorPlugin plugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorCommand blockGeneratorCommand = new BlockGeneratorCommand(sender, plugin);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(sender.getInventory()).thenReturn(inventory);

		// when
		final ItemStack itemStack = new ItemStack(Material.SPONGE);
		final String result = blockGeneratorCommand.generated(new NamedItem("test", itemStack));

		// then
		assertEquals("Generated block: \"test\"", result);
		verify(inventory, times(1)).addItem(itemStack);
	}

	@Test
	void incorrectGenerated() {
		// given
		final Player sender = mock(Player.class);
		final BlockGeneratorPlugin plugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorCommand blockGeneratorCommand = new BlockGeneratorCommand(sender, plugin);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(sender.getInventory()).thenReturn(inventory);

		// when
		final String result = blockGeneratorCommand.generated("test");

		// then
		assertEquals("No generated block found: \"test\"", result);
		verify(inventory, times(0)).addItem(any(ItemStack.class));
	}

	@Test
	void correctItem() {
		// given
		final Player sender = mock(Player.class);
		final BlockGeneratorPlugin plugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorCommand blockGeneratorCommand = new BlockGeneratorCommand(sender, plugin);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(sender.getInventory()).thenReturn(inventory);

		// when
		final ItemStack itemStack = new ItemStack(Material.SPONGE);
		final String result = blockGeneratorCommand.item(new NamedItem("test", itemStack));

		// then
		assertEquals("Drop item: \"test\"", result);
		verify(inventory, times(1)).addItem(itemStack);
	}

	@Test
	void incorrectItem() {
		// given
		final Player sender = mock(Player.class);
		final BlockGeneratorPlugin plugin = mock(BlockGeneratorPlugin.class);
		final BlockGeneratorCommand blockGeneratorCommand = new BlockGeneratorCommand(sender, plugin);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(sender.getInventory()).thenReturn(inventory);

		// when
		final String result = blockGeneratorCommand.item("test");

		// then
		assertEquals("No drop item found: \"test\"", result);
		verify(inventory, times(0)).addItem(any(ItemStack.class));
	}
}
