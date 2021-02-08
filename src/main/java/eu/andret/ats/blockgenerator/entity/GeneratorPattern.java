package eu.andret.ats.blockgenerator.entity;

import lombok.Value;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@Value
public class GeneratorPattern {
	String name;
	int regenDelay;
	ItemStack generatorItem;
	ItemStack generatedItem;

	public boolean matchGeneratedItem(ItemStack candidate) {
		return Objects.equals(generatorItem.getItemMeta(), candidate.getItemMeta());
	}

	public boolean matchGeneratorItem(ItemStack candidate) {
		return Objects.equals(generatorItem.getItemMeta(), candidate.getItemMeta());
	}
}
