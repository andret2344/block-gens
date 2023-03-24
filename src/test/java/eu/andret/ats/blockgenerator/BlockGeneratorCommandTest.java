/*
 * Copyright Andret Tools System (c) 2018-2023. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator;

import eu.andret.ats.blockgenerator.entity.NamedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlockGeneratorCommandTest {
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
		assertThat(result).isEqualTo("Generator block: \"test\"");
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
		assertThat(result).isEqualTo("No generator block found: \"test\"");
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
		assertThat(result).isEqualTo("Generated block: \"test\"");
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
		assertThat(result).isEqualTo("No generated block found: \"test\"");
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
		assertThat(result).isEqualTo("Drop item: \"test\"");
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
		assertThat(result).isEqualTo("No drop item found: \"test\"");
		verify(inventory, times(0)).addItem(any(ItemStack.class));
	}
}
