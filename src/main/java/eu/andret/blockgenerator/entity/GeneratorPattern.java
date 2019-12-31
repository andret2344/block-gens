package eu.andret.blockgenerator.entity;

import lombok.Value;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@Value
public class GeneratorPattern {
	private String name;
	private int regenDelay;
	private ItemStack generatorItem;
	private ItemStack generatedItem;

	public boolean matchGeneratedItem(ItemStack candidate) {
		return Objects.equals(generatorItem.getItemMeta(), candidate.getItemMeta());
	}

	public boolean matchGeneratorItem(ItemStack candidate) {
		return Objects.equals(generatorItem.getItemMeta(), candidate.getItemMeta());
	}
}
