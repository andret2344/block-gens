package eu.andret.blockgens;

import eu.andret.blockgens.config.GeneratorPattern;
import eu.andret.blockgens.helper.PluginTest;
import org.assertj.core.api.ThrowableAssert;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BlockGensPluginTest extends PluginTest {
	@Test
	void reloadReplacesPatternsAndRecipes() throws IOException, InvalidConfigurationException {
		// given
		writeConfig("""
				generators:
				  other:
				    blocks:
				      generator: sponge
				      generated: stone
				    crafting:
				      shape:
				        - 'SS'
				      mapping:
				        'S': STONE
				blocks:
				  sponge:
				    material: SPONGE
				  stone:
				    material: STONE
				""");

		// when
		final int result = plugin.reload();

		// then
		assertThat(result).isEqualTo(1);
		assertThat(plugin.getPatternList()).extracting(GeneratorPattern::name).containsExactly("other");
		assertThat(server.getRecipe(new NamespacedKey(plugin, "sponge"))).isNull();
		assertThat(server.getRecipe(new NamespacedKey(plugin, "dirt"))).isNull();
		assertThat(server.getRecipe(new NamespacedKey(plugin, "other"))).isNotNull();
	}

	@Test
	void reloadStartsGeneratorsOfNewPatterns() throws IOException, InvalidConfigurationException {
		// given
		final Block block = world.getBlockAt(0, 5, 0);
		block.setType(Material.SPONGE);
		plugin.getGeneratorStore().save(block, "added");
		writeConfig("""
				generators:
				  added:
				    blocks:
				      generator: sponge
				      generated: stone
				blocks:
				  sponge:
				    material: SPONGE
				  stone:
				    material: STONE
				""");

		// when
		plugin.reload();

		// then
		assertThat(block.getRelative(0, 1, 0).getType()).isEqualTo(Material.STONE);
	}

	@Test
	void reloadInvalidYamlKeepsEverything() throws IOException {
		// given
		writeConfig("generators: [");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> plugin.reload();

		// then
		assertThatThrownBy(callable).isInstanceOf(InvalidConfigurationException.class);
		assertThat(plugin.getPatternList()).extracting(GeneratorPattern::name).containsExactlyInAnyOrder("sponge", "dirt");
		assertThat(server.getRecipe(new NamespacedKey(plugin, "sponge"))).isNotNull();
	}

	@Test
	void reloadInvalidContentKeepsEverything() throws IOException {
		// given
		writeConfig("""
				generators:
				  broken:
				    blocks:
				      generator: missing
				      generated: missing
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> plugin.reload();

		// then
		assertThatThrownBy(callable).isInstanceOf(IllegalArgumentException.class);
		assertThat(plugin.getPatternList()).extracting(GeneratorPattern::name).containsExactlyInAnyOrder("sponge", "dirt");
		assertThat(server.getRecipe(new NamespacedKey(plugin, "sponge"))).isNotNull();
	}

	@Test
	void disableRemovesRecipes() {
		// when
		server.getPluginManager().disablePlugin(plugin);

		// then
		assertThat(server.getRecipe(new NamespacedKey(plugin, "sponge"))).isNull();
		assertThat(server.getRecipe(new NamespacedKey(plugin, "dirt"))).isNull();
	}

	private void writeConfig(@NotNull final String yaml) throws IOException {
		Files.writeString(new File(plugin.getDataFolder(), "config.yml").toPath(), yaml);
	}
}
