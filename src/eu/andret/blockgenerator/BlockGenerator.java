package eu.andret.blockgenerator;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class BlockGenerator {
    private final int regenDelay;
    private final ItemStack generator;
    private final ItemStack generated;
    private final List<Block> placed = new ArrayList<>();

    public BlockGenerator(int regenDelay, ItemStack generator, ItemStack generated) {
        this.regenDelay = regenDelay;
        this.generator = generator;
        this.generated = generated;
    }

    public int getRegenDelay() {
        return regenDelay;
    }

    public ItemStack getGenerator() {
        return generator;
    }

    public ItemStack getGenerated() {
        return generated;
    }

    public List<Block> getPlaced() {
        return new ArrayList<>(placed);
    }

    public void add(Block b) {
        placed.add(b);
    }

    public void add(Location l) {
        add(l.getBlock());
    }

    public void remove(Block b) {
        placed.remove(b);
    }

    public void remove(Location l) {
        remove(l.getBlock());
    }
}
