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
    private File file;

    private ItemStack generator;
    private Material generated;
    private final List<Block> blocks = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        new BlockGeneratorListener(this);
        file = new File(getDataFolder(), "list.tmp");
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
        ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(this, getDescription().getName()), generator);
        shapedRecipe.shape("@@@", "@#@", "@@@").setIngredient('@', Material.STONE).setIngredient('#', Material.PISTON_BASE);
        getServer().addRecipe(shapedRecipe);
        load();
    }

    @Override
    public void onDisable() {
        try {
            if (!file.exists() && blocks.size() != 0) {
                file.createNewFile();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        for (Block b : blocks) {
            save(b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ());
        }
    }

    private void save(String message) {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(file, true));
            pw.println(message);
            pw.flush();
            pw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void load() {
        try {
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] s = line.split(":");
                    Location l = new Location(getServer().getWorld(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3]));
                    blocks.add(l.getBlock());
                }
                reader.close();
                file.delete();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ItemStack getGenerator() {
        return generator;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public Material getGenerated() {
        return generated;
    }
}
