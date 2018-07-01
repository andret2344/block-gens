package eu.andret.blockgenerator.entity;

import lombok.Value;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

@Value
public class Generator {
    private int regenDelay;
    private ItemStack generator;
    private ItemStack generated;
    private List<Block> placedBlocks = new ArrayList<>();

    public Generator(int regenDelay, ItemStack generator, ItemStack generated) {
        if (regenDelay <= 0) {
            throw new InvalidParameterException("Regen delay cannot be non-positive!");
        }
        this.regenDelay = regenDelay;
        this.generator = generator;
        this.generated = generated;
    }

    public List<Block> getPlacedBlocks() {
        return new ArrayList<>(placedBlocks);
    }

    public void add(Block b) {
        placedBlocks.add(b);
        b.getRelative(0, 1, 0).setType(generated.getType());
    }

    public void add(Location l) {
        add(l.getBlock());
    }

    public void remove(Block b) {
        placedBlocks.remove(b);
        b.getRelative(0, 1, 0).setType(Material.AIR);
    }

    public void remove(Location l) {
        remove(l.getBlock());
    }
}
