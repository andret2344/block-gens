/*
 * Copyright Andret Tools System (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator.utils;

import eu.andret.ats.blockgenerator.BlockGeneratorPlugin;
import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import eu.andret.ats.blockgenerator.entity.NamedItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.mockito.MockedStatic;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ConfigLoaderTest {
	private final BlockGeneratorPlugin plugin = mock(BlockGeneratorPlugin.class);
	private final ConfigLoader configLoader = new ConfigLoader(plugin);

	@Test
	void loadGeneratorPatterns() throws IOException, InvalidConfigurationException, InvalidDescriptionException {
		// given
		final FileConfiguration configuration = getFileConfiguration();
		when(plugin.getServer()).thenReturn(mock(Server.class));
		when(plugin.getName()).thenReturn("pluginName");
		when(plugin.getConfig()).thenReturn(configuration);
		when(plugin.getDescription()).thenReturn(getPluginDescriptionFile());
		final ItemFactory itemFactory = mock(ItemFactory.class);
		when(itemFactory.equals(any(), any())).thenReturn(true);
		try (final MockedStatic<Bukkit> utilities = mockStatic(Bukkit.class)) {
			utilities.when(Bukkit::getItemFactory).thenReturn(itemFactory);
			// when
			final List<GeneratorPattern> result = configLoader.loadGeneratorPatterns();
			// then
			assertThat(result).containsExactlyInAnyOrderElementsOf(createGeneratorPatterns());
		}
	}

	@NotNull
	private PluginDescriptionFile getPluginDescriptionFile() throws IOException, InvalidConfigurationException,
			InvalidDescriptionException {
		final URL resource = getClass().getClassLoader().getResource("plugin.yml");
		if (resource == null) {
			throw new InvalidConfigurationException("The file \"test/resources/plugin.yml\" does not exist!");
		}
		return new PluginDescriptionFile(new FileInputStream(resource.getFile()));
	}

	@NotNull
	private FileConfiguration getFileConfiguration() throws IOException, InvalidConfigurationException {
		final FileConfiguration configuration = new YamlConfiguration();
		final URL resource = getClass().getClassLoader().getResource("config.yml");
		if (resource == null) {
			throw new InvalidConfigurationException("The file \"test/resources/config.yml\" does not exist!");
		}
		configuration.load(new File(resource.getFile()));
		return configuration;
	}

	@NotNull
	@Unmodifiable
	private List<GeneratorPattern> createGeneratorPatterns() {
		final RandomCollection<NamedItem> randomCollection1 = new RandomCollection<>();
		randomCollection1.add(new NamedItem("bread", new ItemStack(Material.BREAD)), 1);
		randomCollection1.add(new NamedItem("apple", new ItemStack(Material.APPLE, 2)), 1);
		final RandomCollection<NamedItem> randomCollection2 = new RandomCollection<>();
		randomCollection2.add(new NamedItem("obsidian", new ItemStack(Material.OBSIDIAN)), 1);
		return List.of(
				new GeneratorPattern("sponge", 1,
						new NamedItem("sponge", new ItemStack(Material.SPONGE)),
						new NamedItem("stone", new ItemStack(Material.STONE)),
						randomCollection1
				),
				new GeneratorPattern("dirt", 10,
						new NamedItem("dirt", new ItemStack(Material.DIRT)),
						new NamedItem("obsidian", new ItemStack(Material.OBSIDIAN)),
						randomCollection2));
	}
}
