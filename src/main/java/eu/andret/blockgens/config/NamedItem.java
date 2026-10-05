package eu.andret.blockgens.config;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public record NamedItem(@NotNull String name, @NotNull ItemStack itemStack) {
}
