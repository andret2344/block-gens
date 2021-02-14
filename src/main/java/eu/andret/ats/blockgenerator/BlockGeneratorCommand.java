package eu.andret.ats.blockgenerator;

import eu.andret.arguments.AnnotatedCommandExecutor;
import eu.andret.arguments.api.annotation.Argument;
import eu.andret.arguments.api.annotation.BaseCommand;
import eu.andret.arguments.api.annotation.Completer;
import eu.andret.arguments.api.annotation.Fallback;
import eu.andret.arguments.api.annotation.Param;
import eu.andret.arguments.api.entity.ExecutorType;
import eu.andret.ats.blockgenerator.entity.NamedItem;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@BaseCommand("blockgenerator")
public class BlockGeneratorCommand extends AnnotatedCommandExecutor<BlockGeneratorPlugin> {
	public BlockGeneratorCommand(final CommandSender sender, final BlockGeneratorPlugin plugin) {
		super(sender, plugin);
	}

	@Argument(description = "Puts generator item into inventory", executorType = ExecutorType.PLAYER,
			permission = "ats.blockgenerator.get")
	public String generator(@Param("generatorName") @Completer("generatorName") final NamedItem generator) {
		((Player) sender).getInventory().addItem(generator.getItemStack());
		return "Generator block: \"" + generator.getName() + "\"";
	}

	@Fallback
	public String generator(final String ignored, final String generator) {
		return "No generator block found: \"" + generator + "\"";
	}

	@Argument(description = "Puts generator item into inventory", executorType = ExecutorType.PLAYER,
			permission = "ats.blockgenerator.get")
	public String generated(@Param("generatedName") @Completer("generatedName") final NamedItem generated) {
		((Player) sender).getInventory().addItem(generated.getItemStack());
		return "Generated block: \"" + generated.getName() + "\"";
	}

	@Fallback
	public String generated(final String ignored, final String block) {
		return "No generated block found: \"" + block + "\"";
	}

	@Argument(description = "Puts generator item into inventory", executorType = ExecutorType.PLAYER,
			permission = "ats.blockgenerator.get")
	public String item(@Param("itemName") @Completer("itemName") final NamedItem item) {
		((Player) sender).getInventory().addItem(item.getItemStack());
		return "Drop item \"" + item.getName() + "\"";
	}

	@Fallback
	public String item(final String ignored, final String item) {
		return "No drop item found: \"" + item + "\"";
	}
}
