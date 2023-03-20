/*
 * Copyright Andret Tools System (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator;

import eu.andret.arguments.AnnotatedCommand;
import eu.andret.arguments.CommandManager;
import eu.andret.arguments.api.entity.FallbackConstants;
import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import eu.andret.ats.blockgenerator.entity.NamedItem;
import eu.andret.ats.blockgenerator.utils.ConfigLoader;
import eu.andret.ats.blockgenerator.utils.RandomCollection;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Generated("Main class")
public class BlockGeneratorPlugin extends JavaPlugin {
	@Getter
	private final List<GeneratorPattern> patternList = new ArrayList<>();
	private final ConfigLoader configLoader = new ConfigLoader(this);

	@Override
	public void onEnable() {
		saveDefaultConfig();
		setUpListeners();
		patternList.addAll(configLoader.loadGeneratorPatterns());
		setUpCommand();
		new Metrics(this, 10330);
	}

	private void setUpCommand() {
		final AnnotatedCommand<BlockGeneratorPlugin> command = CommandManager
				.registerCommand(BlockGeneratorCommand.class, this);
		command.addArgumentCompleter("generatorName", () -> patternList.stream()
				.map(GeneratorPattern::getGeneratorItem)
				.map(NamedItem::getName)
				.collect(Collectors.toSet()));
		command.addArgumentMapper("generatorName", NamedItem.class, name -> patternList.stream()
						.map(GeneratorPattern::getGeneratorItem)
						.filter(item -> item.getName().equals(name))
						.findAny()
						.orElse(null),
				FallbackConstants.ON_NULL);
		command.addArgumentCompleter("generatedName", () -> patternList.stream()
				.map(GeneratorPattern::getGeneratedItem)
				.map(NamedItem::getName)
				.toList());
		command.addArgumentMapper("generatedName", NamedItem.class, name -> patternList.stream()
						.map(GeneratorPattern::getGeneratedItem)
						.filter(item -> item.getName().equals(name))
						.findAny()
						.orElse(null),
				FallbackConstants.ON_NULL);
		command.addArgumentCompleter("itemName", () -> patternList.stream()
				.map(GeneratorPattern::getDropItems)
				.map(RandomCollection::getItems)
				.flatMap(Collection::stream)
				.map(NamedItem::getName)
				.toList());
		command.addArgumentMapper("itemName", NamedItem.class, name -> patternList.stream()
						.map(GeneratorPattern::getDropItems)
						.map(RandomCollection::getItems)
						.flatMap(Collection::stream)
						.filter(item -> item.getName().equals(name))
						.findAny()
						.orElse(null),
				FallbackConstants.ON_NULL);
	}

	private void setUpListeners() {
		getServer().getPluginManager().registerEvents(new BlockGeneratorListener(this), this);
	}

	@NotNull
	public Optional<GeneratorPattern> getPattern(@NotNull final String name) {
		return patternList.stream()
				.filter(generatorPattern -> generatorPattern.getName().equals(name))
				.findFirst();
	}
}
