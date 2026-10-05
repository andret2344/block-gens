package eu.andret.blockgens;

import eu.andret.blockgens.helper.PluginTest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockGensCommandTest extends PluginTest {
	private PlayerMock player;

	@BeforeMethod
	public void setUpPlayer() {
		player = server.addPlayer();
		player.setOp(true);
	}

	@Test
	void generator() {
		// when
		player.performCommand("bg generator sponge");

		// then
		assertThat(player.nextMessage()).isEqualTo("Generator block: \"sponge\"");
		assertThat(inventory()).singleElement()
				.matches(item -> item.isSimilar(pattern("sponge").generatorItem().itemStack()));
	}

	@Test
	void generated() {
		// when
		player.performCommand("blockgens generated obsidian");

		// then
		assertThat(player.nextMessage()).isEqualTo("Generated block: \"obsidian\"");
		assertThat(inventory()).singleElement().extracting(ItemStack::getType).isEqualTo(Material.OBSIDIAN);
	}

	@Test
	void item() {
		// when
		player.performCommand("bg item apple");

		// then
		assertThat(player.nextMessage()).isEqualTo("Drop item: \"apple\"");
		assertThat(inventory()).singleElement().extracting(ItemStack::getType).isEqualTo(Material.APPLE);
	}

	@Test
	void unknownGenerator() {
		// when
		player.performCommand("bg generator nothing");

		// then
		assertThat(nextMessages()).containsExactly("No generator block found: \"nothing\"");
		assertThat(inventory()).isEmpty();
	}

	@Test
	void unknownSubcommand() {
		// when
		player.performCommand("bg nothing");

		// then
		assertThat(nextMessages()).first().asString().startsWith("Failed to find a suitable command");
	}

	@Test
	void suggestions() {
		// when
		final List<String> generators = server.getCommandTabComplete(player, "bg generator ");
		final List<String> items = server.getCommandTabComplete(player, "bg item ");

		// then
		assertThat(generators).containsExactlyInAnyOrder("sponge", "dirt");
		assertThat(items).containsExactlyInAnyOrder("bread", "apple", "obsidian");
	}

	@Test
	void help() {
		// when
		player.performCommand("bg");

		// then
		assertThat(nextMessages()).containsExactlyInAnyOrder(
				"/bg generator <generator> - Puts a generator block into the inventory",
				"/bg generated <generated> - Puts a generated block into the inventory",
				"/bg item <item> - Puts a drop item into the inventory",
				"/bg give <player> <generator> [amount] - Puts a generator block into the inventory of a player",
				"/bg reload - Loads the config again");
	}

	@Test
	void giveFromConsole() {
		// given
		final ConsoleCommandSenderMock console = server.getConsoleSender();

		// when
		server.dispatchCommand(console, "bg give " + player.getName() + " sponge");

		// then
		assertThat(console.nextMessage()).isEqualTo("Gave generator block \"sponge\" to " + player.getName());
		assertThat(inventory()).singleElement()
				.matches(item -> item.isSimilar(pattern("sponge").generatorItem().itemStack()));
	}

	@Test
	void giveWithFullInventory() {
		// given
		// All slots, as MockBukkit's addItem also fills the armour slots, unlike the server
		final ItemStack[] full = new ItemStack[player.getInventory().getContents().length];
		Arrays.fill(full, new ItemStack(Material.DIRT, 64));
		player.getInventory().setContents(full);

		// when
		server.dispatchCommand(server.getConsoleSender(), "bg give " + player.getName() + " sponge");

		// then
		assertThat(player.getWorld().getEntitiesByClass(Item.class))
				.singleElement()
				.matches(item -> item.getItemStack().isSimilar(pattern("sponge").generatorItem().itemStack()));
	}

	@Test
	void giveAmount() {
		// given
		final ConsoleCommandSenderMock console = server.getConsoleSender();

		// when
		server.dispatchCommand(console, "bg give " + player.getName() + " sponge 100");

		// then
		assertThat(console.nextMessage()).isEqualTo("Gave 100 generator blocks \"sponge\" to " + player.getName());
		assertThat(inventory()).extracting(ItemStack::getAmount).containsExactly(64, 36);
		assertThat(inventory()).allMatch(item -> item.isSimilar(pattern("sponge").generatorItem().itemStack()));
	}

	@Test
	void giveAmountWithFullInventory() {
		// given
		final ItemStack[] full = new ItemStack[player.getInventory().getContents().length];
		Arrays.fill(full, new ItemStack(Material.DIRT, 64));
		player.getInventory().setContents(full);

		// when
		server.dispatchCommand(server.getConsoleSender(), "bg give " + player.getName() + " sponge 100");

		// then
		assertThat(player.getWorld().getEntitiesByClass(Item.class))
				.extracting(item -> item.getItemStack().getAmount())
				.containsExactlyInAnyOrder(64, 36);
	}

	@Test
	void giveAmountOutOfRange() {
		// when
		server.dispatchCommand(server.getConsoleSender(), "bg give " + player.getName() + " sponge 0");
		server.dispatchCommand(server.getConsoleSender(), "bg give " + player.getName() + " sponge 2305");

		// then
		assertThat(inventory()).isEmpty();
	}

	@Test
	void giveToUnknownPlayer() {
		// given
		final ConsoleCommandSenderMock console = server.getConsoleSender();

		// when
		server.dispatchCommand(console, "bg give Nobody sponge");

		// then
		assertThat(console.nextMessage()).doesNotStartWith("Gave");
		assertThat(inventory()).isEmpty();
	}

	@Test
	void giveWithoutPermission() {
		// given
		final PlayerMock other = server.addPlayer();

		// when
		other.performCommand("bg give " + player.getName() + " sponge");

		// then
		assertThat(inventory()).isEmpty();
	}

	@Test
	void reload() throws IOException {
		// given
		writeConfig("""
				generators:
				  only:
				    blocks:
				      generator: sponge
				      generated: stone
				blocks:
				  sponge:
				    material: SPONGE
				  stone:
				    material: STONE
				""");

		// when
		player.performCommand("bg reload");

		// then
		assertThat(nextMessages()).containsExactly("Reloaded 1 generators");
	}

	@Test
	void reloadInvalidConfig() throws IOException {
		// given
		writeConfig("""
				blocks:
				  sponge:
				    material: SPONG
				""");

		// when
		player.performCommand("bg reload");

		// then
		assertThat(nextMessages()).containsExactly("The config is invalid and was not reloaded: "
				+ "Unknown material 'SPONG' at 'blocks.sponge.material'.");
		assertThat(plugin.getPatternList()).hasSize(2);
	}

	@Test
	void withoutPermission() {
		// given
		player.setOp(false);

		// when
		player.performCommand("bg generator sponge");

		// then
		assertThat(inventory()).isEmpty();
	}

	private void writeConfig(@NotNull final String yaml) throws IOException {
		Files.writeString(new File(plugin.getDataFolder(), "config.yml").toPath(), yaml);
	}

	@NotNull
	private List<ItemStack> inventory() {
		return Arrays.stream(player.getInventory().getContents())
				.filter(Objects::nonNull)
				.toList();
	}

	// The messages as plain text, without colours
	@NotNull
	private List<String> nextMessages() {
		final List<String> messages = new ArrayList<>();
		Component message;
		while ((message = player.nextComponentMessage()) != null) {
			messages.add(PlainTextComponentSerializer.plainText().serialize(message));
		}
		return messages;
	}
}
