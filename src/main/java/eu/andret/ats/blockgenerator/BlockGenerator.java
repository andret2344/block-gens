package eu.andret.ats.blockgenerator;

import eu.andret.arguments.AnnotatedCommand;
import eu.andret.arguments.CommandManager;
import eu.andret.arguments.api.annotation.Fallback;
import eu.andret.ats.blockgenerator.entity.Generator;
import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import eu.andret.ats.blockgenerator.entity.NamedItem;
import eu.andret.ats.blockgenerator.utils.ConfigLoader;
import eu.andret.ats.blockgenerator.utils.RandomCollection;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Log
public class BlockGenerator extends JavaPlugin {
	private static final String GENERATORS = "generators";

	@Getter
	private final List<GeneratorPattern> patternList = new ArrayList<>();
	@Getter
	private final List<Generator> generatorList = new ArrayList<>();

	private final File file = new File(getDataFolder(), "generators.json");
	private final ConfigLoader configLoader = new ConfigLoader(this);

	@Override
	public void onEnable() {
		saveDefaultConfig();
		setUpListeners();
		patternList.addAll(configLoader.loadGeneratorPatterns());
		loadGenerators();
		setUpCommand();
	}

	@Override
	public void onDisable() {
		saveGenerators();
	}

	private void setUpCommand() {
		final AnnotatedCommand command = CommandManager.registerCommand(BlockGeneratorCommand.class, this);
		command.addArgumentCompleter("generatorName", () -> patternList.stream()
				.map(GeneratorPattern::getGeneratorItem)
				.map(NamedItem::getName)
				.collect(Collectors.toSet()));
		command.addArgumentMapper("generatorName", NamedItem.class, name -> patternList.stream()
				.map(GeneratorPattern::getGeneratorItem)
				.filter(p -> p.getName().equals(name))
				.findAny()
				.orElse(null), Fallback.ON_NULL);
		command.addArgumentCompleter("generatedName", () -> patternList.stream()
				.map(GeneratorPattern::getGeneratedItem)
				.map(NamedItem::getName)
				.collect(Collectors.toList()));
		command.addArgumentMapper("generatedName", NamedItem.class, name -> patternList.stream()
				.map(GeneratorPattern::getGeneratedItem)
				.filter(p -> p.getName().equals(name))
				.findAny()
				.orElse(null), Fallback.ON_NULL);
		command.addArgumentCompleter("itemName", () -> patternList.stream()
				.map(GeneratorPattern::getDropItems)
				.map(RandomCollection::getItems)
				.flatMap(Collection::stream)
				.map(NamedItem::getName)
				.collect(Collectors.toList()));
		command.addArgumentMapper("itemName", NamedItem.class, name -> patternList.stream()
				.map(GeneratorPattern::getDropItems)
				.map(RandomCollection::getItems)
				.flatMap(Collection::stream)
				.filter(p -> p.getName().equals(name))
				.findAny()
				.orElse(null), Fallback.ON_NULL);
	}

	private void setUpListeners() {
		getServer().getPluginManager().registerEvents(new BlockGeneratorListener(this), this);
	}

	private void saveGenerators() {
		try (final PrintWriter pw = new PrintWriter(file)) {
			if (!file.exists() && !file.createNewFile()) {
				log.warning("ERROR WHILE CREATING FILE!");
			}
			final JSONArray generatorsJson = generatorList.stream()
					.collect(Collectors.groupingBy(generator -> generator.getPattern().getName()))
					.entrySet()
					.stream()
					.map(entry -> {
						final JSONObject generatorSet = new JSONObject();
						generatorSet.put("name", entry.getKey());
						final JSONArray jsonArray = entry.getValue()
								.stream()
								.map(this::mapGeneratorToJSON)
								.collect(JSONArray::new, JSONArray::put, (a1, a2) -> a2.iterator().forEachRemaining(a1::put));
						generatorSet.put(GENERATORS, jsonArray);
						return generatorSet;
					})
					.collect(JSONArray::new, JSONArray::put, (a1, a2) -> a2.iterator().forEachRemaining(a1::put));
			pw.write(generatorsJson.toString(4));
		} catch (final IOException ex) {
			log.throwing(getClass().getName(), "saveGenerators", ex);
		}
	}

	@SneakyThrows
	private void loadGenerators() {
		if (!file.exists()) {
			return;
		}
		final JSONArray array = new JSONArray(new JSONTokener(new FileReader(file)));
		array.forEach(o -> {
			final JSONObject object = (JSONObject) o;
			object.getJSONArray(GENERATORS).forEach(g -> {
				final JSONObject generator = (JSONObject) g;
				patternList.stream()
						.filter(p -> p.getName().equals(object.getString("name")))
						.findAny()
						.map(pattern -> mapJSONToGenerator(generator, pattern))
						.ifPresent(generatorList::add);
			});
		});
	}

	private JSONObject mapGeneratorToJSON(final Generator generator) {
		final JSONObject result = new JSONObject();
		result.put("world", generator.getBlock().getWorld().getName());
		result.put("x", generator.getBlock().getX());
		result.put("y", generator.getBlock().getY());
		result.put("z", generator.getBlock().getZ());
		return result;
	}

	private Generator mapJSONToGenerator(final JSONObject jsonObject, final GeneratorPattern pattern) {
		final Location location = new Location(Bukkit.getWorld(jsonObject.getString("world")),
				jsonObject.getInt("x"),
				jsonObject.getInt("y"),
				jsonObject.getInt("z"));
		return new Generator(pattern, location.getBlock());
	}
}
