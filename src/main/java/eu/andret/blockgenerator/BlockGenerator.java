package eu.andret.blockgenerator;

import eu.andret.blockgenerator.entity.Generator;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log
public class BlockGenerator extends JavaPlugin {
	private static final char PARAGRAPH = '\u00A7';
	
	private final File file = new File(getDataFolder(), "list.tmp");

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
			generators.add(new Generator(current.getInt("regen-delay"), generator, generated));
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
		if (section.getStringList("lore") != null) {
			im.setLore(section.getStringList("lore").stream().collect(ArrayList::new, (l, s) -> l.add(s.replace('&', PARAGRAPH)), ArrayList::addAll));
		}
		itemStack.setItemMeta(im);
		return itemStack;
	}

	private void setUpListeners() {
		getServer().getPluginManager().registerEvents(new BlockGeneratorListener(this), this);
	}

	private void createRecipe(@NonNull ItemStack target, @NonNull List<String> shape, @NonNull Map<Character, Material> mapping) {
		String name = (getDescription().getName() + "-" + target.getItemMeta().getDisplayName());
		String normalizedName = name.replace(' ', '_').replaceAll("[^a-zA-Z0-9/._-]", "");
		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, normalizedName), target);
		recipe.shape(shape.toArray(new String[]{}));
		for (Map.Entry<Character, Material> entry : mapping.entrySet()) {
			recipe.setIngredient(entry.getKey(), entry.getValue());
		}
		getServer().addRecipe(recipe);
	}

	private void saveGenerators() {
		try (PrintWriter pw = new PrintWriter(new FileWriter(file, false))) {
			if (!file.exists() && !generators.isEmpty() && !file.createNewFile()) {
				log.warning("ERROR WHILE CREATING FILE!");
			}
		} catch (IOException ex) {
			log.throwing(getClass().getName(), "saveGenerators", ex);
		}
	}

	private void loadGenerators() {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			if (file.exists()) {
				String line;
				while ((line = reader.readLine()) != null) {
					String[] s = line.split(":");
					Location l = new Location(getServer().getWorld(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3]), Integer.parseInt(s[4]));
					Material material = Material.getMaterial(s[0]);
					generators.stream()
							.filter(g -> g.getGeneratorItem().getType().equals(material))
							.limit(1)
							.forEach(g -> g.add(l.getBlock()));
				}
				if (Files.deleteIfExists(file.toPath())) {
					log.warning("Error while removing temporary file");
				}
			}
		} catch (Exception ex) {
			log.throwing("BlockGenerator", "loadGenerators", ex);
		}
	}
}
