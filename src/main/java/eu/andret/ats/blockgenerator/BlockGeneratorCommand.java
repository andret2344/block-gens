package eu.andret.ats.blockgenerator;

import eu.andret.arguments.AnnotatedCommandExecutor;
import eu.andret.arguments.api.annotation.Argument;
import eu.andret.arguments.api.annotation.BaseCommand;
import eu.andret.arguments.api.annotation.Fallback;
import eu.andret.arguments.api.annotation.Param;
import eu.andret.arguments.api.entity.ExecutorType;
import eu.andret.ats.blockgenerator.entity.GeneratorPattern;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@BaseCommand("blockgenerator")
public class BlockGeneratorCommand extends AnnotatedCommandExecutor<BlockGenerator> {
	public BlockGeneratorCommand(final CommandSender sender, final BlockGenerator plugin) {
		super(sender, plugin);
	}

	@Fallback
	public String get(final String generatorPattern) {
		return "No generator found named \"" + generatorPattern + "\"";
	}

	@Argument(description = "Puts generator item into inventory", executorType = ExecutorType.PLAYER,
			permission = "ats.blockgenerator.get")
	public String get(@Param("patternName") final GeneratorPattern generatorPattern) {
		((Player) sender).getInventory().addItem(generatorPattern.getGeneratorItem());
		return "Gave \"" + generatorPattern.getName() + "\"";
	}
}
