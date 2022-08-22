/*
 * Copyright Andret Tools System (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.blockgenerator;

import eu.andret.arguments.AnnotatedCommandExecutor;
import eu.andret.arguments.api.annotation.Argument;
import eu.andret.arguments.api.annotation.ArgumentFallback;
import eu.andret.arguments.api.annotation.BaseCommand;
import eu.andret.arguments.api.annotation.Completer;
import eu.andret.arguments.api.annotation.Mapper;
import eu.andret.arguments.api.entity.ExecutorType;
import eu.andret.ats.blockgenerator.entity.NamedItem;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@BaseCommand("blockgenerator")
public class BlockGeneratorCommand extends AnnotatedCommandExecutor<BlockGeneratorPlugin> {
	public BlockGeneratorCommand(@NotNull final CommandSender sender, @NotNull final BlockGeneratorPlugin plugin) {
		super(sender, plugin);
	}

	@Argument(description = "Puts generator item into inventory", executorType = ExecutorType.PLAYER,
			permission = "ats.blockgenerator.get")
	public String generator(@Mapper("generatorName") @Completer("generatorName") final NamedItem generator) {
		((Player) sender).getInventory().addItem(generator.getItemStack());
		return "Generator block: \"" + generator.getName() + "\"";
	}

	@ArgumentFallback("generatorName")
	public String generator(final String generator) {
		return "No generator block found: \"" + generator + "\"";
	}

	@Argument(description = "Puts generator item into inventory", executorType = ExecutorType.PLAYER,
			permission = "ats.blockgenerator.get")
	public String generated(@Mapper("generatedName") @Completer("generatedName") final NamedItem generated) {
		((Player) sender).getInventory().addItem(generated.getItemStack());
		return "Generated block: \"" + generated.getName() + "\"";
	}

	@ArgumentFallback("generatedName")
	public String generated(final String block) {
		return "No generated block found: \"" + block + "\"";
	}

	@Argument(description = "Puts generator item into inventory", executorType = ExecutorType.PLAYER,
			permission = "ats.blockgenerator.get")
	public String item(@Mapper("itemName") @Completer("itemName") final NamedItem item) {
		((Player) sender).getInventory().addItem(item.getItemStack());
		return "Drop item: \"" + item.getName() + "\"";
	}

	@ArgumentFallback("itemName")
	public String item(final String item) {
		return "No drop item found: \"" + item + "\"";
	}
}
