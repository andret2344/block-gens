package eu.andret.blockgenerator;

import eu.andret.blockgenerator.entity.Generator;
import eu.andret.blockgenerator.entity.GeneratorPattern;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
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
import java.util.stream.Collectors;

@Log
public class BlockGenerator extends JavaPlugin {
	private static final char PARAGRAPH = '\u00A7';

	private final File file = new File(getDataFolder(), "list.tmp");

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
			for (String key : configurationSection.getKeys(false)) {
				Material mat = Material.getMaterial(configurationSection.getString(key));
				if (mat != null) {
					mapping.put(key.charAt(0), mat);
				}
			}
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
		Material tmp = Material.getMaterial(section.getString("material"));
		if (tmp == null) {
			throw new IllegalArgumentException("Unknown material!");
		}
		ItemStack itemStack = new ItemStack(tmp);
		ItemMeta im = itemStack.getItemMeta();
		if (section.getString("name") != null) {
			im.setDisplayName(PARAGRAPH + "r" + section.getString("name").replace('&', PARAGRAPH));
		}
		section.getStringList("lore");
		im.setLore(section.getStringList("lore").stream().collect(ArrayList::new, (l, s) -> l.add(s.replace('&', PARAGRAPH)), ArrayList::addAll));
		itemStack.setItemMeta(im);
		return itemStack;
	}

	private void setUpListeners() {
		getServer().getPluginManager().registerEvents(new BlockGeneratorListener(this), this);
	}

	private void createRecipe(@NonNull ItemStack target, @NonNull List<String> shape, @NonNull Map<Character, Material> mapping) {
		String name = getDescription().getName() + "-" + target.getItemMeta().getDisplayName();
		String normalizedName = name.replace(' ', '_').replaceAll("[^a-zA-Z0-9/._-]", "");
		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, normalizedName), target);
		recipe.shape(shape.toArray(new String[]{}));
		mapping.forEach(recipe::setIngredient);
		getServer().addRecipe(recipe);
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
