/*
 * Copyright Andret Tools System (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

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
