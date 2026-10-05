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
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConfigLoaderTest extends PluginTest {
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
		assertThat(meta.itemName()).isEqualTo(Component.text("Generator", NamedTextColor.RED));
		assertThat(meta.lore()).containsExactly(
				Component.text("first", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
				Component.text("second", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, true));
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

	@Test
	void rejectGeneratorWithGravity() throws InvalidConfigurationException {
		// given
		loadConfig("""
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
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("sand falls down. A generator block cannot be affected by gravity.");
	}

	@Test
	void rejectNonBlock() throws InvalidConfigurationException {
		// given
		loadConfig("""
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
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("apple is not a block. Block required.");
	}

	@Test
	void rejectMissingBlock() throws InvalidConfigurationException {
		// given
		loadConfig("""
				generators:
				  missing:
				    blocks:
				      generator: sponge
				      generated: stone
				blocks:
				  stone:
				    material: STONE
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("No 'sponge' in item list found. Check config.");
	}

	@Test
	void rejectUnknownExplosionMode() throws InvalidConfigurationException {
		// given
		loadConfig("""
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
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Unknown 'explosion.generator' value 'explode' in generator 'boom'. "
						+ "Use one of: prevent, drop, remove.");
	}

	@Test
	void rejectCraftingWithoutShape() throws InvalidConfigurationException {
		// given
		loadConfig("""
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
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Generator 'shapeless' has a crafting section without a shape.");
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
		assertThat(drops).anySatisfy(drop -> {
			assertThat(drop.getType()).isEqualTo(Material.BREAD);
			assertThat(drop.getAmount()).isEqualTo(3);
			assertThat(drop.getItemMeta().itemName()).isEqualTo(Component.text("Special bread"));
		});
		assertThat(drops).anySatisfy(drop -> {
			assertThat(drop.getType()).isEqualTo(Material.APPLE);
			assertThat(drop.getAmount()).isEqualTo(1);
		});
	}

	@Test
	void rejectUnknownMaterial() throws InvalidConfigurationException {
		// given
		loadConfig("""
				blocks:
				  sponge:
				    material: SPONG
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Unknown material 'SPONG' at 'blocks.sponge.material'.");
	}

	@Test
	void rejectMissingMaterial() throws InvalidConfigurationException {
		// given
		loadConfig("""
				items:
				  bread:
				    name: 'Bread'
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Missing material at 'items.bread.material'.");
	}

	@Test
	void rejectUnknownCraftingMaterial() throws InvalidConfigurationException {
		// given
		loadConfig("""
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
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Unknown material 'STONEE' at 'generators.typo.crafting.mapping.S'.");
	}

	@Test
	void rejectUnknownDropItem() throws InvalidConfigurationException {
		// given
		loadConfig("""
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
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Unknown item 'bread' at 'generators.missing.drops.bread'. Drops have to be defined in 'items'.");
	}

	@Test
	void rejectDropWithoutWeight() throws InvalidConfigurationException {
		// given
		loadConfig("""
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
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Missing or not positive weight at 'generators.weightless.drops.bread.weight'.");
	}

	@Test
	void rejectDropWithoutPositiveCount() throws InvalidConfigurationException {
		// given
		loadConfig("""
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
				""");

		// when
		final ThrowableAssert.ThrowingCallable callable = () -> new ConfigLoader(plugin).loadGeneratorPatterns(config);

		// then
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Not positive count at 'generators.empty.drops.bread.count'.");
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
