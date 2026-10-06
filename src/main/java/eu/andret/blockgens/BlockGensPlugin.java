package eu.andret.blockgens;

import eu.andret.blockgens.command.DropItem;
import eu.andret.blockgens.command.GeneratedItem;
import eu.andret.blockgens.command.GeneratorItem;
import eu.andret.blockgens.command.NamedItemParameterType;
import eu.andret.blockgens.command.PlaceholderCondition;
import eu.andret.blockgens.config.ConfigLoader;
import eu.andret.blockgens.config.GeneratorPattern;
import eu.andret.blockgens.storage.GeneratorStore;
import eu.andret.blockgens.util.RandomCollection;
import org.bstats.bukkit.Metrics;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BlockGensPlugin extends JavaPlugin {
	@NotNull
	private final List<GeneratorPattern> patternList = new ArrayList<>();
	@NotNull
	private final NamespacedKey generatorKey = new NamespacedKey(this, "generator");
	@NotNull
	private final ConfigLoader configLoader = new ConfigLoader(this);
	@NotNull
	private final GeneratorStore generatorStore = new GeneratorStore(this);
	@NotNull
	private final Map<Block, Integer> schedulers = new HashMap<>();
	@NotNull
	private final BlockGensListener listener = new BlockGensListener(this);

	@Override
	public void onEnable() {
		saveDefaultConfig();
		applyPatterns(configLoader.loadGeneratorPatterns(getConfig()));
		setUpListeners();
		setUpCommand();
		new Metrics(this, 34535);
	}

	@Override
	public void onDisable() {
		// The recipes stay on the server otherwise, and enabling the plugin again would register them twice
		applyPatterns(List.of());
	}

	/**
	 * Loads the config again. When it is invalid, nothing changes and the exception says why.
	 *
	 * @return the number of generators loaded
	 * @throws IOException                   when the config file cannot be read
	 * @throws InvalidConfigurationException when the config is not valid YAML
	 * @throws IllegalArgumentException      when the content of the config is invalid
	 */
	public int reload() throws IOException, InvalidConfigurationException {
		final YamlConfiguration config = new YamlConfiguration();
		config.load(new File(getDataFolder(), "config.yml"));
		final List<GeneratorPattern> patterns = configLoader.loadGeneratorPatterns(config);
		reloadConfig();
		applyPatterns(patterns);
		// Generators of patterns that were missing before can start now
		restoreLoadedChunks();
		return patterns.size();
	}

	private void applyPatterns(@NotNull final List<GeneratorPattern> patterns) {
		patternList.stream()
				.map(GeneratorPattern::recipe)
				.filter(Objects::nonNull)
				.forEach(recipe -> getServer().removeRecipe(recipe.getKey(), false));
		patternList.clear();
		patternList.addAll(patterns);
		patternList.stream()
				.map(GeneratorPattern::recipe)
				.filter(Objects::nonNull)
				.forEach(recipe -> getServer().addRecipe(recipe, false));
		getServer().updateRecipes();
	}

	private void setUpCommand() {
		final Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this)
				.parameterTypes(types -> types
						.addParameterType(GeneratorItem.class, new NamedItemParameterType<>(() -> patternList.stream().map(GeneratorPattern::generatorItem).toList(), GeneratorItem::new, "No generator block found: \"%s\""))
						.addParameterType(GeneratedItem.class, new NamedItemParameterType<>(() -> patternList.stream().map(GeneratorPattern::generatedItem).toList(), GeneratedItem::new, "No generated block found: \"%s\""))
						.addParameterType(DropItem.class, new NamedItemParameterType<>(() -> patternList.stream()
								.map(GeneratorPattern::dropItems)
								.map(RandomCollection::getItems)
								.flatMap(Collection::stream)
								.toList(),
								DropItem::new, "No drop item found: \"%s\"")))
				.commandCondition(new PlaceholderCondition())
				.build();
		lamp.register(new BlockGensCommand(this));
	}

	private void setUpListeners() {
		getServer().getPluginManager().registerEvents(listener, this);
		// The chunks loaded before the plugin was enabled never fire a ChunkLoadEvent for it
		restoreLoadedChunks();
	}

	private void restoreLoadedChunks() {
		getServer().getWorlds()
				.stream()
				.map(World::getLoadedChunks)
				.flatMap(Arrays::stream)
				.forEach(listener::restore);
	}

	@NotNull
	public Optional<GeneratorPattern> getPattern(@NotNull final String name) {
		return patternList.stream()
				.filter(generatorPattern -> generatorPattern.name().equals(name))
				.findFirst();
	}

	@NotNull
	public List<GeneratorPattern> getPatternList() {
		return patternList;
	}

	/**
	 * The key of the tag that makes an item a generator, with the name of its pattern as the value.
	 */
	@NotNull
	public NamespacedKey getGeneratorKey() {
		return generatorKey;
	}

	@NotNull
	public GeneratorStore getGeneratorStore() {
		return generatorStore;
	}

	public void addScheduler(@NotNull final Block block, final int id) {
		schedulers.put(block, id);
	}

	public boolean isSchedulerPresent(@NotNull final Block block) {
		return schedulers.containsKey(block);
	}

	public void cancelScheduler(@NotNull final Block block) {
		getServer().getScheduler().cancelTask(schedulers.get(block));
		removeScheduler(block);
	}

	public void removeScheduler(@NotNull final Block block) {
		schedulers.remove(block);
	}
}
