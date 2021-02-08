package eu.andret.ats.blockgenerator;

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
		List<?> sections = getConfig().getList("generators");
		for (Object section : sections) {
			ConfigurationSection current = getConfig().createSection("current", (Map<?, ?>) section);
			ItemStack generator = createBlock(current.getConfigurationSection("items.generator"));
			ItemStack generated = createBlock(current.getConfigurationSection("items.generated"));
			patterns.add(new GeneratorPattern(current.getString("name"), current.getInt("regen-delay"), generator, generated));
			List<String> shape = current.getStringList("crafting.shape");
			Map<Character, Material> mapping = new HashMap<>();
			ConfigurationSection configurationSection = current.getConfigurationSection("crafting.mapping");
			Objects.requireNonNull(configurationSection).getKeys(false).forEach(key -> Optional.of(key)
					.map(configurationSection::getString)
					.map(Material::getMaterial)
					.ifPresent(material -> mapping.put(key.charAt(0), material)));
			createRecipe(generator, shape, mapping);
		}
		loadGenerators();
	}

	@Override
	public void onDisable() {
		saveGenerators();
	}

	@NonNull
	private ItemStack createBlock(ConfigurationSection section) {
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

	private void createRecipe(@NonNull ItemStack target, @NonNull List<String> shape, @NonNull Map<Character, Material> mapping) {
		ShapedRecipe recipe = new ShapedRecipe(createKey(target), target);
		recipe.shape(shape.toArray(new String[]{}));
		mapping.forEach(recipe::setIngredient);
		getServer().addRecipe(recipe);
	}

	private NamespacedKey createKey(@NonNull ItemStack target) {
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
		try (PrintWriter pw = new PrintWriter(file)) {
			if (!file.exists() && !file.createNewFile()) {
				log.warning("ERROR WHILE CREATING FILE!");
			}
			JSONArray generatorsJson = generators.stream()
					.collect(Collectors.groupingBy(generator -> generator.getPattern().getName()))
					.entrySet()
					.stream()
					.map(entry -> {
						JSONObject generatorSet = new JSONObject();
						generatorSet.put("name", entry.getKey());
						JSONArray jsonArray = entry.getValue()
								.stream()
								.map(Generator::toJSON)
								.collect(JSONArray::new, JSONArray::put, (a1, a2) -> a2.iterator().forEachRemaining(a1::put));
						generatorSet.put("generators", jsonArray);
						return generatorSet;
					})
					.collect(JSONArray::new, JSONArray::put, (a1, a2) -> a2.iterator().forEachRemaining(a1::put));
			pw.write(generatorsJson.toString(4));
		} catch (IOException ex) {
			log.throwing(getClass().getName(), "saveGenerators", ex);
		}
	}

	@SneakyThrows
	private void loadGenerators() {
		if (!file.exists()) {
			return;
		}
		JSONArray array = new JSONArray(String.join("", Files.readAllLines(file.toPath())));
		array.forEach(o -> {
			JSONObject object = (JSONObject) o;
			object.getJSONArray("generators").forEach(g -> {
				JSONObject generator = (JSONObject) g;
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
