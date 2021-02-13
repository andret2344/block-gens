package eu.andret.ats.blockgenerator.entity;

import lombok.Value;
import org.bukkit.block.Block;

@Value
public class Generator {
	GeneratorPattern pattern;
	Block block;
}
