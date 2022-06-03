/*
 * Copyright Andret Tools System (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator.entity;

import lombok.Value;
import org.bukkit.block.Block;

@Value
public class Generator {
	GeneratorPattern pattern;
	Block block;
}
