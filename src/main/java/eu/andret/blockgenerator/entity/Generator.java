package eu.andret.blockgenerator.entity;

import lombok.Value;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

@Value
public class Generator {
	private int regenDelay;
	private ItemStack generatorItem;
	private ItemStack generatedItem;
	private List<Block> placedBlocks = new ArrayList<>();

	public Generator(int regenDelay, ItemStack generatorItem, ItemStack generatedItem) {
		if (regenDelay <= 0) {
			throw new InvalidParameterException("Regen delay cannot be non-positive!");
		}
		this.regenDelay = regenDelay;
		this.generatorItem = generatorItem;
		this.generatedItem = generatedItem;
	}

	public List<Block> getPlacedBlocks() {
		return new ArrayList<>(placedBlocks);
	}

	public void add(Block b) {
		placedBlocks.add(b);
		b.getRelative(0, 1, 0).setType(generatedItem.getType());
	}

	public void remove(Block b) {
		placedBlocks.remove(b);
		b.getRelative(0, 1, 0).setType(Material.AIR);
	}
}
