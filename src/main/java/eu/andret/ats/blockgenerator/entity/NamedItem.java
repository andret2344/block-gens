/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator.entity;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public record NamedItem(@NotNull String name, @NotNull ItemStack itemStack) {
}
