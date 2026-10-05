package eu.andret.blockgens;

import eu.andret.blockgens.command.DropItem;
import eu.andret.blockgens.command.GeneratedItem;
import eu.andret.blockgens.command.GeneratorItem;
import eu.andret.blockgens.config.NamedItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Range;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.command.ExecutableCommand;
import revxrsal.commands.help.Help;

import java.io.IOException;

@Command({"blockgens", "bg"})
public class BlockGensCommand {
	private static final String PERMISSION_GET = "blockgens.get";
	private static final String PERMISSION_GIVE = "blockgens.give";
	private static final String PERMISSION_RELOAD = "blockgens.reload";
	// A full inventory of stacks of 64, so a typo cannot drop thousands of items on the ground
	private static final int MAX_AMOUNT = 36 * 64;

	@NotNull
	private final BlockGensPlugin plugin;

	public BlockGensCommand(@NotNull final BlockGensPlugin plugin) {
		this.plugin = plugin;
	}

	@CommandPlaceholder
	public void help(@NotNull final CommandSender sender,
					 @NotNull final Help.ChildrenCommands<BukkitCommandActor> commands) {
		for (final ExecutableCommand<BukkitCommandActor> command : commands) {
			if (command.description() == null) {
				sender.sendMessage("/" + command.usage());
			} else {
				sender.sendMessage("/" + command.usage() + " - " + command.description());
			}
		}
	}

	@Subcommand("generator")
	@Description("Puts a generator block into the inventory")
	@CommandPermission(PERMISSION_GET)
	public void generator(@NotNull final Player sender, @NotNull final GeneratorItem generator) {
		give(sender, generator.item());
		sender.sendMessage("Generator block: \"" + generator.item().name() + "\"");
	}

	@Subcommand("generated")
	@Description("Puts a generated block into the inventory")
	@CommandPermission(PERMISSION_GET)
	public void generated(@NotNull final Player sender, @NotNull final GeneratedItem generated) {
		give(sender, generated.item());
		sender.sendMessage("Generated block: \"" + generated.item().name() + "\"");
	}

	@Subcommand("item")
	@Description("Puts a drop item into the inventory")
	@CommandPermission(PERMISSION_GET)
	public void item(@NotNull final Player sender, @NotNull final DropItem item) {
		give(sender, item.item());
		sender.sendMessage("Drop item: \"" + item.item().name() + "\"");
	}

	// Works from the console, e.g. for shop plugins selling generators
	@Subcommand("give")
	@Description("Puts a generator block into the inventory of a player")
	@CommandPermission(PERMISSION_GIVE)
	public void give(@NotNull final CommandSender sender, @NotNull final Player player,
			@NotNull final GeneratorItem generator, @Default("1") @Range(min = 1, max = MAX_AMOUNT) final int amount) {
		give(player, generator.item(), amount);
		final String blocks = amount == 1 ? "generator block" : amount + " generator blocks";
		sender.sendMessage("Gave " + blocks + " \"" + generator.item().name() + "\" to " + player.getName());
	}

	@Subcommand("reload")
	@Description("Loads the config again")
	@CommandPermission(PERMISSION_RELOAD)
	public void reload(@NotNull final CommandSender sender) {
		try {
			sender.sendMessage("Reloaded " + plugin.reload() + " generators");
		} catch (final IOException | InvalidConfigurationException | IllegalArgumentException e) {
			sender.sendMessage(Component.text("The config is invalid and was not reloaded: " + e.getMessage(),
					NamedTextColor.RED));
		}
	}

	private void give(@NotNull final Player player, @NotNull final NamedItem item) {
		give(player, item, item.itemStack().getAmount());
	}

	// What does not fit into the inventory lands at the player's feet instead of disappearing
	private void give(@NotNull final Player player, @NotNull final NamedItem item, final int amount) {
		// A copy, as adding to an inventory may change the amount of the given item
		player.getInventory().addItem(item.itemStack().asQuantity(amount))
				.values()
				.forEach(left -> drop(player, left));
	}

	// In stacks no bigger than the game allows
	private void drop(@NotNull final Player player, @NotNull final ItemStack itemStack) {
		for (int left = itemStack.getAmount(); left > 0; left -= itemStack.getMaxStackSize()) {
			player.getWorld().dropItemNaturally(player.getLocation(),
					itemStack.asQuantity(Math.min(left, itemStack.getMaxStackSize())));
		}
	}
}
