package eu.andret.blockgens.storage;

import eu.andret.blockgens.helper.PluginTest;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratorStoreTest extends PluginTest {
	@Test
	void saveUnderPositionKey() {
		// given
		final Block block = world.getBlockAt(-1, 5, -3);

		// when
		plugin.getGeneratorStore().save(block, "sponge");

		// then
		assertThat(block.getChunk().getPersistentDataContainer()
				.get(new NamespacedKey(plugin, "generator/-1/5/-3"), PersistentDataType.STRING))
				.isEqualTo("sponge");
	}

	@Test
	void findStored() {
		// given
		final Block block = world.getBlockAt(-1, 5, -3);
		plugin.getGeneratorStore().save(block, "sponge");

		// when
		final Optional<String> result = plugin.getGeneratorStore().find(world.getBlockAt(-1, 5, -3));

		// then
		assertThat(result).contains("sponge");
	}

	@Test
	void findNotStored() {
		// when
		final Optional<String> result = plugin.getGeneratorStore().find(world.getBlockAt(-1, 5, -3));

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void remove() {
		// given
		final Block block = world.getBlockAt(-1, 5, -3);
		plugin.getGeneratorStore().save(block, "sponge");

		// when
		plugin.getGeneratorStore().remove(block);

		// then
		assertThat(plugin.getGeneratorStore().find(block)).isEmpty();
	}

	@Test
	void findAllSkipsForeignEntries() {
		// given
		final Block first = world.getBlockAt(-1, 5, -3);
		final Block second = world.getBlockAt(-2, 70, -4);
		plugin.getGeneratorStore().save(first, "sponge");
		plugin.getGeneratorStore().save(second, "dirt");
		final PersistentDataContainer container = first.getChunk().getPersistentDataContainer();
		container.set(new NamespacedKey("other", "generator/3/1/3"), PersistentDataType.STRING, "other plugin");
		container.set(new NamespacedKey(plugin, "something/4/1/3"), PersistentDataType.STRING, "other key");
		container.set(new NamespacedKey(plugin, "generator/x/1/3"), PersistentDataType.STRING, "malformed");
		container.set(new NamespacedKey(plugin, "generator/5/1"), PersistentDataType.STRING, "too short");
		container.set(new NamespacedKey(plugin, "generator/6/1/3"), PersistentDataType.INTEGER, 6);

		// when
		final Map<Block, String> result = plugin.getGeneratorStore().findAll(first.getChunk());

		// then
		assertThat(result).containsOnly(Map.entry(first, "sponge"), Map.entry(second, "dirt"));
	}
}
