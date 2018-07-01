package eu.andret.blockgenerator;

import eu.andret.blockgenerator.entity.Generator;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockGenerator extends JavaPlugin {
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

    private ItemStack createBlock(ConfigurationSection section) {
        Material tmp = Material.getMaterial(section.getString("material"));
        if (tmp == null) {
            return null;
        }
        ItemStack itemStack = new ItemStack(tmp);
        ItemMeta im = itemStack.getItemMeta();
        if (section.getString("name") != null) {
            im.setDisplayName(section.getString("name").replace("&", "§"));
        }
        if (section.getStringList("lore") != null) {
            im.setLore(section.getStringList("lore").stream().collect(ArrayList::new, (l, s) -> l.add(s.replace("&", "§")), ArrayList::addAll));
        }
        itemStack.setItemMeta(im);
        return itemStack;
    }

    private void setUpListeners() {
        getServer().getPluginManager().registerEvents(new BlockGeneratorListener(this), this);
    }

    private void createRecipe(ItemStack target, List<String> shape, Map<Character, Material> mapping) {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, (getDescription().getFullName() + "-" + target.getItemMeta().getDisplayName()).replace(' ', '_')), target);
        recipe.shape(shape.toArray(new String[]{}));
        for (Map.Entry<Character, Material> entry : mapping.entrySet()) {
            recipe.setIngredient(entry.getKey(), entry.getValue());
        }
        getServer().addRecipe(recipe);
    }

    private void saveGenerators() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, false))) {
            if (!file.exists() && generators.size() != 0) {
                if (!file.createNewFile()) {
                    System.err.println("ERROR WHILE CREATING FILE!");
                    return;
                }
            }
            generators.stream().flatMap(s -> s.getPlacedBlocks().stream())
                    .forEach(block -> pw.println(block.getType().name() + ":" + block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ()));
        } catch (IOException ex) {
            ex.printStackTrace();
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
                    for (Generator generator : generators) {
                        if (generator.getGenerator().getType().equals(material)) {
                            generator.add(l);
                            break;
                        }
                    }
                }
                if (!file.delete()) {
                    System.err.print("Error while removing temporary file");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
