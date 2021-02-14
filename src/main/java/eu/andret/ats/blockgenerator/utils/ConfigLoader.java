package eu.andret.ats.blockgenerator.utils;

import eu.andret.ats.blockgenerator.BlockGeneratorPlugin;
import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import eu.andret.ats.blockgenerator.entity.NamedItem;
import lombok.NonNull;
import lombok.Value;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
public class ConfigLoader {
	private static final char PARAGRAPH = '\u00A7';
	BlockGeneratorPlugin plugin;

	public List<GeneratorPattern> loadGeneratorPatterns() {
		final List<NamedItem> items = loadEntities(plugin.getConfig().getConfigurationSection("items"));
		final List<NamedItem> blocks = loadEntities(plugin.getConfig().getConfigurationSection("blocks"));
		return loadGenerators(items, blocks);
	}

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
				.collect(Collectors.toList());
	}

	private GeneratorPattern loadGenerator(final ConfigurationSection section, final List<NamedItem> items, final List<NamedItem> blocks) {
		final String generator = section.getString("blocks.generator");
		final String generated = section.getString("blocks.generated");
		final NamedItem generatorItemStack = blocks.stream()
				.filter(i -> i.getName().equals(generator))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No '" + generator + "' in item list found. Check config."));
		final NamedItem generatedItemStack = blocks.stream()
				.filter(b -> b.getName().equals(generated))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No '" + generated + "' in block list found. Check config."));
		verifyBlock(generatorItemStack);
		verifyBlock(generatedItemStack);
		loadCrafting(section, generatorItemStack.getItemStack());
		return new GeneratorPattern(section.getName(), section.getLong("delay", 1L), generatorItemStack, generatedItemStack, loadDropList(section, items));
	}

	private void verifyBlock(final NamedItem entity) {
		if (!entity.getItemStack().getType().isBlock()) {
			throw new IllegalArgumentException(entity.getName() + " is not a block. Block required.");
		}
	}

	private void loadCrafting(final ConfigurationSection section, final ItemStack generator) {
		final List<String> shape = section.getStringList("crafting.shape");
		final Map<Character, Material> mapping = new HashMap<>();
		Optional.of(section)
				.map(s -> s.getConfigurationSection("crafting.mapping"))
				.ifPresent(configurationSection -> Optional.of(configurationSection)
						.map(s -> s.getKeys(false))
						.stream()
						.flatMap(Collection::stream)
						.forEach(key -> Optional.of(key)
								.map(configurationSection::getString)
								.map(Material::getMaterial)
								.ifPresent(material -> mapping.put(key.charAt(0), material))));
		createRecipe(generator, shape, mapping);
	}

	private RandomCollection<NamedItem> loadDropList(final ConfigurationSection section, final List<NamedItem> items) {
		final RandomCollection<NamedItem> collection = new RandomCollection<>();
		Optional.of(section)
				.map(s -> s.getConfigurationSection("drops"))
				.map(s -> s.getKeys(false))
				.stream()
				.flatMap(Collection::stream)
				.forEach(itemName -> Optional.of(section)
						.map(s -> s.getConfigurationSection("drops"))
						.map(s -> s.getConfigurationSection(itemName))
						.ifPresent(item -> items.stream()
								.filter(i -> i.getName().equals(itemName))
								.findAny()
								.map(NamedItem::getItemStack)
								.map(ItemStack::getType)
								.map(type -> new ItemStack(type, item.getInt("count")))
								.map(itemStack -> new NamedItem(itemName, itemStack))
								.ifPresent(itemStack -> collection.add(itemStack, item.getDouble("weight")))));
		return collection;
	}

	private void createRecipe(@NonNull final ItemStack target, @NonNull final List<String> shape, @NonNull final Map<Character, Material> mapping) {
		final ShapedRecipe recipe = new ShapedRecipe(createKey(target), target);
		recipe.shape(shape.toArray(new String[0]));
		mapping.forEach(recipe::setIngredient);
		plugin.getServer().addRecipe(recipe);
	}

	private NamespacedKey createKey(@NonNull final ItemStack target) {
		return new NamespacedKey(plugin, Optional.of(target)
				.map(ItemStack::getItemMeta)
				.map(ItemMeta::getDisplayName)
				.map(String::toLowerCase)
				.map(name -> name.replaceAll(PARAGRAPH + "[0-9a-f]", ""))
				.map(name -> name.replaceAll("[^a-z0-9/._-]", ""))
				.orElse(plugin.getDescription().getName()));
	}

	private List<NamedItem> loadEntities(@NonNull final ConfigurationSection section) {
		return Optional.of(section)
				.map(s -> s.getKeys(false))
				.stream()
				.flatMap(Collection::stream)
				.map(section::getConfigurationSection)
				.filter(Objects::nonNull)
				.map(s -> new NamedItem(s.getName(), loadItem(s)))
				.collect(Collectors.toList());
	}

	private ItemStack loadItem(final ConfigurationSection section) {
		return Optional.ofNullable(section)
				.map(s -> s.getString("material"))
				.map(Material::getMaterial)
				.map(ItemStack::new)
				.map(itemStack -> loadMeta(itemStack, section))
				.orElse(null);
	}

	private ItemStack loadMeta(final ItemStack itemStack, final ConfigurationSection section) {
		return Optional.of(itemStack)
				.map(ItemStack::getItemMeta)
				.map(itemMeta -> {
					Optional.of(section)
							.map(s -> s.getString("name"))
							.map(s -> ChatColor.RESET + s)
							.map(s -> s.replace('&', PARAGRAPH))
							.ifPresent(itemMeta::setDisplayName);
					itemMeta.setLore(section.getStringList("lore").stream()
							.map(s -> ChatColor.RESET + s)
							.map(s -> s.replace('&', PARAGRAPH))
							.collect(Collectors.toList()));
					itemStack.setItemMeta(itemMeta);
					return itemStack;
				})
				.orElse(itemStack);
	}
}
