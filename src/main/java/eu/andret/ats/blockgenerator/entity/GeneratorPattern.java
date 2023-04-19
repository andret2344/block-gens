/*
 * Copyright (c) 2018 Andret Tools System. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator.entity;

import eu.andret.ats.blockgenerator.utils.RandomCollection;
import org.jetbrains.annotations.NotNull;

public record GeneratorPattern(@NotNull String name,
							   long delay,
							   @NotNull NamedItem generatorItem,
							   @NotNull NamedItem generatedItem,
							   @NotNull RandomCollection<NamedItem> dropItems) {
}
