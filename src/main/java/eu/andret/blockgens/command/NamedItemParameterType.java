package eu.andret.blockgens.command;

import eu.andret.blockgens.config.NamedItem;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NamedItemParameterType<T> implements ParameterType<BukkitCommandActor, T> {
	@NotNull
	private final Supplier<Collection<NamedItem>> items;
	@NotNull
	private final Function<NamedItem, T> wrapper;
	@NotNull
	private final String notFoundMessage;

	public NamedItemParameterType(@NotNull final Supplier<Collection<NamedItem>> items,
			@NotNull final Function<NamedItem, T> wrapper, @NotNull final String notFoundMessage) {
		this.items = items;
		this.wrapper = wrapper;
		this.notFoundMessage = notFoundMessage;
	}

	@Override
	public T parse(@NotNull final MutableStringStream input, @NotNull final ExecutionContext<BukkitCommandActor> context) {
		final String name = input.readString();
		return items.get()
				.stream()
				.filter(item -> item.name().equals(name))
				.findAny()
				.map(wrapper)
				.orElseThrow(() -> new CommandErrorException(notFoundMessage.formatted(name)));
	}

	@NotNull
	@Override
	public SuggestionProvider<BukkitCommandActor> defaultSuggestions() {
		return context -> items.get()
				.stream()
				.map(NamedItem::name)
				.distinct()
				.toList();
	}
}
