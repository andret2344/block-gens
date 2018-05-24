package eu.andret.blockgenerator;

import java.util.ArrayList;
import java.util.List;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

@Value
@NonFinal
public class BlockGenerator {
    private int regenDelay;
    private ItemStack generator;
    private ItemStack generated;
    private List<Block> placed = new ArrayList<>();

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
