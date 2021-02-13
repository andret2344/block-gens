package eu.andret.ats.blockgenerator.entity;

import lombok.Value;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

@Value
public class GeneratorPattern {
	String name;
	long delay;
	ItemStack generatorItem;
	ItemStack generatedItem;

	public boolean matchGeneratedItem(final ItemStack candidate) {
		return Objects.equals(generatorItem.getItemMeta(), candidate.getItemMeta());
	}

	public boolean matchGeneratorItem(final ItemStack candidate) {
		return Objects.equals(generatorItem.getItemMeta(), candidate.getItemMeta());
	}
}
