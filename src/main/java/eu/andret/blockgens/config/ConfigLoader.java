package eu.andret.blockgens.config;

import eu.andret.blockgens.BlockGensPlugin;
import eu.andret.blockgens.util.RandomCollection;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfigLoader {
	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
	@NotNull
	private final BlockGensPlugin plugin;

	public ConfigLoader(@NotNull final BlockGensPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Reads the generators from the config. Builds their recipes but does not register them, so nothing changes on
	 * the server when the config turns out to be invalid.
	 *
	 * @throws IllegalArgumentException when the config is invalid, with a message for the server admin
	 */
	@NotNull
	public List<GeneratorPattern> loadGeneratorPatterns(@NotNull final ConfigurationSection config) {
		final List<NamedItem> items = loadEntities(config.getConfigurationSection("items"));
		final List<NamedItem> blocks = loadEntities(config.getConfigurationSection("blocks"));
		return loadGenerators(config, items, blocks);
	}

	@NotNull
	private List<GeneratorPattern> loadGenerators(@NotNull final ConfigurationSection config,
			@NotNull final List<NamedItem> items, @NotNull final List<NamedItem> blocks) {
		final ConfigurationSection generators = config.getConfigurationSection("generators");
		if (generators == null) {
			return Collections.emptyList();
		}
		return Optional.of(generators)
				.map(section -> section.getKeys(false))
				.stream()
				.flatMap(Collection::stream)
				.map(generators::getConfigurationSection)
				.filter(Objects::nonNull)
				.map(section -> loadGenerator(section, items, blocks))
				.toList();
	}

	@NotNull
	private GeneratorPattern loadGenerator(@NotNull final ConfigurationSection configurationSection,
			@NotNull final List<NamedItem> items, @NotNull final List<NamedItem> blocks) {
		final String generator = configurationSection.getString("blocks.generator");
		final String generated = configurationSection.getString("blocks.generated");
		final NamedItem generatorNamedItem = blocks.stream()
				.filter(namedItem -> namedItem.name().equals(generator))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No '" + generator
						+ "' in item list found. Check config."));
		final NamedItem generatedNamedItem = blocks.stream()
				.filter(namedItem -> namedItem.name().equals(generated))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No '" + generated
						+ "' in block list found. Check config."));
		verifyBlock(generatorNamedItem);
		verifyBlock(generatedNamedItem);
		verifyNoGravity(generatorNamedItem);
		final NamedItem generatorItem = tagGenerator(generatorNamedItem, configurationSection.getName());
		return new GeneratorPattern(configurationSection.getName(),
				configurationSection.getLong("delay", 1L),
				generatorItem,
				generatedNamedItem,
				loadDropList(configurationSection, items),
				loadExplosionMode(configurationSection, "explosion.generator", ExplosionMode.DROP),
				loadExplosionMode(configurationSection, "explosion.generated", ExplosionMode.REMOVE),
				loadRecipe(configurationSection, generatorItem.itemStack()));
	}

	// The tag, not the look of the item, makes it a generator, so a renamed item or a changed config keeps it working.
	// A copy, as one block may be the generator of more than one pattern.
	@NotNull
	private NamedItem tagGenerator(@NotNull final NamedItem block, @NotNull final String patternName) {
		final ItemStack itemStack = block.itemStack().asQuantity(block.itemStack().getAmount());
		itemStack.editPersistentDataContainer(container ->
				container.set(plugin.getGeneratorKey(), PersistentDataType.STRING, patternName));
		return new NamedItem(block.name(), itemStack);
	}

	private void verifyBlock(@NotNull final NamedItem namedItem) {
		if (!namedItem.itemStack().getType().isBlock()) {
			throw new IllegalArgumentException(namedItem.name() + " is not a block. Block required.");
		}
	}

	// A falling generator would leave its stored position and turn into a regular block where it lands
	private void verifyNoGravity(@NotNull final NamedItem namedItem) {
		if (namedItem.itemStack().getType().hasGravity()) {
			throw new IllegalArgumentException(namedItem.name()
					+ " falls down. A generator block cannot be affected by gravity.");
		}
	}

	@NotNull
	private ExplosionMode loadExplosionMode(@NotNull final ConfigurationSection configurationSection,
			@NotNull final String path, @NotNull final ExplosionMode defaultMode) {
		final String value = configurationSection.getString(path);
		if (value == null) {
			return defaultMode;
		}
		try {
			return ExplosionMode.valueOf(value.toUpperCase(Locale.ROOT));
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException("Unknown '" + path + "' value '" + value + "' in generator '"
					+ configurationSection.getName() + "'. Use one of: " + Arrays.stream(ExplosionMode.values())
					.map(mode -> mode.name().toLowerCase(Locale.ROOT))
					.collect(Collectors.joining(", ")) + ".", e);
		}
	}

	@Nullable
	private ShapedRecipe loadRecipe(@NotNull final ConfigurationSection configurationSection,
			@NotNull final ItemStack generator) {
		if (!configurationSection.isConfigurationSection("crafting")) {
			return null;
		}
		final List<String> shape = configurationSection.getStringList("crafting.shape");
		if (shape.isEmpty()) {
			throw new IllegalArgumentException("Generator '" + configurationSection.getName()
					+ "' has a crafting section without a shape.");
		}
		final ShapedRecipe recipe = new ShapedRecipe(createKey(configurationSection.getName()), generator);
		recipe.shape(shape.toArray(new String[0]));
		final ConfigurationSection mapping = configurationSection.getConfigurationSection("crafting.mapping");
		if (mapping != null) {
			mapping.getKeys(false).forEach(key -> recipe.setIngredient(key.charAt(0),
					loadMaterial(mapping.getString(key), mapping.getCurrentPath() + "." + key)));
		}
		return recipe;
	}

	@NotNull
	private RandomCollection<NamedItem> loadDropList(@NotNull final ConfigurationSection configurationSection,
			@NotNull final List<NamedItem> items) {
		final RandomCollection<NamedItem> collection = new RandomCollection<>();
		final ConfigurationSection drops = configurationSection.getConfigurationSection("drops");
		if (drops == null) {
			return collection;
		}
		drops.getKeys(false).forEach(itemName -> {
			final String path = drops.getCurrentPath() + "." + itemName;
			final NamedItem namedItem = items.stream()
					.filter(item -> item.name().equals(itemName))
					.findAny()
					.orElseThrow(() -> new IllegalArgumentException("Unknown item '" + itemName + "' at '" + path
							+ "'. Drops have to be defined in 'items'."));
			final ConfigurationSection drop = drops.getConfigurationSection(itemName);
			if (drop == null || drop.getDouble("weight") <= 0) {
				throw new IllegalArgumentException("Missing or not positive weight at '" + path + ".weight'.");
			}
			final double weight = drop.getDouble("weight");
			final int count = drop.getInt("count", 1);
			if (count < 1) {
				throw new IllegalArgumentException("Not positive count at '" + path + ".count'.");
			}
			// A copy keeps the name and lore of the item
			collection.add(new NamedItem(itemName, namedItem.itemStack().asQuantity(count)), weight);
		});
		return collection;
	}

	// Generator names are unique config keys, so they make unique recipe keys
	@NotNull
	private NamespacedKey createKey(@NotNull final String generatorName) {
		return new NamespacedKey(plugin, generatorName.toLowerCase(Locale.ROOT).replaceAll("[^a-z\\d/._-]", "_"));
	}

	@NotNull
	private List<NamedItem> loadEntities(@Nullable final ConfigurationSection configurationSection) {
		if (configurationSection == null) {
			return Collections.emptyList();
		}
		return Optional.of(configurationSection)
				.map(section -> section.getKeys(false))
				.stream()
				.flatMap(Collection::stream)
				.map(configurationSection::getConfigurationSection)
				.filter(Objects::nonNull)
				.map(section -> new NamedItem(section.getName(), loadItem(section)))
				.toList();
	}

	@NotNull
	private ItemStack loadItem(@NotNull final ConfigurationSection configurationSection) {
		final Material material = loadMaterial(configurationSection.getString("material"),
				configurationSection.getCurrentPath() + ".material");
		return loadMeta(new ItemStack(material), configurationSection);
	}

	@NotNull
	private Material loadMaterial(@Nullable final String name, @NotNull final String path) {
		if (name == null) {
			throw new IllegalArgumentException("Missing material at '" + path + "'.");
		}
		final Material material = Material.getMaterial(name);
		if (material == null) {
			throw new IllegalArgumentException("Unknown material '" + name + "' at '" + path + "'.");
		}
		return material;
	}

	@NotNull
	private ItemStack loadMeta(final ItemStack itemStack, final ConfigurationSection configurationSection) {
		return Optional.of(itemStack)
				.map(ItemStack::getItemMeta)
				.map(itemMeta -> {
					// The custom name, as some items (potions, tipped arrows) ignore the item name.
					// Not italic, which a custom name is by default.
					Optional.of(configurationSection)
							.map(section -> section.getString("name"))
							.map(MINI_MESSAGE::deserialize)
							.map(name -> name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
							.ifPresent(itemMeta::customName);
					itemMeta.lore(configurationSection.getStringList("lore").stream()
							.map(MINI_MESSAGE::deserialize)
							.map(text -> text.colorIfAbsent(NamedTextColor.WHITE)
									.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
							.toList());
					itemStack.setItemMeta(itemMeta);
					return itemStack;
				})
				.orElse(itemStack);
	}
}
