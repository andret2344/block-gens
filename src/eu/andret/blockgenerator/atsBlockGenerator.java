package eu.andret.blockgenerator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class atsBlockGenerator extends JavaPlugin {
    private final File file = new File(getDataFolder(), "list.tmp");
    private final List<Block> generators = new ArrayList<>();

    private ItemStack generator;
    private Material generated;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setUpListeners();
        createBlocks();
        createRecipe();
        loadGenerators();
    }

    @Override
    public void onDisable() {
        saveGenerators();
    }

    private void setUpListeners() {
        new BlockGeneratorListener(this);
    }

    private void createRecipe() {
        ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(this, getDescription().getName()), generator);
        shapedRecipe.shape("@@@", "@#@", "@@@").setIngredient('@', Material.STONE).setIngredient('#', Material.PISTON_BASE);
        getServer().addRecipe(shapedRecipe);
    }

    private void createBlocks() {
        Material tmp = Material.getMaterial(getConfig().getString("material.generator", "SPONGE"));
        if (tmp == null) {
            tmp = Material.SPONGE;
        }
        generator = new ItemStack(tmp);
        ItemMeta im = generator.getItemMeta();
        im.setDisplayName(getConfig().getString("stone.name").replace("&", "§"));
        im.setLore(Arrays.asList(getConfig().getString("stone.lore").replace("&", "§").split("\r")));
        generated = Material.getMaterial(getConfig().getString("material.generated", "STONE"));
        if (generated == null) {
            generated = Material.STONE;
        }
        generator.setItemMeta(im);
    }

    private void saveGenerators() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
            if (!file.exists() && generators.size() != 0) {
                if (!file.createNewFile()) {
                    System.err.println("ERROR WHILE CREATING FILE!");
                    return;
                }
            }
            for (Block b : generators) {
                pw.println(b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ());
            }
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
                    Location l = new Location(getServer().getWorld(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3]));
                    generators.add(l.getBlock());
                }
                file.delete();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ItemStack getGenerator() {
        return generator;
    }

    public List<Block> getGenerators() {
        return generators;
    }

    public Material getGenerated() {
        return generated;
    }
}
