/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator.utils;

import eu.andret.ats.blockgenerator.BlockGeneratorPlugin;
import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import eu.andret.ats.blockgenerator.entity.NamedItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ConfigLoader {
	private static final char PARAGRAPH = '§';
	@NotNull
	private final BlockGeneratorPlugin plugin;

	public ConfigLoader(@NotNull final BlockGeneratorPlugin plugin) {
		this.plugin = plugin;
	}

	@NotNull
	public List<GeneratorPattern> loadGeneratorPatterns() {
		final List<NamedItem> items = loadEntities(plugin.getConfig().getConfigurationSection("items"));
		final List<NamedItem> blocks = loadEntities(plugin.getConfig().getConfigurationSection("blocks"));
		return loadGenerators(items, blocks);
	}

	@NotNull
	private List<GeneratorPattern> loadGenerators(final List<NamedItem> items, final List<NamedItem> blocks) {
		final ConfigurationSection generators = plugin.getConfig().getConfigurationSection("generators");
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
										   @NotNull final List<NamedItem> items,
										   @NotNull final List<NamedItem> blocks) {
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
		loadCrafting(configurationSection, generatorNamedItem.itemStack());
		return new GeneratorPattern(configurationSection.getName(),
				configurationSection.getLong("delay", 1L),
				generatorNamedItem,
				generatedNamedItem,
				loadDropList(configurationSection, items));
	}

	private void verifyBlock(@NotNull final NamedItem namedItem) {
		if (!namedItem.itemStack().getType().isBlock()) {
			throw new IllegalArgumentException(namedItem.name() + " is not a block. Block required.");
		}
	}

	private void loadCrafting(@NotNull final ConfigurationSection configurationSection,
							  @NotNull final ItemStack generator) {
		final List<String> shape = configurationSection.getStringList("crafting.shape");
		final Map<Character, Material> mapping = new HashMap<>();
		Optional.of(configurationSection)
				.map(section -> section.getConfigurationSection("crafting.mapping"))
				.ifPresent(section -> Optional.of(section)
						.map(mappingSection -> mappingSection.getKeys(false))
						.stream()
						.flatMap(Collection::stream)
						.forEach(key -> Optional.of(key)
								.map(section::getString)
								.map(Material::getMaterial)
								.ifPresent(material -> mapping.put(key.charAt(0), material))));
		createRecipe(generator, shape, mapping);
	}

	@NotNull
	private RandomCollection<NamedItem> loadDropList(@NotNull final ConfigurationSection configurationSection,
													 @NotNull final List<NamedItem> items) {
		final RandomCollection<NamedItem> collection = new RandomCollection<>();
		Optional.of(configurationSection)
				.map(section -> section.getConfigurationSection("drops"))
				.map(section -> section.getKeys(false))
				.stream()
				.flatMap(Collection::stream)
				.forEach(itemName -> Optional.of(configurationSection)
						.map(section -> section.getConfigurationSection("drops"))
						.map(section -> section.getConfigurationSection(itemName))
						.ifPresent(item -> items.stream()
								.filter(namedItem -> namedItem.name().equals(itemName))
								.findAny()
								.map(NamedItem::itemStack)
								.map(ItemStack::getType)
								.map(type -> new ItemStack(type, item.getInt("count")))
								.map(itemStack -> new NamedItem(itemName, itemStack))
								.ifPresent(itemStack -> collection.add(itemStack, item.getDouble("weight")))));
		return collection;
	}

	private void createRecipe(@NotNull final ItemStack target, @NotNull final List<String> shape,
							  @NotNull final Map<Character, Material> mapping) {
		final ShapedRecipe recipe = new ShapedRecipe(createKey(target), target);
		recipe.shape(shape.toArray(new String[0]));
		mapping.forEach(recipe::setIngredient);
		plugin.getServer().addRecipe(recipe);
	}

	@NotNull
	private NamespacedKey createKey(@NotNull final ItemStack target) {
		return new NamespacedKey(plugin, Optional.of(target)
				.map(ItemStack::getItemMeta)
				.map(ItemMeta::getDisplayName)
				.map(String::toLowerCase)
				.map(name -> name.replaceAll(PARAGRAPH + "[\\da-f]", ""))
				.map(name -> name.replaceAll("[^a-z\\d/._-]", ""))
				.orElse(plugin.getDescription().getName()));
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
	private ItemStack loadItem(final ConfigurationSection configurationSection) {
		return Optional.ofNullable(configurationSection)
				.map(section -> section.getString("material"))
				.map(Material::getMaterial)
				.map(ItemStack::new)
				.map(itemStack -> loadMeta(itemStack, configurationSection))
				.orElseThrow();
	}

	@NotNull
	private ItemStack loadMeta(final ItemStack itemStack, final ConfigurationSection configurationSection) {
		return Optional.of(itemStack)
				.map(ItemStack::getItemMeta)
				.map(itemMeta -> {
					Optional.of(configurationSection)
							.map(section -> section.getString("name"))
							.map(text -> ChatColor.RESET + text)
							.map(text -> ChatColor.translateAlternateColorCodes('&', text))
							.ifPresent(itemMeta::setDisplayName);
					itemMeta.setLore(configurationSection.getStringList("lore").stream()
							.map(text -> ChatColor.RESET + text)
							.map(text -> ChatColor.translateAlternateColorCodes('&', text))
							.toList());
					itemStack.setItemMeta(itemMeta);
					return itemStack;
				})
				.orElse(itemStack);
	}
}
