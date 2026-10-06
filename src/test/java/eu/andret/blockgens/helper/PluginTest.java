package eu.andret.blockgens.helper;

import eu.andret.blockgens.BlockGensPlugin;
import eu.andret.blockgens.config.GeneratorPattern;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

/**
 * Starts a mocked server with the plugin loaded from the shipped {@code config.yml} before every test.
 */
public abstract class PluginTest {
	protected ServerMock server;
	protected BlockGensPlugin plugin;
	protected WorldMock world;

	@BeforeEach
	void setUpServer() {
		server = MockBukkit.mock();
		plugin = MockBukkit.load(BlockGensPlugin.class);
		world = server.addSimpleWorld("world");
	}

	@AfterEach
	void tearDownServer() {
		MockBukkit.unmock();
	}

	@NotNull
	protected GeneratorPattern pattern(@NotNull final String name) {
		return plugin.getPattern(name).orElseThrow();
	}

	/**
	 * Puts a generator of the given pattern at the given position, with its generated block above it.
	 */
	@NotNull
	protected Block placeGenerator(@NotNull final GeneratorPattern pattern, final int x, final int y, final int z) {
		final Block block = world.getBlockAt(x, y, z);
		block.setType(pattern.generatorItem().itemStack().getType());
		block.getRelative(0, 1, 0).setType(pattern.generatedItem().itemStack().getType());
		plugin.getGeneratorStore().save(block, pattern.name());
		return block;
	}

	@NotNull
	protected List<ItemStack> droppedItems() {
		return world.getEntitiesByClass(Item.class)
				.stream()
				.map(Item::getItemStack)
				.toList();
	}
}
