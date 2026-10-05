package eu.andret.blockgens.storage;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps the placed generators in the persistent data of their chunks, so they survive a server restart. Each generator
 * is a separate entry with the key {@code generator/<x>/<y>/<z>} and the pattern name as the value.
 */
public class GeneratorStore {
	private static final String PREFIX = "generator";
	private static final String SEPARATOR = "/";

	@NotNull
	private final Plugin plugin;

	public GeneratorStore(@NotNull final Plugin plugin) {
		this.plugin = plugin;
	}

	public void save(@NotNull final Block block, @NotNull final String patternName) {
		block.getChunk().getPersistentDataContainer().set(createKey(block), PersistentDataType.STRING, patternName);
	}

	@NotNull
	public Optional<String> find(@NotNull final Block block) {
		return Optional.ofNullable(block.getChunk().getPersistentDataContainer()
				.get(createKey(block), PersistentDataType.STRING));
	}

	public void remove(@NotNull final Block block) {
		block.getChunk().getPersistentDataContainer().remove(createKey(block));
	}

	@NotNull
	public Map<Block, String> findAll(@NotNull final Chunk chunk) {
		final PersistentDataContainer container = chunk.getPersistentDataContainer();
		final String namespace = plugin.getName().toLowerCase(Locale.ROOT);
		final Map<Block, String> result = new HashMap<>();
		container.getKeys()
				.stream()
				.filter(key -> key.getNamespace().equals(namespace))
				.forEach(key -> Optional.of(key.getKey().split(SEPARATOR))
						.filter(parts -> parts.length == 4 && parts[0].equals(PREFIX))
						.flatMap(parts -> parseBlock(chunk, parts))
						.ifPresent(block -> Optional.ofNullable(container.get(key, PersistentDataType.STRING))
								.ifPresent(patternName -> result.put(block, patternName))));
		return result;
	}

	@NotNull
	private Optional<Block> parseBlock(@NotNull final Chunk chunk, @NotNull final String[] parts) {
		try {
			return Optional.of(chunk.getWorld().getBlockAt(
					Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2]),
					Integer.parseInt(parts[3])));
		} catch (final NumberFormatException _) {
			return Optional.empty();
		}
	}

	@NotNull
	private NamespacedKey createKey(@NotNull final Block block) {
		final String join = String.join(
				SEPARATOR,
				PREFIX,
				String.valueOf(block.getX()),
				String.valueOf(block.getY()),
				String.valueOf(block.getZ()));
		return new NamespacedKey(plugin, join);
	}
}
