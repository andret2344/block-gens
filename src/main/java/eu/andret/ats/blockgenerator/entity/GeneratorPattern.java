/*
 * Copyright Andret Tools System (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator.entity;

import eu.andret.ats.blockgenerator.utils.RandomCollection;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class GeneratorPattern {
	@NotNull
	String name;
	long delay;
	@NotNull
	NamedItem generatorItem;
	@NotNull
	NamedItem generatedItem;
	@NotNull
	RandomCollection<NamedItem> dropItems;
}
