package eu.andret.atsstonegenerator;

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

public class atsStoneGenerator extends JavaPlugin {
    private File file;
    private final Listener listener = new StonePlace(this);

    private final ItemStack stoneGenerator = new ItemStack(Material.SPONGE);
    private final List<Block> blocks = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        file = new File(getDataFolder(), "list.tmp");
        ItemMeta im = stoneGenerator.getItemMeta();
        im.setDisplayName(getConfig().getString("stone.name").replace("&", "§"));
        im.setLore(Arrays.asList(getConfig().getString("stone.lore").replace("&", "§").split("\r")));
        stoneGenerator.setItemMeta(im);
        ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(this, getDescription().getName()), stoneGenerator);
        shapedRecipe.shape("@@@", "@#@", "@@@").setIngredient('@', Material.STONE).setIngredient('#', Material.PISTON_BASE);
        getServer().addRecipe(shapedRecipe);
        load();
    }

    @Override
    public void onDisable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
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
                String line = "";
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

    public ItemStack getStoneGenerator() {
        return stoneGenerator;
    }

    public List<Block> getBlocks() {
        return blocks;
    }
}
