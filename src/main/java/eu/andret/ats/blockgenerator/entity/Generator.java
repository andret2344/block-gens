/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator.entity;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public record Generator(@NotNull GeneratorPattern pattern, @NotNull Block block) {
}
