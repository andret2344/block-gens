package eu.andret.ats.blockgenerator;

import eu.andret.arguments.AnnotatedCommand;
import eu.andret.arguments.CommandManager;
import eu.andret.arguments.api.annotation.Fallback;
import eu.andret.ats.blockgenerator.entity.Generator;
import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Log
public class BlockGenerator extends JavaPlugin {
	private static final char PARAGRAPH = '\u00A7';

	private final File file = new File(getDataFolder(), "generators.json");

	@Getter
	private final List<GeneratorPattern> patterns = new ArrayList<>();

	@Getter
	private final List<Generator> generators = new ArrayList<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		setUpListeners();
		final List<?> sections = getConfig().getList("generators");
		for (final Object section : sections) {
			final ConfigurationSection current = getConfig().createSection("current", (Map<?, ?>) section);
			final ItemStack generator = createBlock(current.getConfigurationSection("items.generator"));
			final ItemStack generated = createBlock(current.getConfigurationSection("items.generated"));
			patterns.add(new GeneratorPattern(current.getString("name"), current.getLong("delay", 1L), generator, generated));
			final List<String> shape = current.getStringList("crafting.shape");
			final Map<Character, Material> mapping = new HashMap<>();
			final ConfigurationSection configurationSection = current.getConfigurationSection("crafting.mapping");
			Objects.requireNonNull(configurationSection).getKeys(false).forEach(key -> Optional.of(key)
					.map(configurationSection::getString)
					.map(Material::getMaterial)
					.ifPresent(material -> mapping.put(key.charAt(0), material)));
			createRecipe(generator, shape, mapping);
		}
		loadGenerators();
		setUpCommand();
	}

	@Override
	public void onDisable() {
		saveGenerators();
	}

	private void setUpCommand() {
		final AnnotatedCommand command = CommandManager.registerCommand(BlockGeneratorCommand.class, this);
		command.addTypeCompleter(GeneratorPattern.class, () -> patterns.stream().map(GeneratorPattern::getName).collect(Collectors.toSet()));
		command.addArgumentMapper("patternName", GeneratorPattern.class, name -> patterns.stream().filter(p -> p.getName().equals(name)).findAny().orElse(null), Fallback.ON_NULL);
	}

	@NonNull
	private ItemStack createBlock(final ConfigurationSection section) {
		return Optional.of(section)
				.map(s -> s.getString("material"))
				.map(Material::getMaterial)
				.map(ItemStack::new)
				.map(is -> Optional.of(is)
						.map(ItemStack::getItemMeta)
						.map(im -> {
							Optional.of(section)
									.map(s -> s.getString("name"))
									.map(s -> ChatColor.RESET + s)
									.map(s -> s.replace('&', PARAGRAPH))
									.ifPresent(im::setDisplayName);
							im.setLore(section.getStringList("lore").stream()
									.map(s -> ChatColor.RESET + s)
									.map(s -> s.replace('&', PARAGRAPH))
									.collect(Collectors.toList()));
							is.setItemMeta(im);
							return is;
						}))
				.flatMap(x -> x)
				.orElseThrow(IllegalArgumentException::new);
	}

	private void setUpListeners() {
		getServer().getPluginManager().registerEvents(new BlockGeneratorListener(this), this);
	}

	private void createRecipe(@NonNull final ItemStack target, @NonNull final List<String> shape, @NonNull final Map<Character, Material> mapping) {
		final ShapedRecipe recipe = new ShapedRecipe(createKey(target), target);
		recipe.shape(shape.toArray(new String[]{}));
		mapping.forEach(recipe::setIngredient);
		getServer().addRecipe(recipe);
	}

	private NamespacedKey createKey(@NonNull final ItemStack target) {
		return new NamespacedKey(this,
				Optional.of(target)
						.map(ItemStack::getItemMeta)
						.map(ItemMeta::getDisplayName)
						.map(String::toLowerCase)
						.map(name -> name.replaceAll(PARAGRAPH + "[0-9a-f]", ""))
						.map(name -> name.replaceAll("[^a-z0-9/._-]", ""))
						.orElse(getDescription().getName()));
	}

	private void saveGenerators() {
		try (final PrintWriter pw = new PrintWriter(file)) {
			if (!file.exists() && !file.createNewFile()) {
				log.warning("ERROR WHILE CREATING FILE!");
			}
			final JSONArray generatorsJson = generators.stream()
					.collect(Collectors.groupingBy(generator -> generator.getPattern().getName()))
					.entrySet()
					.stream()
					.map(entry -> {
						final JSONObject generatorSet = new JSONObject();
						generatorSet.put("name", entry.getKey());
						final JSONArray jsonArray = entry.getValue()
								.stream()
								.map(Generator::toJSON)
								.collect(JSONArray::new, JSONArray::put, (a1, a2) -> a2.iterator().forEachRemaining(a1::put));
						generatorSet.put("generators", jsonArray);
						return generatorSet;
					})
					.collect(JSONArray::new, JSONArray::put, (a1, a2) -> a2.iterator().forEachRemaining(a1::put));
			pw.write(generatorsJson.toString(4));
		} catch (final IOException ex) {
			log.throwing(getClass().getName(), "saveGenerators", ex);
		}
	}

	@SneakyThrows
	private void loadGenerators() {
		if (!file.exists()) {
			return;
		}
		final JSONArray array = new JSONArray(String.join("", Files.readAllLines(file.toPath())));
		array.forEach(o -> {
			final JSONObject object = (JSONObject) o;
			object.getJSONArray("generators").forEach(g -> {
				final JSONObject generator = (JSONObject) g;
				patterns.stream()
						.filter(p -> p.getName().equals(object.getString("name")))
						.findAny()
						.map(Generator::new)
						.ifPresent(gen -> {
							gen.fromJSON(generator);
							generators.add(gen);
						});
			});
		});
	}
}
