package eu.andret.blockgens.config;

import eu.andret.blockgens.helper.PluginTest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.assertj.core.api.ThrowableAssert;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class ConfigLoaderTest extends PluginTest {
	private final YamlConfiguration config = new YamlConfiguration();

	@Test
	void loadShippedConfig() {
		// when
		final List<GeneratorPattern> result = plugin.getPatternList();

		// then
		assertThat(result).extracting(GeneratorPattern::name).containsExactlyInAnyOrder("sponge", "dirt");
		final GeneratorPattern sponge = pattern("sponge");
		assertThat(sponge.delay()).isEqualTo(1);
		assertThat(sponge.generatorItem().itemStack().getType()).isEqualTo(Material.SPONGE);
		assertThat(sponge.generatedItem().itemStack().getType()).isEqualTo(Material.STONE);
		assertThat(sponge.dropItems().getItems()).extracting(NamedItem::name).containsExactlyInAnyOrder("bread", "apple");
		assertThat(sponge.generatorExplosion()).isEqualTo(ExplosionMode.DROP);
		assertThat(sponge.generatedExplosion()).isEqualTo(ExplosionMode.REMOVE);
		assertThat(server.getRecipe(new NamespacedKey(plugin, "sponge"))).isNotNull();
		assertThat(server.getRecipe(new NamespacedKey(plugin, "dirt"))).isNotNull();
	}

	@Test
	void loadNameAndLore() throws InvalidConfigurationException {
		// given
		loadConfig("""
				generators:
				  named:
				    blocks:
				      generator: sponge
				      generated: stone
				blocks:
				  sponge:
				    material: SPONGE
				    name: '<red>Generator'
				    lore:
				      - 'first'
				      - '<italic><green>second'
				  stone:
				    material: STONE
				""");

		// when
		final List<GeneratorPattern> result = new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		final ItemMeta meta = result.getFirst().generatorItem().itemStack().getItemMeta();
		assertThat(meta.customName())
				.isEqualTo(Component.text("Generator", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		assertThat(meta.lore()).containsExactly(
				Component.text("first", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
				Component.text("second", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, true));
	}

	@Test
	void tagGeneratorItemsWithTheirPattern() throws InvalidConfigurationException {
		// given
		loadConfig("""
				generators:
				  first:
				    blocks:
				      generator: sponge
				      generated: stone
				  second:
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
		final List<GeneratorPattern> result = new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThat(result).extracting(pattern -> pattern.generatorItem().itemStack().getPersistentDataContainer()
				.get(plugin.getGeneratorKey(), PersistentDataType.STRING)).containsExactly("first", "second");
		assertThat(result).extracting(pattern -> pattern.generatedItem().itemStack().getPersistentDataContainer()
				.has(plugin.getGeneratorKey())).containsOnly(false);
	}

	@Test
	void loadDefaults() throws InvalidConfigurationException {
		// given
		loadConfig("""
				generators:
				  plain:
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
		final List<GeneratorPattern> result = new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		final GeneratorPattern plain = result.getFirst();
		assertThat(plain.delay()).isEqualTo(1);
		assertThat(plain.dropItems().getItems()).isEmpty();
		assertThat(plain.generatorExplosion()).isEqualTo(ExplosionMode.DROP);
		assertThat(plain.generatedExplosion()).isEqualTo(ExplosionMode.REMOVE);
		assertThat(plain.recipe()).isNull();
	}

	@Test
	void loadExplosionModesIgnoringCase() throws InvalidConfigurationException {
		// given
		loadConfig("""
				generators:
				  modes:
				    blocks:
				      generator: sponge
				      generated: stone
				    explosion:
				      generator: PREVENT
				      generated: Drop
				blocks:
				  sponge:
				    material: SPONGE
				  stone:
				    material: STONE
				""");

		// when
		final List<GeneratorPattern> result = new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThat(result.getFirst().generatorExplosion()).isEqualTo(ExplosionMode.PREVENT);
		assertThat(result.getFirst().generatedExplosion()).isEqualTo(ExplosionMode.DROP);
	}

	@Test
	void loadCraftingUnderSanitizedKey() throws InvalidConfigurationException {
		// given
		loadConfig("""
				generators:
				  My Gen:
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
		final List<GeneratorPattern> result = new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThat(result.getFirst().recipe()).isNotNull()
				.extracting(ShapedRecipe::getKey)
				.isEqualTo(new NamespacedKey(plugin, "my_gen"));
		assertThat(server.getRecipe(new NamespacedKey(plugin, "my_gen"))).isNull();
	}

	@ParameterizedTest
	@MethodSource("invalidConfigs")
	void rejectInvalidConfig(@NotNull final String yaml, @NotNull final String message)
			throws InvalidConfigurationException {
		// given
		loadConfig(yaml);

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(message);
	}

	@NotNull
	static Stream<Arguments> invalidConfigs() {
		return Stream.of(
				argumentSet("generator with gravity", """
								generators:
								  falling:
								    blocks:
								      generator: sand
								      generated: stone
								blocks:
								  sand:
								    material: SAND
								  stone:
								    material: STONE
								""",
						"sand falls down. A generator block cannot be affected by gravity."),
				argumentSet("non block", """
								generators:
								  item:
								    blocks:
								      generator: sponge
								      generated: apple
								blocks:
								  sponge:
								    material: SPONGE
								  apple:
								    material: APPLE
								""",
						"apple is not a block. Block required."),
				argumentSet("missing block", """
								generators:
								  missing:
								    blocks:
								      generator: sponge
								      generated: stone
								blocks:
								  stone:
								    material: STONE
								""",
						"No 'sponge' in item list found. Check config."),
				argumentSet("unknown explosion mode", """
								generators:
								  boom:
								    blocks:
								      generator: sponge
								      generated: stone
								    explosion:
								      generator: explode
								blocks:
								  sponge:
								    material: SPONGE
								  stone:
								    material: STONE
								""",
						"Unknown 'explosion.generator' value 'explode' in generator 'boom'. "
								+ "Use one of: prevent, drop, remove."),
				argumentSet("crafting without shape", """
								generators:
								  shapeless:
								    blocks:
								      generator: sponge
								      generated: stone
								    crafting:
								      mapping:
								        'S': STONE
								blocks:
								  sponge:
								    material: SPONGE
								  stone:
								    material: STONE
								""",
						"Generator 'shapeless' has a crafting section without a shape."),
				argumentSet("unknown material", """
								blocks:
								  sponge:
								    material: SPONG
								""",
						"Unknown material 'SPONG' at 'blocks.sponge.material'."),
				argumentSet("missing material", """
								items:
								  bread:
								    name: 'Bread'
								""",
						"Missing material at 'items.bread.material'."),
				argumentSet("unknown crafting material", """
								generators:
								  typo:
								    blocks:
								      generator: sponge
								      generated: stone
								    crafting:
								      shape:
								        - 'SS'
								      mapping:
								        'S': STONEE
								blocks:
								  sponge:
								    material: SPONGE
								  stone:
								    material: STONE
								""",
						"Unknown material 'STONEE' at 'generators.typo.crafting.mapping.S'."),
				argumentSet("unknown drop item", """
								generators:
								  missing:
								    blocks:
								      generator: sponge
								      generated: stone
								    drops:
								      bread:
								        weight: 1
								blocks:
								  sponge:
								    material: SPONGE
								  stone:
								    material: STONE
								items:
								  apple:
								    material: APPLE
								""",
						"Unknown item 'bread' at 'generators.missing.drops.bread'. Drops have to be defined in 'items'."),
				argumentSet("drop without weight", """
								generators:
								  weightless:
								    blocks:
								      generator: sponge
								      generated: stone
								    drops:
								      bread:
								        count: 2
								blocks:
								  sponge:
								    material: SPONGE
								  stone:
								    material: STONE
								items:
								  bread:
								    material: BREAD
								""",
						"Missing or not positive weight at 'generators.weightless.drops.bread.weight'."),
				argumentSet("drop without positive count", """
								generators:
								  empty:
								    blocks:
								      generator: sponge
								      generated: stone
								    drops:
								      bread:
								        weight: 1
								        count: 0
								blocks:
								  sponge:
								    material: SPONGE
								  stone:
								    material: STONE
								items:
								  bread:
								    material: BREAD
								""",
						"Not positive count at 'generators.empty.drops.bread.count'."));
	}

	@Test
	void loadDropsWithNameAndCount() throws InvalidConfigurationException {
		// given
		loadConfig("""
				generators:
				  dropping:
				    blocks:
				      generator: sponge
				      generated: stone
				    drops:
				      bread:
				        weight: 1
				        count: 3
				      apple:
				        weight: 1
				blocks:
				  sponge:
				    material: SPONGE
				  stone:
				    material: STONE
				items:
				  bread:
				    material: BREAD
				    name: 'Special bread'
				  apple:
				    material: APPLE
				""");

		// when
		final List<GeneratorPattern> result = new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		final List<ItemStack> drops = result.getFirst().dropItems().getItems().stream().map(NamedItem::itemStack).toList();
		assertThat(drops)
				.anySatisfy(drop -> {
					assertThat(drop.getType()).isEqualTo(Material.BREAD);
					assertThat(drop.getAmount()).isEqualTo(3);
					assertThat(drop.getItemMeta().customName())
							.isEqualTo(Component.text("Special bread").decoration(TextDecoration.ITALIC, false));
				})
				.anySatisfy(drop -> {
					assertThat(drop.getType()).isEqualTo(Material.APPLE);
					assertThat(drop.getAmount()).isEqualTo(1);
				});
	}

	@Test
	void loadWithoutGenerators() throws InvalidConfigurationException {
		// given
		loadConfig("""
				blocks:
				  stone:
				    material: STONE
				""");

		// when
		final List<GeneratorPattern> result = new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThat(result).isEmpty();
	}

	private void loadConfig(@NotNull final String yaml) throws InvalidConfigurationException {
		config.loadFromString(yaml);
	}
}
