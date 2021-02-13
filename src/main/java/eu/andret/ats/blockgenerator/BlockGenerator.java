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
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Log
public class BlockGenerator extends JavaPlugin {
	private static final char PARAGRAPH = '\u00A7';
	private static final String GENERATORS = "generators";

	private final File file = new File(getDataFolder(), "generators.json");
	@Getter
	private final List<GeneratorPattern> patternList = new ArrayList<>();
	@Getter
	private final List<Generator> generatorList = new ArrayList<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		setUpListeners();
		loadConfig();
		loadGenerators();
		setUpCommand();
	}

	@Override
	public void onDisable() {
		saveGenerators();
	}

	private void loadConfig() {
		Optional.of(getConfig())
				.map(config -> config.getConfigurationSection(GENERATORS))
				.map(section -> section.getKeys(false))
				.stream()
				.flatMap(Collection::stream)
				.forEach(name -> Optional.of(getConfig())
						.map(config -> config.getConfigurationSection(GENERATORS))
						.map(section -> section.getConfigurationSection(name))
						.ifPresent(section -> {
							final ItemStack generator = createBlock(section.getConfigurationSection("items.generator"));
							final ItemStack generated = createBlock(section.getConfigurationSection("items.generated"));
							patternList.add(new GeneratorPattern(name, section.getLong("delay", 1L), generator, generated));
							final List<String> shape = section.getStringList("crafting.shape");
							final Map<Character, Material> mapping = new HashMap<>();
							final ConfigurationSection configurationSection = section.getConfigurationSection("crafting.mapping");
							Objects.requireNonNull(configurationSection).getKeys(false).forEach(key -> Optional.of(key)
									.map(configurationSection::getString)
									.map(Material::getMaterial)
									.ifPresent(material -> mapping.put(key.charAt(0), material)));
							createRecipe(generator, shape, mapping);
						}));
	}

	private void setUpCommand() {
		final AnnotatedCommand command = CommandManager.registerCommand(BlockGeneratorCommand.class, this);
		command.addTypeCompleter(GeneratorPattern.class, () -> patternList.stream().map(GeneratorPattern::getName).collect(Collectors.toSet()));
		command.addArgumentMapper("patternName", GeneratorPattern.class, name -> patternList.stream().filter(p -> p.getName().equals(name)).findAny().orElse(null), Fallback.ON_NULL);
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
		recipe.shape(shape.toArray(new String[0]));
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
			final JSONArray generatorsJson = generatorList.stream()
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
						generatorSet.put(GENERATORS, jsonArray);
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
		final JSONArray array = new JSONArray(new JSONTokener(new FileReader(file)));
		array.forEach(o -> {
			final JSONObject object = (JSONObject) o;
			object.getJSONArray(GENERATORS).forEach(g -> {
				final JSONObject generator = (JSONObject) g;
				patternList.stream()
						.filter(p -> p.getName().equals(object.getString("name")))
						.findAny()
						.map(Generator::new)
						.ifPresent(gen -> {
							gen.fromJSON(generator);
							generatorList.add(gen);
						});
			});
		});
	}
}
