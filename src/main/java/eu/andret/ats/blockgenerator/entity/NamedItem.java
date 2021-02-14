package eu.andret.ats.blockgenerator.entity;

import lombok.NonNull;
import lombok.Value;
import org.bukkit.inventory.ItemStack;

@Value
public class NamedItem {
	@NonNull
	String name;
	@NonNull
	ItemStack itemStack;
}
