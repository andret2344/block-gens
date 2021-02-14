package eu.andret.ats.blockgenerator.entity;

import eu.andret.ats.blockgenerator.utils.RandomCollection;
import lombok.Value;

@Value
public class GeneratorPattern {
	String name;
	long delay;
	NamedItem generatorItem;
	NamedItem generatedItem;
	RandomCollection<NamedItem> dropItems;
}
