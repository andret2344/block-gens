/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator.helper;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ServerMock {
	@NotNull
	public static Server newServer() {
		final Server server = mock(Server.class);

		final Logger noOp = mock(Logger.class);
		doReturn(noOp).when(server).getLogger();
		when(server.isPrimaryThread()).thenReturn(true);

		// Server must be available before tags can be mocked.
		Bukkit.setServer(server);

		// Bukkit has a lot of static constants referencing registry values. To initialize those, the
		// registries must be able to be fetched before the classes are touched.
		final Map<Class<? extends Keyed>, Object> registers = new HashMap<>();

		doAnswer(invocationGetRegistry ->
				registers.computeIfAbsent(invocationGetRegistry.getArgument(0), clazz -> {
					final Registry<?> registry = mock();
					final Map<NamespacedKey, Keyed> cache = new HashMap<>();
					doAnswer(invocationGetEntry -> {
						final NamespacedKey key = invocationGetEntry.getArgument(0);
						// Some classes (like BlockType and ItemType) have extra generics that will be
						// erased during runtime calls. To ensure accurate typing, grab the constant's field.
						// This approach also allows us to return null for unsupported keys.
						final Class<? extends Keyed> constantClazz;
						try {
							//noinspection unchecked
							constantClazz = (Class<? extends Keyed>) clazz.getField(key.getKey().toUpperCase(Locale.ROOT).replace('.', '_')).getType();
						} catch (final ClassCastException e) {
							throw new RuntimeException(e);
						} catch (final NoSuchFieldException e) {
							return null;
						}

						return cache.computeIfAbsent(key, key1 -> {
							final Keyed keyed = mock(constantClazz);
							doReturn(key).when(keyed).getKey();
							return keyed;
						});
					}).when(registry).get(any());
					return registry;
				}))
				.when(server).getRegistry(any());

		// Tags are dependent on registries, but use a different method.
		// This will set up blank tags for each constant; all that needs to be done to render them
		// functional is to re-mock Tag#getValues.
		doAnswer(invocationGetTag -> {
			final Tag<?> tag = mock();
			doReturn(invocationGetTag.getArgument(1)).when(tag).getKey();
			doReturn(Set.of()).when(tag).getValues();
			doAnswer(invocationIsTagged -> {
				final Keyed keyed = invocationIsTagged.getArgument(0);
				final Class<?> type = invocationGetTag.getArgument(2);
				if (!type.isAssignableFrom(keyed.getClass())) {
					return null;
				}
				// Since these are mocks, the exact instance might not be equal. Consider equal keys equal.
				return tag.getValues().contains(keyed) || tag.getValues().stream().anyMatch(value -> value.getKey().equals(keyed.getKey()));
			}).when(tag).isTagged(notNull());
			return tag;
		}).when(server).getTag(notNull(), notNull(), notNull());

		// Once the server is all set up, touch BlockType and ItemType to initialize.
		// This prevents issues when trying to access dependent methods from a Material constant.
		try {
			Class.forName("org.bukkit.inventory.ItemType");
			Class.forName("org.bukkit.block.BlockType");
			return server;
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	// You should probably not use this - instead, use a new fork so you have full control of the mock server state.
	// Set reuseForks to false in maven-surefire-plugin config.
	public static void unsetBukkitServer() {
		try {
			final Field server = Bukkit.class.getDeclaredField("server");
			server.setAccessible(true);
			server.set(null, null);
		} catch (final NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private ServerMock() {
	}
}
